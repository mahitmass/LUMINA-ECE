import asyncio
import ast
import math
import time
import sys
import json
from collections import deque
import numpy as np
from scipy.signal import butter, filtfilt
import serial
import serial.tools.list_ports
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn

# --- HARDWARE CONFIGURATION ---
PORT = "AUTO"
BAUD_RATE = 921600
SUBCARRIERS = [10, 20, 30, 40, 50]
NUM_SUB = len(SUBCARRIERS)

# --- DSP ENGINE CONFIG ---
FS = 70.0               
WINDOW_SECONDS = 2.0
BUFFER_SIZE = int(FS * WINDOW_SECONDS)

# --- DUAL-TRIGGER TUNABLE THRESHOLDS ---
# Trigger 1: Direct Thresholds (Peak -> Low Stillness)
PEAK_THRESHOLD = 8.0        
LOW_STILLNESS_MAX = 2.2     

# Trigger 2: Graph Cliff Detector (Rapid Drop in Short Time)
MIN_PEAK_FOR_CLIFF = 6.5    
CLIFF_DROP_DELTA = 4.5      
CLIFF_BOTTOM_MAX = 2.5      

app = FastAPI(title="Sentinel CSI Dual-Trigger Fall Engine")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Shared State
csi_buffer = deque(maxlen=BUFFER_SIZE)
score_history = deque(maxlen=16)  
fall_state = "IDLE"               
time_of_impact = 0.0
impact_peak_value = 0.0
last_trigger_reason = "None"
connected_clients = set()
event_logs = []

class ThresholdUpdate(BaseModel):
    peak_threshold: float
    low_stillness_max: float
    cliff_drop_delta: float

def play_os_beep():
    try:
        if sys.platform == "win32":
            import winsound
            winsound.Beep(1200, 450)
        else:
            sys.stdout.write('\a')
            sys.stdout.flush()
    except Exception:
        pass

def highpass_filter_1d(data, cutoff=2.5, fs=70.0, order=4):
    nyquist = 0.5 * fs
    normal_cutoff = min(max(cutoff / nyquist, 0.01), 0.99)
    b, a = butter(order, normal_cutoff, btype="high", analog=False)
    return filtfilt(b, a, data, padlen=min(len(data) - 1, 15))

def detect_serial_port():
    if PORT != "AUTO":
        return PORT
    ports = list(serial.tools.list_ports.comports())
    for p in ports:
        desc = (p.description or "").lower()
        if any(k in desc for k in ["cp210", "ch340", "usb", "uart", "serial", "com"]):
            return p.device
    return "COM4" if sys.platform == "win32" else "/dev/tty.usbserial-0001"

async def broadcast_telemetry(payload: dict):
    if not connected_clients:
        return
    msg = json.dumps(payload)
    dead_clients = set()
    for ws in list(connected_clients):
        try:
            await ws.send_text(msg)
        except Exception:
            dead_clients.add(ws)
    for ws in dead_clients:
        connected_clients.discard(ws)

def trigger_confirmed_fall(reason: str, current_score: float, peak_score: float, drop_delta: float):
    global fall_state, last_trigger_reason
    fall_state = "CONFIRMED_FALL"
    last_trigger_reason = reason
    play_os_beep()
    event_logs.insert(0, {
        "time": time.strftime("%H:%M:%S"),
        "type": "CONFIRMED_FALL",
        "reason": reason,
        "peak_score": round(peak_score, 2),
        "low_score": round(current_score, 2),
        "drop_delta": round(drop_delta, 2)
    })
    print("\n" + "=" * 60)
    print(f" [!!!] SIREN TRIGGERED: {reason}")
    print(f"       Peak: {peak_score:.2f} -> Low: {current_score:.2f} (Drop: -{drop_delta:.2f})")
    print("=" * 60 + "\n")

