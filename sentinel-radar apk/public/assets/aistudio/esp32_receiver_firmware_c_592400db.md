/*
 * PROJECT SENTINEL: Wi-Fi CSI Radar Receiver
 * Hardware: ESP32-WROOM-32
 * Description: Sets the ESP32 into promiscuous mode to intercept 802.11 packets 
 * and extract the Channel State Information (CSI) matrix. 
 * The data is piped via high-speed UART Serial to the Python backend.
 */

#include <WiFi.h>
#include <esp_wifi.h>

// Set this to the channel your TX ESP32 is broadcasting on
#define WIFI_CHANNEL 6 
#define BAUD_RATE 921600 // High baud rate is critical for real-time DSP

// The CSI callback function triggered by the ESP-IDF Wi-Fi driver
void csi_callback(void *ctx, wifi_csi_info_t *data) {
  // data->mac holds the sender's MAC address. 
  // In a full implementation, you filter for your specific TX node here.
  
  wifi_csi_info_t d = data[0];
  int8_t *mac = d.mac;
  
  // Print packet header (MAC and RSSI)
  Serial.printf("CSI_DATA,%02X:%02X:%02X:%02X:%02X:%02X,%d,", 
                mac[0], mac[1], mac[2], mac[3], mac[4], mac[5], d.rx_ctrl.rssi);

  // The CSI payload contains imaginary and real parts for 52 subcarriers (104 bytes total for 20MHz bandwidth)
  int payload_len = d.len;
  for (int i = 0; i < payload_len; i++) {
    Serial.printf("%d", d.buf[i]);
    if (i < payload_len - 1) {
      Serial.print(",");
    }
  }
  Serial.println();
}

void setup() {
  Serial.begin(BAUD_RATE);
  Serial.println("Initializing Project Sentinel RX Node...");

  // 1. Initialize Wi-Fi
  WiFi.mode(WIFI_MODE_STA);
  WiFi.disconnect();
  
  // 2. Configure ESP32 Promiscuous Mode
  wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
  esp_wifi_init(&cfg);
  esp_wifi_set_storage(WIFI_STORAGE_RAM);
  esp_wifi_set_mode(WIFI_MODE_NULL);
  
  // 3. Start Wi-Fi driver on a specific channel
  esp_wifi_start();
  esp_wifi_set_channel(WIFI_CHANNEL, WIFI_SECOND_CHAN_NONE);

  // 4. Register the CSI Callback Function
  ESP_ERROR_CHECK(esp_wifi_set_promiscuous(true));
  ESP_ERROR_CHECK(esp_wifi_set_promiscuous_rx_cb(&csi_callback));
  
  Serial.println("System Ready. Intercepting RF Subcarriers...");
}

void loop() {
  // The main loop remains empty. The ESP-IDF handles CSI extraction asynchronously via the callback.
  delay(1000); 
}