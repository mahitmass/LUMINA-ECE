package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarDarkBackground
import com.example.ui.theme.RadarEmerald

@Composable
fun FirmwareCodeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Architecture Audit, 1: TX Firmware, 2: RX Firmware, 3: Python DSP

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RadarDarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B132B))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Engineering,
                        contentDescription = "Architecture",
                        tint = RadarCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "FIRMWARE & ARCHITECTURE SPECIFICATIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "Source code repository and pre-production engineering audit for Project Sentinel ESP-IDF C++ nodes and Python DSP backend.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF020617))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CodeTabButton("Audit Notes", selectedTab == 0, { selectedTab = 0 }, Modifier.weight(1f))
                    CodeTabButton("TX Code", selectedTab == 1, { selectedTab = 1 }, Modifier.weight(1f))
                    CodeTabButton("RX Code", selectedTab == 2, { selectedTab = 2 }, Modifier.weight(1f))
                    CodeTabButton("Python DSP", selectedTab == 3, { selectedTab = 3 }, Modifier.weight(1f))
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                item {
                    AuditSectionCard(
                        title = "1. CSI Callback Guru Meditation Fix",
                        subtitle = "esp_wifi_set_promiscuous_rx_cb vs esp_wifi_set_csi_rx_cb",
                        content = "Calling esp_wifi_set_promiscuous_rx_cb to extract CSI caused a fatal ESP32 Guru Meditation Error because promiscuous mode passes wifi_promiscuous_pkt_t* instead of wifi_csi_info_t*.\n\nFix: Explicitly configure the CSI subsystem using wifi_csi_config_t (enabling lltf_en & htltf_en) and register via esp_wifi_set_csi_rx_cb."
                    )
                }
                item {
                    AuditSectionCard(
                        title = "2. 104-Printf ISR Bottleneck Elimination",
                        subtitle = "FreeRTOS Wi-Fi Task Priority 23 Starvation",
                        content = "Executing 104 separate Serial.printf() statements inside the driver callback caused buffer overflows and watchdog resets because ESP32 hardware UART TX buffer is only 128 bytes.\n\nFix: Pre-filter target TX MAC via memcmp in O(1) time and format payload into a single stack buffer (snprintf) before executing one atomic Serial.write() at 921600 baud."
                    )
                }
                item {
                    AuditSectionCard(
                        title = "3. Stateful SOS Filter vs Impulse Ringing",
                        subtitle = "Eliminating Startup Edge Transients",
                        content = "Stateless scipy.signal.lfilter on rolling buffers triggered massive impulse response transients (filter ringing) across 30-50 samples due to the step change from 0 to raw DC amplitude (~24 dB).\n\nFix: Implemented continuous stateful Second-Order Sections (scipy.signal.sosfilt) across 40 subcarriers simultaneously, primed with sosfilt_zi on the very first packet."
                    )
                }
                item {
                    AuditSectionCard(
                        title = "4. Frequency-Selective Fading & 90th Percentile",
                        subtitle = "Avoiding Multipath Phase Cancellation",
                        content = "Averaging subcarrier amplitudes before filtering violates RF multipath physics: human movement causes constructive interference on some subcarriers while causing deep fading nulls on others. Averaging cancels the signal.\n\nFix: Filter each of the 40 clean OFDM subcarriers independently, compute temporal variance per subcarrier, and take the 90th percentile."
                    )
                }
            }
            1 -> {
                item {
                    CodeViewerCard(
                        fileName = "esp32_tx.ino (Deterministic 100Hz RF Injector)",
                        code = TX_CODE,
                        onCopy = { copyToClipboard(context, TX_CODE, "TX Code") }
                    )
                }
            }
            2 -> {
                item {
                    CodeViewerCard(
                        fileName = "esp32_rx.ino (CSI Radar Receiver & Actuator)",
                        code = RX_CODE,
                        onCopy = { copyToClipboard(context, RX_CODE, "RX Code") }
                    )
                }
            }
            3 -> {
                item {
                    CodeViewerCard(
                        fileName = "backend.py (Production DSP & WebSocket Backend)",
                        code = PYTHON_DSP_CODE,
                        onCopy = { copyToClipboard(context, PYTHON_DSP_CODE, "Python DSP Code") }
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Color(0xFF1E293B) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) RadarCyan else Color(0xFF64748B)
        )
    }
}

@Composable
private fun AuditSectionCard(
    title: String,
    subtitle: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B132B))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = RadarCyan
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = RadarAmber
        )
        Text(
            text = content,
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun CodeViewerCard(
    fileName: String,
    code: String,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070D1E))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Code",
                    tint = RadarCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = fileName,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = RadarCyan
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("copy_code_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(text = "Copy", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        SelectionContainer {
            Text(
                text = code,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8),
                lineHeight = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF020617))
                    .padding(12.dp)
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