async def csi_reader_loop():
    global fall_state, time_of_impact, impact_peak_value, last_trigger_reason

    target_port = detect_serial_port()
    ser = None
    hardware_connected = False

    try:
        ser = serial.Serial(target_port, BAUD_RATE, timeout=0.1)
        hardware_connected = True
        print(f"[+] Connected to ESP32 Receiver on {target_port} @ {BAUD_RATE} baud")
    except Exception as e:
        print(f"[!] Hardware not detected on {target_port} ({e}). Running in Demo/Simulation Ready mode.")

    last_eval_time = time.monotonic()

    while True:
        await asyncio.sleep(0.005)

        if hardware_connected and ser and ser.is_open:
            try:
                while ser.in_waiting > 0:
                    line = ser.readline().decode("utf-8", errors="replace").strip()
                    if "CSI_DATA," in line:
                        start = line.rfind("[")
                        end = line.rfind("]") + 1
                        if start != -1 and end > 0:
                            values = ast.literal_eval(line[start:end])
                            if len(values) == 384:
                                mags = [math.hypot(values[sc * 2], values[sc * 2 + 1]) for sc in SUBCARRIERS]
                                csi_buffer.append(mags)
            except Exception:
                pass
        else:
            idle_mags = [13.5 + np.random.normal(0, 0.5) for _ in SUBCARRIERS]
            csi_buffer.append(idle_mags)
            await asyncio.sleep(1.0 / FS)

        now = time.monotonic()

        if len(csi_buffer) >= (BUFFER_SIZE // 2) and (now - last_eval_time) >= 0.25:
            last_eval_time = now

            window_data_2d = np.array(csi_buffer)
            window_data_1d = np.mean(window_data_2d, axis=1)
            current_altitude = float(np.mean(window_data_1d))

            try:
                hf_1d = highpass_filter_1d(window_data_1d, cutoff=2.5, fs=FS)
                filtered_var = float(np.var(hf_1d))
                raw_var = float(np.var(window_data_1d))
                activity_score = round((0.65 * filtered_var) + (0.35 * raw_var), 2)
                subcarrier_bars = [round(float(np.var(window_data_2d[:, i])), 2) for i in range(NUM_SUB)]
            except ValueError:
                activity_score = round(float(np.var(window_data_1d)), 2)
                subcarrier_bars = [activity_score] * NUM_SUB

            score_history.append(activity_score)
            recent_peak = float(np.max(score_history)) if score_history else activity_score
            peak_idx = int(np.argmax(score_history)) if score_history else 0
            recent_drop_delta = round(recent_peak - activity_score, 2)

            if fall_state == "IDLE":
                if activity_score >= PEAK_THRESHOLD:
                    fall_state = "IMPACT_DETECTED"
                    time_of_impact = now
                    impact_peak_value = activity_score
                    print(f"[!] PEAK DETECTED ({activity_score:.2f} >= {PEAK_THRESHOLD}). Watching for stillness drop...")

                elif (
                    len(score_history) >= 4
                    and peak_idx < len(score_history) - 1
                    and recent_peak >= MIN_PEAK_FOR_CLIFF
                    and recent_drop_delta >= CLIFF_DROP_DELTA
                    and activity_score <= CLIFF_BOTTOM_MAX
                ):
                    trigger_confirmed_fall(
                        reason="Trigger 2: Rapid Graph Cliff Drop",
                        current_score=activity_score,
                        peak_score=recent_peak,
                        drop_delta=recent_drop_delta
                    )

            elif fall_state == "IMPACT_DETECTED":
                if activity_score > impact_peak_value:
                    impact_peak_value = activity_score

                elapsed = now - time_of_impact

                if elapsed >= 0.75:
                    drop_amount = impact_peak_value - activity_score

                    if activity_score <= LOW_STILLNESS_MAX:
                        trigger_confirmed_fall(
                            reason="Trigger 1: Peak + Low Stillness Threshold",
                            current_score=activity_score,
                            peak_score=impact_peak_value,
                            drop_delta=drop_amount
                        )
                    elif drop_amount >= CLIFF_DROP_DELTA and activity_score <= CLIFF_BOTTOM_MAX:
                        trigger_confirmed_fall(
                            reason="Trigger 2: Rapid Graph Cliff Drop",
                            current_score=activity_score,
                            peak_score=impact_peak_value,
                            drop_delta=drop_amount
                        )
                    elif elapsed >= 3.5:
                        fall_state = "FALSE_ALARM"
                        event_logs.insert(0, {
                            "time": time.strftime("%H:%M:%S"),
                            "type": "CONTINUOUS_MOVEMENT",
                            "reason": "Stayed active (Walking/Standing) — No stillness drop",
                            "peak_score": round(impact_peak_value, 2),
                            "low_score": round(activity_score, 2),
                            "drop_delta": round(drop_amount, 2)
                        })
                        print(f"[-] Movement continued (Score: {activity_score:.2f} > {LOW_STILLNESS_MAX}). Resetting to IDLE.")

            if fall_state == "FALSE_ALARM" and (now - time_of_impact) >= 6.0:
                fall_state = "IDLE"

            payload = {
                "timestamp": time.strftime("%H:%M:%S"),
                "hardware_connected": hardware_connected,
                "port": target_port if hardware_connected else "SIMULATION",
                "state": fall_state,
                "trigger_reason": last_trigger_reason,
                "hf_energy": activity_score,
                "recent_peak": round(recent_peak, 2),
                "drop_delta": recent_drop_delta,
                "peak_threshold": PEAK_THRESHOLD,
                "low_threshold": LOW_STILLNESS_MAX,
                "cliff_delta_threshold": CLIFF_DROP_DELTA,
                "csi_altitude": round(current_altitude, 2),
                "subcarrier_energies": subcarrier_bars,
                "logs": event_logs[:15]
            }
            await broadcast_telemetry(payload)

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(csi_reader_loop())

@app.post("/api/reset")
async def reset_alarm():
    global fall_state, last_trigger_reason
    fall_state = "IDLE"
    last_trigger_reason = "None"
    score_history.clear()
    return {"status": "reset", "state": fall_state}

@app.post("/api/thresholds")
async def update_thresholds(cfg: ThresholdUpdate):
    global PEAK_THRESHOLD, LOW_STILLNESS_MAX, CLIFF_DROP_DELTA
    PEAK_THRESHOLD = cfg.peak_threshold
    LOW_STILLNESS_MAX = cfg.low_stillness_max
    CLIFF_DROP_DELTA = cfg.cliff_drop_delta
    print(f"[*] Updated Thresholds -> Peak: {PEAK_THRESHOLD}, Stillness: {LOW_STILLNESS_MAX}, Cliff Drop: {CLIFF_DROP_DELTA}")
    return {"status": "updated"}

@app.post("/api/simulate/{sim_type}")
async def simulate_event(sim_type: str):
    global fall_state, time_of_impact
    now = time.monotonic()
    if sim_type == "fall":
        score_history.extend([7.2, 10.4, 3.1, 1.1])
        trigger_confirmed_fall(
            reason="Simulated Fall (Peak 10.4 -> Low 1.1)",
            current_score=1.1,
            peak_score=10.4,
            drop_delta=9.3
        )
    elif sim_type == "drop":
        fall_state = "FALSE_ALARM"
        time_of_impact = now - 3.6
        event_logs.insert(0, {
            "time": time.strftime("%H:%M:%S"),
            "type": "CONTINUOUS_MOVEMENT",
            "reason": "Spike detected without floor stillness",
            "peak_score": 9.1,
            "low_score": 5.4,
            "drop_delta": 3.7
        })
    return {"status": "ok", "simulated": sim_type}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    connected_clients.add(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            if data == "RESET":
                global fall_state
                fall_state = "IDLE"
    except WebSocketDisconnect:
        connected_clients.discard(websocket)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)