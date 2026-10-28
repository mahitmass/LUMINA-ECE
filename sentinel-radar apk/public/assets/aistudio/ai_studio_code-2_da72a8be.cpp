/*
 * PROJECT SENTINEL: High-Speed CSI Radar Receiver & Actuator (RX Node)
 * Hardware: ESP32-WROOM-32D
 * Description: Captures L-LTF / HT-LTF CSI matrices strictly from the Sentinel TX MAC,
 * uses single-buffer zero-heap-allocation formatting for 921600 baud output,
 * and drives the 2N2222 transistor alarm circuit on GPIO 25.
 */

#include <WiFi.h>
#include <esp_wifi.h>
#include <esp_now.h>

#define WIFI_CHANNEL 6
#define BAUD_RATE 921600
#define ALARM_GPIO_PIN 25 // Drives Base of 2N2222 Transistor via 1k Ohm resistor

// Expected TX MAC address (must match esp32_tx.ino)
const uint8_t TARGET_TX_MAC[6] = {0x1A, 0x00, 0x00, 0x00, 0x00, 0x01};

// Dedicated CSI Callback executed inside ESP-IDF Wi-Fi Task
void IRAM_ATTR csi_rx_callback(void *ctx, wifi_csi_info_t *data) {
  if (!data || !data->buf) return;

  // 1. O(1) Hardware MAC Filter: Reject all ambient exhibition hall Wi-Fi traffic
  if (memcmp(data->mac, TARGET_TX_MAC, 6) != 0) {
    return;
  }

  // 2. Single-Buffer Stack Formatting (Eliminates 104 blocking Serial.printf calls)
  // Max L-LTF/HT-LTF length we process is 128 bytes (64 subcarriers * 2 I/Q bytes)
  char out_buf[640];
  int offset = snprintf(
    out_buf, sizeof(out_buf),
    "CSI_DATA,%02X:%02X:%02X:%02X:%02X:%02X,%d,",
    data->mac[0], data->mac[1], data->mac[2],
    data->mac[3], data->mac[4], data->mac[5],
    data->rx_ctrl.rssi
  );

  int payload_len = (data->len > 128) ? 128 : data->len;
  int8_t *csi_buf = data->buf;

  for (int i = 0; i < payload_len; i++) {
    offset += snprintf(
      out_buf + offset,
      sizeof(out_buf) - offset,
      (i < payload_len - 1) ? "%d," : "%d\n",
      csi_buf[i]
    );
    if (offset >= (int)sizeof(out_buf) - 12) break; // Guard stack overflow
  }

  // 3. Atomic UART TX write
  Serial.write((const uint8_t *)out_buf, offset);
}

void setup() {
  // Expand Hardware UART TX ring buffer to prevent blocking at 100Hz
  Serial.setTxBufferSize(2048);
  Serial.begin(BAUD_RATE);

  // Configure FR-05 Hardware Actuator Pin
  pinMode(ALARM_GPIO_PIN, OUTPUT);
  digitalWrite(ALARM_GPIO_PIN, LOW);

  // 1. Initialize Wi-Fi in STA mode & disable sleep
  WiFi.mode(WIFI_MODE_STA);
  WiFi.disconnect();
  ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));

  // 2. Lock Channel 6 and enable Promiscuous packet sniffing
  ESP_ERROR_CHECK(esp_wifi_set_promiscuous(true));
  ESP_ERROR_CHECK(esp_wifi_set_channel(WIFI_CHANNEL, WIFI_SECOND_CHAN_NONE));

  // Initialize ESP-NOW so Baseband decodes incoming ESP-NOW Action frames cleanly
  esp_now_init();

  // 3. Configure ESP-IDF CSI Subsystem (This is what was missing in your original code!)
  wifi_csi_config_t csi_config;
  csi_config.lltf_en           = true;  // Enable Legacy Long Training Field (52 subcarriers)
  csi_config.htltf_en          = true;  // Enable HT Long Training Field (64 subcarriers)
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
  // Non-blocking Downlink Listener for FR-05 Hardware Actuation
  // Python sends '1' to trigger buzzer/LEDs, '0' to disarm
  while (Serial.available() > 0) {
    char cmd = (char)Serial.read();
    if (cmd == '1') {
      digitalWrite(ALARM_GPIO_PIN, HIGH);
    } else if (cmd == '0') {
      digitalWrite(ALARM_GPIO_PIN, LOW);
    }
  }
  delay(5);
}