// Embedded Source Code constants from uploaded files
private const val TX_CODE = """/*
 * PROJECT SENTINEL: Deterministic 100Hz RF Injector (TX Node)
 * Hardware: ESP32-WROOM-32D
 * Description: Spoofs custom locally-administered MAC and transmits 
 * connectionless ESP-NOW packets at 100Hz on Channel 6 to excite the RF channel.
 */

#include <WiFi.h>
#include <esp_wifi.h>
#include <esp_now.h>

#define WIFI_CHANNEL 6
#define TX_INTERVAL_MS 10 // 100Hz deterministic sampling rate

const uint8_t SENTINEL_TX_MAC[6] = {0x1A, 0x00, 0x00, 0x00, 0x00, 0x01};
const uint8_t BROADCAST_MAC[6]   = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};

typedef struct __attribute__((packed)) {
  uint32_t seq_num;
  uint32_t timestamp_ms;
  uint8_t  magic_byte;
} rf_probe_packet_t;

rf_probe_packet_t probe_pkt;
esp_now_peer_info_t peer_info;
uint32_t seq_counter = 0;
unsigned long last_tx_time = 0;

void setup() {
  Serial.begin(921600);
  WiFi.mode(WIFI_MODE_STA);
  WiFi.disconnect();

  ESP_ERROR_CHECK(esp_wifi_set_mac(WIFI_IF_STA, SENTINEL_TX_MAC));
  ESP_ERROR_CHECK(esp_wifi_set_promiscuous(true));
  ESP_ERROR_CHECK(esp_wifi_set_channel(WIFI_CHANNEL, WIFI_SECOND_CHAN_NONE));
  ESP_ERROR_CHECK(esp_wifi_set_promiscuous(false));
  ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));
  ESP_ERROR_CHECK(esp_wifi_set_max_tx_power(78)); // 19.5 dBm

  if (esp_now_init() != ESP_OK) {
    ESP.restart();
  }

  memset(&peer_info, 0, sizeof(peer_info));
  memcpy(peer_info.peer_addr, BROADCAST_MAC, 6);
  peer_info.channel = WIFI_CHANNEL;
  peer_info.encrypt = false;
  ESP_ERROR_CHECK(esp_now_add_peer(&peer_info));

  probe_pkt.magic_byte = 0xA5;
  Serial.println("SENTINEL TX ONLINE: Injecting 100Hz RF Probes on Channel 6...");
}

void loop() {
  unsigned long now = millis();
  if (now - last_tx_time >= TX_INTERVAL_MS) {
    last_tx_time = now;
    probe_pkt.seq_num = seq_counter++;
    probe_pkt.timestamp_ms = now;
    esp_now_send(BROADCAST_MAC, (uint8_t *)&probe_pkt, sizeof(probe_pkt));
  }
}"""

