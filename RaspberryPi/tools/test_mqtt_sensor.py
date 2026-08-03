from __future__ import annotations

import json
import signal
import sys
from dataclasses import dataclass
from datetime import datetime
from typing import Any

import paho.mqtt.client as mqtt


MQTT_BROKER = "127.0.0.1"
MQTT_PORT = 1883
MQTT_TOPIC = "smartgarden/sensors/soil-001"
MQTT_CLIENT_ID = "smartgarden-pi-test"


@dataclass(frozen=True)
class WirelessSensorReading:
    sensor_id: str
    raw: int
    voltage: float
    moisture: int
    rssi: int
    received_at: datetime


def parse_sensor_payload(payload: bytes) -> WirelessSensorReading:
    try:
        text = payload.decode("utf-8")
        data: dict[str, Any] = json.loads(text)
    except UnicodeDecodeError as exc:
        raise ValueError("Mesaj UTF-8 formatında değil.") from exc
    except json.JSONDecodeError as exc:
        raise ValueError("Mesaj geçerli JSON değil.") from exc

    required_fields = {
        "sensor_id",
        "raw",
        "voltage",
        "moisture",
        "rssi",
    }

    missing_fields = required_fields.difference(data)

    if missing_fields:
        missing_text = ", ".join(sorted(missing_fields))
        raise ValueError(f"Eksik alanlar: {missing_text}")

    sensor_id = str(data["sensor_id"])
    raw = int(data["raw"])
    voltage = float(data["voltage"])
    moisture = int(data["moisture"])
    rssi = int(data["rssi"])

    if not sensor_id:
        raise ValueError("sensor_id boş olamaz.")

    if not 0 <= moisture <= 100:
        raise ValueError(
            f"Nem değeri 0–100 aralığında değil: {moisture}"
        )

    if raw < 0:
        raise ValueError(f"Raw değeri negatif olamaz: {raw}")

    if voltage < 0:
        raise ValueError(
            f"Voltaj değeri negatif olamaz: {voltage}"
        )

    return WirelessSensorReading(
        sensor_id=sensor_id,
        raw=raw,
        voltage=voltage,
        moisture=moisture,
        rssi=rssi,
        received_at=datetime.now().astimezone(),
    )


def on_connect(
    client: mqtt.Client,
    userdata: Any,
    flags: mqtt.ConnectFlags,
    reason_code: mqtt.ReasonCode,
    properties: mqtt.Properties | None,
) -> None:
    if reason_code != 0:
        print(
            f"HATA: MQTT broker bağlantısı başarısız. "
            f"Kod: {reason_code}"
        )
        return

    print("MQTT broker bağlantısı kuruldu.")
    print(f"Dinlenen konu: {MQTT_TOPIC}")

    result, message_id = client.subscribe(
        MQTT_TOPIC,
        qos=0,
    )

    if result != mqtt.MQTT_ERR_SUCCESS:
        print(
            f"HATA: Konuya abone olunamadı. Kod: {result}"
        )
        return

    print(f"Abonelik isteği gönderildi. Mesaj ID: {message_id}")
    print("ESP32 verisi bekleniyor...")
    print()


def on_disconnect(
    client: mqtt.Client,
    userdata: Any,
    disconnect_flags: mqtt.DisconnectFlags,
    reason_code: mqtt.ReasonCode,
    properties: mqtt.Properties | None,
) -> None:
    if reason_code == 0:
        print("\nMQTT bağlantısı kapatıldı.")
    else:
        print(
            f"\nUYARI: MQTT bağlantısı beklenmedik şekilde koptu. "
            f"Kod: {reason_code}"
        )


def on_message(
    client: mqtt.Client,
    userdata: Any,
    message: mqtt.MQTTMessage,
) -> None:
    try:
        reading = parse_sensor_payload(message.payload)
    except (ValueError, TypeError) as exc:
        print(f"HATALI MQTT MESAJI: {exc}")
        print(f"Ham mesaj: {message.payload!r}")
        print()
        return

    print("=" * 48)
    print(f"Sensör ID : {reading.sensor_id}")
    print(f"Raw       : {reading.raw}")
    print(f"Voltaj    : {reading.voltage:.3f} V")
    print(f"Nem       : %{reading.moisture}")
    print(f"Wi-Fi RSSI: {reading.rssi} dBm")
    print(
        "Alınma zamanı: "
        f"{reading.received_at.strftime('%Y-%m-%d %H:%M:%S')}"
    )


def create_mqtt_client() -> mqtt.Client:
    client = mqtt.Client(
        callback_api_version=mqtt.CallbackAPIVersion.VERSION2,
        client_id=MQTT_CLIENT_ID,
        protocol=mqtt.MQTTv311,
    )

    client.on_connect = on_connect
    client.on_disconnect = on_disconnect
    client.on_message = on_message

    client.reconnect_delay_set(
        min_delay=1,
        max_delay=30,
    )

    return client


def main() -> None:
    client = create_mqtt_client()

    def stop_program(
        signal_number: int,
        frame: Any,
    ) -> None:
        print("\nProgram kapatılıyor...")
        client.disconnect()

    signal.signal(signal.SIGINT, stop_program)
    signal.signal(signal.SIGTERM, stop_program)

    print("SmartGarden kablosuz sensör testi")
    print(f"Broker: {MQTT_BROKER}:{MQTT_PORT}")

    try:
        client.connect(
            MQTT_BROKER,
            MQTT_PORT,
            keepalive=60,
        )

        client.loop_forever(
            retry_first_connection=True
        )
    except ConnectionRefusedError:
        print(
            "HATA: MQTT bağlantısı reddedildi. "
            "Mosquitto servisinin çalıştığını kontrol et."
        )
        sys.exit(1)
    except OSError as exc:
        print(f"HATA: MQTT bağlantısı kurulamadı: {exc}")
        sys.exit(1)
    except KeyboardInterrupt:
        client.disconnect()


if __name__ == "__main__":
    main()