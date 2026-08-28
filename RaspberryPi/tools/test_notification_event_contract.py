"""Regression checks for the language-neutral Pi-to-Android notification contract."""

from __future__ import annotations

import sys
import types
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

try:
    import paho.mqtt.client  # noqa: F401
except ModuleNotFoundError:
    paho = types.ModuleType("paho")
    paho_mqtt = types.ModuleType("paho.mqtt")
    mqtt_client = types.ModuleType("paho.mqtt.client")
    paho.mqtt = paho_mqtt
    paho_mqtt.client = mqtt_client
    sys.modules["paho"] = paho
    sys.modules["paho.mqtt"] = paho_mqtt
    sys.modules["paho.mqtt.client"] = mqtt_client

from core.firebase_service import FirebaseService


def main() -> None:
    payload = FirebaseService._notification_event_payload(
        event_code="irrigation_completed",
        event_id="watering:zone-002:record-42",
        zone_id="zone-002",
        duration_seconds=120,
    )

    assert payload == {
        "event_code": "IRRIGATION_COMPLETED",
        "event_id": "watering:zone-002:record-42",
        "zone_id": "zone-002",
        "duration_seconds": "120",
    }
    assert "title" not in payload
    assert "description" not in payload
    assert "type" not in payload
    assert "priority" not in payload

    print("Notification event contract tests passed.")


if __name__ == "__main__":
    main()
