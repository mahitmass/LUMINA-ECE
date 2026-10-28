/*
 * PROJECT SENTINEL: Deterministic 100Hz RF Injector (TX Node)
 * Hardware: ESP32-WROOM-32D
 * Description: Spoofs a custom locally-administered MAC address and transmits 
 * connectionless ESP-NOW packets at 100Hz on Channel 6 to excite the RF channel.
 */

#include <WiFi.h>
#include <esp_wifi.h>
#include <esp_now.h>

#define WIFI_CHANNEL 6
#define TX_INTERVAL_MS 10 // 100Hz deterministic sampling rate (Exceeds FR-01 50Hz min)

// Custom Locally-Administered MAC for strict RX filtering
const uint8_t SENTINEL_TX_MAC[6] = {0x1A, 0x00, 0x00, 0x00, 0x00, 0x01};
// Broadcast destination so RX captures frame without ACK handshake jitter
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

  // 1. Initialize Wi-Fi in Station Mode
  WiFi.mode(WIFI_MODE_STA);
  WiFi.disconnect();

  // 2. Spoof MAC Address so RX Node ignores ambient exhibition Wi-Fi noise
  ESP_ERROR_CHECK(esp_wifi_set_mac(WIFI_IF_STA, SENTINEL_TX_MAC));

  // 3. Lock Channel 6 (2.437 GHz) & disable power-save modem sleep
  ESP_ERROR_CHECK(esp_wifi_set_promiscuous(true));
  ESP_ERROR_CHECK(esp_wifi_set_channel(WIFI_CHANNEL, WIFI_SECOND_CHAN_NONE));
  ESP_ERROR_CHECK(esp_wifi_set_promiscuous(false));
  ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));

  // 4. Lock maximum TX power (19.5 dBm = 78 in 0.25dBm units) for wall penetration
  ESP_ERROR_CHECK(esp_wifi_set_max_tx_power(78));

  // 5. Initialize ESP-NOW protocol
  if (esp_now_init() != ESP_OK) {
    Serial.println("FATAL: ESP-NOW Init Failed!");
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
}