"""
PROJECT SENTINEL: Digital Signal Processing (DSP) Backend
Description: Reads raw CSI data from the ESP32 via USB Serial, applies digital 
Butterworth bandpass filters, calculates subcarrier variance, and serves 
the system state to the React frontend via WebSockets.

Dependencies: pip install fastapi uvicorn pyserial numpy scipy
"""

import asyncio
import serial
import numpy as np
from scipy.signal import butter, lfilter
from fastapi import FastAPI, WebSocket
import uvicorn
import json

app = FastAPI()

# --- HARDWARE CONFIGURATION ---
SERIAL_PORT = '/dev/cu.usbserial-0001' # Change to 'COM3' on Windows
BAUD_RATE = 921600
VARIANCE_THRESHOLD_FALL = 30.0 # dB Threshold for transient fall event

# --- DSP FILTER DESIGN ---
# Nyquist frequency is half the sampling rate. Assuming ~50Hz sampling from ESP32.
FS = 50.0 
NYQ = 0.5 * FS

def butter_bandpass(lowcut, highcut, fs, order=4):
    low = lowcut / NYQ
    high = highcut / NYQ
    b, a = butter(order, [low, high], btype='band')
    return b, a

def apply_filter(data, b, a):
    # Applies the linear filter along 1-dimension (time series)
    return lfilter(b, a, data)

# Generate filter coefficients for human movement (0.1 Hz to 5.0 Hz)
b_band, a_band = butter_bandpass(0.1, 5.0, FS, order=4)

# --- GLOBAL DATA BUFFERS ---
# Buffer to store the rolling window of the last 100 CSI amplitudes
csi_buffer = []

def process_serial_line(line):
    """ Parses the CSV string from the ESP32 into a numpy array. """
    try:
        parts = line.strip().split(',')
        if parts[0] == "CSI_DATA":
            # Extract raw imaginary and real parts (skipping Header, MAC, RSSI)
            raw_csi = np.array([int(x) for x in parts[3:]])
            
            # Calculate Amplitude (Magnitude) from Complex Numbers: sqrt(I^2 + Q^2)
            # ESP32 packs data as: Imaginary, Real, Imaginary, Real...
            imaginary = raw_csi[0::2]
            real = raw_csi[1::2]
            amplitudes = np.sqrt(imaginary**2 + real**2)
            
            # Use the average amplitude across all 52 subcarriers for this packet
            avg_amplitude = np.mean(amplitudes)
            return avg_amplitude
    except Exception as e:
        pass
    return None

async def read_sensor_data(websocket: WebSocket):
    """ Background task to read USB Serial, apply DSP, and push via WebSocket """
    try:
        ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
        print(f"Connected to ESP32 on {SERIAL_PORT}")
        
        while True:
            if ser.in_waiting > 0:
                line = ser.readline().decode('utf-8')
                amp = process_serial_line(line)
                
                if amp is not None:
                    csi_buffer.append(amp)
                    if len(csi_buffer) > 100:
                        csi_buffer.pop(0) # Maintain rolling window of 100 packets
                    
                    # Wait until buffer is full before filtering
                    if len(csi_buffer) == 100:
                        # 1. Apply Bandpass Filter to remove DC offset and high-freq noise
                        filtered_data = apply_filter(csi_buffer, b_band, a_band)
                        
                        # 2. Calculate Variance of the filtered signal (Energy of movement)
                        current_variance = np.var(filtered_data[-20:]) # Look at last 20 samples
                        
                        # 3. State Machine Logic
                        state = "empty"
                        if current_variance > VARIANCE_THRESHOLD_FALL:
                            state = "fall"
                        elif current_variance > 2.0: # Micro-Doppler threshold
                            state = "breathing"
                            
                        # 4. Transmit payload to React Dashboard
                        payload = {
                            "state": state,
                            "variance_db": round(current_variance, 2),
                            "raw_amplitude": round(amp, 2)
                        }
                        await websocket.send_text(json.dumps(payload))
                        
            await asyncio.sleep(0.01) # Yield to event loop
            
    except serial.SerialException:
        print("Waiting for ESP32 Connection...")
        await asyncio.sleep(2)

@app.websocket("/ws/radar")
async def radar_endpoint(websocket: WebSocket):
    await websocket.accept()
    # Start the hardware reading task when frontend connects
    await read_sensor_data(websocket)

if __name__ == "__main__":
    uvicorn.run("backend:app", host="0.0.0.0", port=8000, reload=True)