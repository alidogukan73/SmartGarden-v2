"""Regression checks for the wireless sensor startup grace period."""

from __future__ import annotations

import time
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.hardware_test_stubs import install_hardware_import_stubs

install_hardware_import_stubs()

from hardware.sensor_provider import SoilMoistureSensorProvider


class FakeMqttSensor:
    def __init__(self) -> None:
        self.reading = None

    def get_latest_reading(self):
        return self.reading


def main() -> None:
    provider = SoilMoistureSensorProvider.__new__(
        SoilMoistureSensorProvider,
    )
    provider._mode = "mqtt"
    provider._initialized = True
    provider._mqtt_sensor = FakeMqttSensor()
    provider._mqtt_startup_timeout_seconds = 20.0
    provider._initialized_at_monotonic = time.monotonic()

    assert provider.is_waiting_for_first_reading() is True

    provider._mqtt_sensor.reading = object()
    assert provider.is_waiting_for_first_reading() is False

    provider._mqtt_sensor.reading = None
    provider._initialized_at_monotonic = time.monotonic() - 21.0
    assert provider.is_waiting_for_first_reading() is False

    provider._mode = "wired"
    assert provider.is_waiting_for_first_reading() is False

    print("[PASS] Wireless sensor startup grace scenarios.")


if __name__ == "__main__":
    main()