private const val RX_CODE = """/*
 * PROJECT SENTINEL: High-Speed CSI Radar Receiver & Actuator (RX Node)
 * Hardware: ESP32-WROOM-32D
 * Description: Captures L-LTF / HT-LTF CSI matrices strictly from Sentinel TX MAC,
 * uses single-buffer zero-heap formatting for 921600 baud,
 * and drives 2N2222 transistor alarm circuit on GPIO 25.
 */

#include <WiFi.h>
#include <esp_wifi.h>
#include <esp_now.h>

#define WIFI_CHANNEL 6
#define BAUD_RATE 921600
#define ALARM_GPIO_PIN 25 // Drives Base of 2N2222 Transistor via 1k Ohm resistor

const uint8_t TARGET_TX_MAC[6] = {0x1A, 0x00, 0x00, 0x00, 0x00, 0x01};

void IRAM_ATTR csi_rx_callback(void *ctx, wifi_csi_info_t *data) {
  if (!data || !data->buf) return;

  // 1. O(1) Hardware MAC Filter
  if (memcmp(data->mac, TARGET_TX_MAC, 6) != 0) return;

  // 2. Single-Buffer Stack Formatting (Eliminates 104 blocking printf calls)
  char out_buf[640];
  int offset = snprintf(out_buf, sizeof(out_buf),
    "CSI_DATA,%02X:%02X:%02X:%02X:%02X:%02X,%d,",
    data->mac[0], data->mac[1], data->mac[2],
    data->mac[3], data->mac[4], data->mac[5],
    data->rx_ctrl.rssi
  );

  int payload_len = (data->len > 128) ? 128 : data->len;
  int8_t *csi_buf = data->buf;
  for (int i = 0; i < payload_len; i++) {
    offset += snprintf(out_buf + offset, sizeof(out_buf) - offset,
      (i < payload_len - 1) ? "%d," : "%d\n", csi_buf[i]);
    if (offset >= (int)sizeof(out_buf) - 12) break;
  }

  // 3. Atomic UART TX write
  Serial.write((const uint8_t *)out_buf, offset);
}

void setup() {
  Serial.setTxBufferSize(2048);
  Serial.begin(BAUD_RATE);

  pinMode(ALARM_GPIO_PIN, OUTPUT);
  digitalWrite(ALARM_GPIO_PIN, LOW);

  WiFi.mode(WIFI_MODE_STA);
  WiFi.disconnect();
  ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));
  ESP_ERROR_CHECK(esp_wifi_set_promiscuous(true));
  ESP_ERROR_CHECK(esp_wifi_set_channel(WIFI_CHANNEL, WIFI_SECOND_CHAN_NONE));
  esp_now_init();

  wifi_csi_config_t csi_config;
  csi_config.lltf_en           = true;
  csi_config.htltf_en          = true;
  csi_config.stbc_htltf2_en    = false;
  csi_config.ltf_merge_en      = true;
  csi_config.channel_filter_en = true;
  csi_config.manu_scale        = false;
  csi_config.shift             = false;

  ESP_ERROR_CHECK(esp_wifi_set_csi_config(&csi_config));
  ESP_ERROR_CHECK(esp_wifi_set_csi_rx_cb(&csi_rx_callback, NULL));
  ESP_ERROR_CHECK(esp_wifi_set_csi(true));

  Serial.println("SENTINEL RX READY: CSI Engine & GPIO 25 Actuator Armed.");
}

void loop() {
  while (Serial.available() > 0) {
    char cmd = (char)Serial.read();
    if (cmd == '1') digitalWrite(ALARM_GPIO_PIN, HIGH);
    else if (cmd == '0') digitalWrite(ALARM_GPIO_PIN, LOW);
  }
  delay(5);
}"""

private const val PYTHON_DSP_CODE = """# PROJECT SENTINEL: Production DSP & WebSocket Backend (backend.py)
import asyncio, json, os, time
from collections import deque
import numpy as np, serial, uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from scipy.signal import butter, sosfilt, sosfilt_zi

app = FastAPI(title="Project Sentinel DSP Engine")
FS = 100.0; LOWCUT_HZ = 0.15; HIGHCUT_HZ = 6.0; FILTER_ORDER = 4
VARIANCE_THRESHOLD_FALL = 30.0; VARIANCE_THRESHOLD_BREATHING = 2.0
VALID_SUBCARRIER_INDICES = np.concatenate([np.arange(6, 26), np.arange(38, 58)])

class RealTimeCSIDSPEngine:
    def __init__(self):
        nyq = 0.5 * FS
        self.sos = butter(FILTER_ORDER, [LOWCUT_HZ/nyq, HIGHCUT_HZ/nyq], btype="bandpass", output="sos")
        zi_base = sosfilt_zi(self.sos)
        self.zi = np.repeat(zi_base[:, :, np.newaxis], 40, axis=2)
        self.filtered_window = deque(maxlen=50)
        self.alarm_active = False

    def process_raw_line(self, line: str):
        if not line.startswith("CSI_DATA,"): return None
        parts = line.strip().split(",")
        if len(parts) < 131: return None
        iq_raw = np.fromiter((int(x) for x in parts[3:131]), dtype=np.float64, count=128)
        amplitudes = np.hypot(iq_raw[0::2], iq_raw[1::2])
        clean_amps = amplitudes[VALID_SUBCARRIER_INDICES]

        filtered_step, self.zi = sosfilt(self.sos, clean_amps[np.newaxis, :], axis=0, zi=self.zi)
        self.filtered_window.append(filtered_step[0])
        if len(self.filtered_window) < 15: return None

        window_matrix = np.array(self.filtered_window)
        subcarrier_variances = np.var(window_matrix, axis=0)
        robust_variance = float(np.percentile(subcarrier_variances, 90))

        state = "empty"
        if robust_variance >= VARIANCE_THRESHOLD_FALL: state = "fall"
        elif robust_variance >= VARIANCE_THRESHOLD_BREATHING: state = "breathing"
        return {"state": state, "variance_db": round(robust_variance, 2), "raw_amplitude": round(float(np.mean(clean_amps)), 2)}
"""
