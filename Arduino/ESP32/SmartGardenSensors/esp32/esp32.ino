//
// AVORA Wireless Soil Sensors v2.1.0
// Two ADS1115 modules, up to eight capacitive soil sensors.
//
#include <WiFi.h>
#include <Wire.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <PubSubClient.h>
#include <Adafruit_ADS1X15.h>

#include "secrets.h"

constexpr int SDA_PIN = 21;
constexpr int SCL_PIN = 22;
constexpr uint8_t ADS_PRIMARY_ADDRESS = 0x48;
constexpr uint8_t ADS_SECONDARY_ADDRESS = 0x49;
constexpr uint16_t MQTT_PORT = 1883;
constexpr unsigned long PUBLISH_INTERVAL_MS = 5000;
constexpr uint8_t FILTER_SAMPLE_COUNT = 10;
constexpr uint16_t FILTER_SAMPLE_DELAY_MS = 50;

const char* MQTT_BROKER = "192.168.1.99";
const char* FIRMWARE_VERSION = "2.1.0";
const char* SENSOR_CONFIG_TOPIC_FILTER =
        "smartgarden/config/esp32/sensors/#";
const char* CALIBRATION_CONFIG_TOPIC_FILTER =
        "smartgarden/config/esp32/calibration/#";
const char* SENSOR_CONFIG_TOPIC_PREFIX =
        "smartgarden/config/esp32/sensors/";
const char* CALIBRATION_CONFIG_TOPIC_PREFIX =
        "smartgarden/config/esp32/calibration/";

Adafruit_ADS1115 adsPrimary;
Adafruit_ADS1115 adsSecondary;
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

bool adsAvailable[] = {false, false};
unsigned long lastPublishMillis = 0;

struct SensorConfig {
    const char* id;
    uint8_t adsIndex;
    uint8_t channel;
    bool enabled;
    int16_t dryRaw;
    int16_t wetRaw;
};

// Enable a slot only after its sensor is physically connected.  This prevents
// floating ADS1115 inputs from being mistaken for real, online soil sensors.
SensorConfig sensors[] = {
    {"soil-001", 0, 0, true, 12650, 505},
    {"soil-002", 0, 1, true, 12650, 505},
    {"soil-003", 0, 2, false, 12650, 505},
    {"soil-004", 0, 3, false, 12650, 505},
    {"soil-005", 1, 0, false, 12650, 505},
    {"soil-006", 1, 1, false, 12650, 505},
    {"soil-007", 1, 2, false, 12650, 505},
    {"soil-008", 1, 3, false, 12650, 505},
};

constexpr size_t SENSOR_COUNT = sizeof(sensors) / sizeof(sensors[0]);

void setSensorEnabled(const char* sensorId, bool enabled) {
    for (SensorConfig& sensor : sensors) {
        if (strcmp(sensor.id, sensorId) != 0) {
            continue;
        }

        sensor.enabled = enabled;
        Serial.print("Sensor ayari guncellendi: ");
        Serial.print(sensor.id);
        Serial.print(" = ");
        Serial.println(enabled ? "ACIK" : "KAPALI");
        return;
    }

    Serial.print("Bilinmeyen sensor ayari yok sayildi: ");
    Serial.println(sensorId);
}

void setSensorCalibration(
        const char* sensorId,
        int16_t dryRaw,
        int16_t wetRaw
) {
    if (dryRaw <= wetRaw) {
        Serial.println("Gecersiz kalibrasyon: kuru deger islak degerden buyuk olmali.");
        return;
    }

    for (SensorConfig& sensor : sensors) {
        if (strcmp(sensor.id, sensorId) != 0) {
            continue;
        }

        sensor.dryRaw = dryRaw;
        sensor.wetRaw = wetRaw;
        Serial.print("Kalibrasyon guncellendi: ");
        Serial.print(sensor.id);
        Serial.print(" kuru=");
        Serial.print(dryRaw);
        Serial.print(" islak=");
        Serial.println(wetRaw);
        return;
    }

    Serial.print("Bilinmeyen sensor kalibrasyonu yok sayildi: ");
    Serial.println(sensorId);
}

