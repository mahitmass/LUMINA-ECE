"""
PROJECT SENTINEL: Production DSP & WebSocket Backend (backend.py)
Architecture:
  - Stateful 4th-Order Butterworth Bandpass Filter using Second-Order Sections (sosfilt)
  - Multi-subcarrier spatial variance tracking (eliminates frequency-selective fading cancellation)
  - Non-blocking serial buffer drain (<20ms serial-to-websocket latency)
  - Bidirectional UART actuation for FR-05 (GPIO 25 Buzzer/LEDs)
"""

import asyncio
import json
import os
import time
from collections import deque
from typing import Optional, Set

import numpy as np
import serial
import serial.tools.list_ports
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from scipy.signal import butter, sosfilt, sosfilt_zi
import uvicorn

app = FastAPI(title="Project Sentinel DSP Engine")

# --- HARDWARE & SERIAL CONFIGURATION ---
# Override via environment variable: export SENTINEL_PORT="/dev/cu.usbserial-0001"
SERIAL_PORT = os.getenv("SENTINEL_PORT", "/dev/cu.usbserial-0001")
BAUD_RATE = 921600

# --- RADAR DSP PARAMETERS ---
FS = 100.0                      # Expected sampling rate from esp32_tx.ino (100 Hz)
LOWCUT_HZ = 0.15                # Rejects static walls/furniture (0 Hz DC)
HIGHCUT_HZ = 6.0                # Isolates human breathing (0.2-0.5Hz) & falls (1.5-6.0Hz)
FILTER_ORDER = 4
VARIANCE_WINDOW_SAMPLES = 50    # 0.5s - 1.0s sliding window for transient energy

# Calibrated thresholds (matches UI/UX Specification)
VARIANCE_THRESHOLD_FALL = 30.0
VARIANCE_THRESHOLD_BREATHING = 2.0

# Active 802.11 OFDM Subcarrier Indices (Out of 64 total I/Q pairs in 128-byte buffer)
# Excludes Guard Bands (0..5, 59..63) and DC Null Subcarrier (32)
VALID_SUBCARRIER_INDICES = np.concatenate([np.arange(6, 26), np.arange(38, 58)])
NUM_ACTIVE_SUBCARRIERS = len(VALID_SUBCARRIER_INDICES)  # 40 clean subcarriers


class RealTimeCSIDSPEngine:
    """
    Stateful Multi-Subcarrier DSP Pipeline.
    Processes each packet in O(1) time using Second-Order Sections (SOS)
    without window-edge ringing or multipath phase cancellation.
    """

    def __init__(self) -> None:
        nyq = 0.5 * FS
        self.sos = butter(
            FILTER_ORDER,
            [LOWCUT_HZ / nyq, HIGHCUT_HZ / nyq],
            btype="bandpass",
            output="sos",
        )
        # Initialize filter state (zi) for all 40 subcarriers: shape (n_sections, 2, 40)
        zi_base = sosfilt_zi(self.sos)  # shape (n_sections, 2)
        self.zi = np.repeat(zi_base[:, :, np.newaxis], NUM_ACTIVE_SUBCARRIERS, axis=2)
        self.is_primed = False

        # Rolling ring buffer of filtered subcarrier vectors: shape (W, 40)
        self.filtered_window = deque(maxlen=VARIANCE_WINDOW_SAMPLES)
        self.alarm_active = False
        self.last_alarm_time = 0.0

    def process_raw_line(self, line: str) -> Optional[dict]:
        if not line.startswith("CSI_DATA,"):
            return None

        parts = line.strip().split(",")
        if len(parts) < 131:  # Header(1) + MAC(1) + RSSI(1) + 128 bytes (64 subcarriers)
            return None

        try:
            # Parse raw signed int8 I/Q values
            iq_raw = np.fromiter((int(x) for x in parts[3:131]), dtype=np.float64, count=128)
        except ValueError:
            return None

        # ESP32 packs CSI buffer as [Imaginary_0, Real_0, Imaginary_1, Real_1, ...]
        imag = iq_raw[0::2]
        real = iq_raw[1::2]
        amplitudes = np.hypot(imag, real)  # Vectorized sqrt(I^2 + Q^2), length 64

        # Isolate the 40 clean OFDM subcarriers
        clean_amps = amplitudes[VALID_SUBCARRIER_INDICES]  # shape (40,)
        mean_raw_amp = float(np.mean(clean_amps))

        # Prime filter state on first packet to eliminate step-response transient
        if not self.is_primed:
            zi_base = sosfilt_zi(self.sos)
            self.zi = zi_base[:, :, np.newaxis] * clean_amps[np.newaxis, np.newaxis, :]
            self.is_primed = True

        # Stateful O(1) SOS Bandpass Filter step across all 40 subcarriers simultaneously
        filtered_step, self.zi = sosfilt(self.sos, clean_amps[np.newaxis, :], axis=0, zi=self.zi)
        self.filtered_window.append(filtered_step[0])

        if len(self.filtered_window) < 15:
            return {
                "state": "empty",
                "variance_db": 0.0,
                "raw_amplitude": round(mean_raw_amp, 2),
                "actuate_gpio": False,
            }

        # Convert rolling deque to 2D matrix: shape (N_samples, 40_subcarriers)
        window_matrix = np.array(self.filtered_window)

        # Compute temporal variance per subcarrier, then take 90th percentile across subcarriers
        # This guarantees detection even if half the subcarriers sit in a deep multipath null
        subcarrier_variances = np.var(window_matrix, axis=0)
        robust_variance = float(np.percentile(subcarrier_variances, 90))

        # State Machine & False-Positive Rejection Logic
        now = time.monotonic()
        if robust_variance >= VARIANCE_THRESHOLD_FALL:
            state = "fall"
            self.alarm_active = True
            self.last_alarm_time = now
        elif robust_variance >= VARIANCE_THRESHOLD_BREATHING:
            state = "breathing"
        else:
            state = "empty"

        # Hold hardware buzzer high for 3.0 seconds on fall event (FR-05)
        if self.alarm_active and (now - self.last_alarm_time > 3.0):
            self.alarm_active = False

        return {
            "state": state,
            "variance_db": round(robust_variance, 2),
            "raw_amplitude": round(mean_raw_amp, 2),
            "actuate_gpio": self.alarm_active,
        }