bool parseCalibration(
        const byte* payload,
        unsigned int length,
        int16_t& dryRaw,
        int16_t& wetRaw
) {
    if (length == 0 || length >= 32) {
        return false;
    }

    char text[32];
    memcpy(text, payload, length);
    text[length] = '\0';

    char* separator = strchr(text, ',');
    if (separator == nullptr) {
        return false;
    }

    *separator = '\0';
    char* end = nullptr;
    long dry = strtol(text, &end, 10);
    if (*end != '\0' || dry < INT16_MIN || dry > INT16_MAX) {
        return false;
    }

    long wet = strtol(separator + 1, &end, 10);
    if (*end != '\0' || wet < INT16_MIN || wet > INT16_MAX) {
        return false;
    }

    dryRaw = static_cast<int16_t>(dry);
    wetRaw = static_cast<int16_t>(wet);
    return dryRaw > wetRaw;
}

// Retained MQTT command examples:
// smartgarden/config/esp32/sensors/soil-003     -> 1 (enable) / 0 (disable)
// smartgarden/config/esp32/calibration/soil-003 -> 12480,620 (dry,wet)
void onMqttMessage(char* topic, byte* payload, unsigned int length) {
    size_t sensorPrefixLength = strlen(SENSOR_CONFIG_TOPIC_PREFIX);
    size_t calibrationPrefixLength = strlen(CALIBRATION_CONFIG_TOPIC_PREFIX);

    if (strncmp(topic, SENSOR_CONFIG_TOPIC_PREFIX, sensorPrefixLength) == 0) {
        const char* sensorId = topic + sensorPrefixLength;
        if (*sensorId == '\0' || length != 1) {
            Serial.println("Gecersiz sensor ayari MQTT mesaji yok sayildi.");
            return;
        }

        char value = static_cast<char>(payload[0]);
        if (value != '0' && value != '1') {
            Serial.println("Sensor ayari sadece 0 veya 1 olabilir.");
            return;
        }

        setSensorEnabled(sensorId, value == '1');
        return;
    }

    if (strncmp(topic, CALIBRATION_CONFIG_TOPIC_PREFIX, calibrationPrefixLength) != 0) {
        Serial.println("Bilinmeyen MQTT ayar konusu yok sayildi.");
        return;
    }

    const char* sensorId = topic + calibrationPrefixLength;
    int16_t dryRaw;
    int16_t wetRaw;
    if (*sensorId == '\0' || !parseCalibration(payload, length, dryRaw, wetRaw)) {
        Serial.println("Gecersiz kalibrasyon mesaji yok sayildi.");
        return;
    }

    setSensorCalibration(sensorId, dryRaw, wetRaw);
}

int calculateMoisturePercent(int16_t raw, const SensorConfig& sensor) {
    float percentage =
            ((sensor.dryRaw - raw)
             / static_cast<float>(sensor.dryRaw - sensor.wetRaw))
            * 100.0f;

    return static_cast<int>(constrain(roundf(percentage), 0.0f, 100.0f));
}

Adafruit_ADS1115& adsFor(uint8_t adsIndex) {
    return adsIndex == 0 ? adsPrimary : adsSecondary;
}

int16_t readFilteredRaw(const SensorConfig& sensor) {
    long total = 0;
    Adafruit_ADS1115& ads = adsFor(sensor.adsIndex);

    for (uint8_t sample = 0; sample < FILTER_SAMPLE_COUNT; sample++) {
        total += ads.readADC_SingleEnded(sensor.channel);
        delay(FILTER_SAMPLE_DELAY_MS);
    }

    return static_cast<int16_t>(total / FILTER_SAMPLE_COUNT);
}

void connectToWiFi() {
    if (WiFi.status() == WL_CONNECTED) {
        return;
    }

    Serial.print("Wi-Fi baglantisi kuruluyor");
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    while (WiFi.status() != WL_CONNECTED) {
        Serial.print(".");
        delay(500);
    }

    Serial.println("\nWi-Fi baglantisi kuruldu.");
    Serial.print("IP adresi: ");
    Serial.println(WiFi.localIP());
}

void connectToMqtt() {
    while (!mqttClient.connected()) {
        String clientId =
                "smartgarden-esp32-" +
                String(static_cast<uint32_t>(ESP.getEfuseMac()), HEX);

        if (mqttClient.connect(clientId.c_str())) {
            Serial.println("MQTT baglantisi kuruldu.");
            if (!mqttClient.subscribe(SENSOR_CONFIG_TOPIC_FILTER)) {
                Serial.println("HATA: Sensor ayar konusuna abone olunamadi.");
            } else {
                Serial.println("Sensor ayarlari dinleniyor.");
            }
            if (!mqttClient.subscribe(CALIBRATION_CONFIG_TOPIC_FILTER)) {
                Serial.println("HATA: Kalibrasyon konusuna abone olunamadi.");
            } else {
                Serial.println("Kalibrasyon ayarlari dinleniyor.");
            }
            return;
        }

        Serial.print("MQTT hatasi, kod=");
        Serial.println(mqttClient.state());
        delay(5000);
    }
}

void publishSensorData(const SensorConfig& sensor) {
    int16_t raw = readFilteredRaw(sensor);
    float voltage = adsFor(sensor.adsIndex).computeVolts(raw);
    int moisture = calculateMoisturePercent(raw, sensor);

    char topic[64];
    char payload[240];
    snprintf(topic, sizeof(topic), "smartgarden/sensors/%s", sensor.id);
    snprintf(
            payload,
            sizeof(payload),
            "{\"sensor_id\":\"%s\",\"firmware\":\"%s\","
            "\"raw\":%d,\"voltage\":%.3f,"
            "\"moisture\":%d,\"rssi\":%d,\"uptime\":%lu}",
            sensor.id,
            FIRMWARE_VERSION,
            raw,
            voltage,
            moisture,
            WiFi.RSSI(),
            millis() / 1000
    );

    if (mqttClient.publish(topic, payload, false)) {
        Serial.print("MQTT gonderildi: ");
        Serial.println(payload);
    } else {
        Serial.print("MQTT gonderilemedi: ");
        Serial.println(sensor.id);
    }
}

void initializeAds() {
    adsAvailable[0] = adsPrimary.begin(ADS_PRIMARY_ADDRESS, &Wire);
    if (adsAvailable[0]) {
        adsPrimary.setGain(GAIN_ONE);
        Serial.println("ADS1115 #1 bulundu: 0x48");
    } else {
        Serial.println("HATA: Zorunlu ADS1115 #1 (0x48) bulunamadi.");
        while (true) {
            delay(1000);
        }
    }

    adsAvailable[1] = adsSecondary.begin(ADS_SECONDARY_ADDRESS, &Wire);
    if (adsAvailable[1]) {
        adsSecondary.setGain(GAIN_ONE);
        Serial.println("ADS1115 #2 bulundu: 0x49");
    } else {
        Serial.println("ADS1115 #2 (0x49) bekleniyor; soil-005..soil-008 pasif.");
    }
}

void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println("AVORA 8 kanalli sensor baslatiliyor...");

    Wire.begin(SDA_PIN, SCL_PIN);
    initializeAds();
    connectToWiFi();
    mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
    mqttClient.setCallback(onMqttMessage);
    connectToMqtt();
}

void loop() {
    if (WiFi.status() != WL_CONNECTED) {
        connectToWiFi();
    }
    if (!mqttClient.connected()) {
        connectToMqtt();
    }
    mqttClient.loop();

    unsigned long now = millis();
    if (now - lastPublishMillis < PUBLISH_INTERVAL_MS) {
        return;
    }
    lastPublishMillis = now;

    for (const SensorConfig& sensor : sensors) {
        if (!sensor.enabled || !adsAvailable[sensor.adsIndex]) {
            continue;
        }
        publishSensorData(sensor);
    }
}