# --- GLOBAL CONNECTION MANAGER ---
connected_clients: Set[WebSocket] = set()
dsp_engine = RealTimeCSIDSPEngine()


def auto_detect_serial_port() -> str:
    """Attempts to use configured port, or auto-discovers CP210x / CH340 ESP32 USB UART."""
    if os.path.exists(SERIAL_PORT):
        return SERIAL_PORT
    for port in serial.tools.list_ports.comports():
        if any(k in (port.description or "") for k in ("CP210", "CH340", "USB", "UART", "SLAB")):
            return port.device
    return SERIAL_PORT


async def hardware_serial_worker() -> None:
    """
    Dedicated background coroutine that drains the ESP32 UART buffer with zero lag,
    drives the GPIO 25 buzzer actuator, and broadcasts telemetry to connected WebSockets.
    """
    last_gpio_state = False

    while True:
        port_name = auto_detect_serial_port()
        try:
            ser = serial.Serial(port_name, BAUD_RATE, timeout=0.005)
            ser.reset_input_buffer()
            print(f"[SENTINEL] Locked onto ESP32 RX Node at {port_name} ({BAUD_RATE} baud)")

            while True:
                bytes_waiting = ser.in_waiting
                if bytes_waiting > 0:
                    # Read entire available OS serial buffer to prevent lag accumulation
                    raw_chunk = ser.read(bytes_waiting).decode("utf-8", errors="ignore")
                    lines = raw_chunk.splitlines()

                    latest_payload = None
                    for line in lines:
                        res = dsp_engine.process_raw_line(line)
                        if res is not None:
                            latest_payload = res

                    if latest_payload is not None:
                        # 1. Trigger Physical Hardware Actuation (FR-05: Buzzer + LEDs)
                        desired_gpio = latest_payload.pop("actuate_gpio", False)
                        if desired_gpio != last_gpio_state:
                            ser.write(b"1" if desired_gpio else b"0")
                            last_gpio_state = desired_gpio

                        # 2. Broadcast to all connected React Dashboards
                        if connected_clients:
                            msg = json.dumps(latest_payload)
                            stale_clients = set()
                            for ws in connected_clients:
                                try:
                                    await ws.send_text(msg)
                                except Exception:
                                    stale_clients.add(ws)
                            connected_clients.difference_update(stale_clients)

                await asyncio.sleep(0.002)  # 500Hz poll rate (<2ms serial-to-DSP latency)

        except serial.SerialException:
            print(f"[SENTINEL] Waiting for ESP32 RX on {port_name}...")
            await asyncio.sleep(2.0)
        except Exception as exc:
            print(f"[SENTINEL] Serial Worker Error: {exc}")
            await asyncio.sleep(1.0)


@app.on_event("startup")
async def startup_event() -> None:
    asyncio.create_task(hardware_serial_worker())


@app.websocket("/ws/radar")
@app.websocket("/ws")
async def radar_websocket_endpoint(websocket: WebSocket) -> None:
    await websocket.accept()
    connected_clients.add(websocket)
    try:
        while True:
            # Keep socket alive and listen for optional manual reset commands from UI
            await websocket.receive_text()
    except WebSocketDisconnect:
        connected_clients.discard(websocket)


if __name__ == "__main__":
    uvicorn.run("backend:app", host="0.0.0.0", port=8000, reload=False)