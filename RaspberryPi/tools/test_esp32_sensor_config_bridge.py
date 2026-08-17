"""Focused checks for Firebase-to-ESP32 sensor configuration bridging."""

from __future__ import annotations

import logging

from core.firebase_service import FirebaseService


class FakePublisher:
    def __init__(self) -> None:
        self.calls: list[tuple[str, bool, int, int]] = []

    def publish(
        self,
        sensor_id: str,
        enabled: bool,
        dry_raw: int,
        wet_raw: int,
    ) -> None:
        self.calls.append((sensor_id, enabled, dry_raw, wet_raw))


def make_service() -> FirebaseService:
    service = FirebaseService.__new__(FirebaseService)
    service._logger = logging.getLogger("test")
    service._published_sensor_configs = {}
    service._sensor_config_publisher = FakePublisher()
    return service


def main() -> None:
    service = make_service()
    publisher = service._sensor_config_publisher

    service._publish_saved_sensor_configs(
        {
            "soil-001": {"sensor_id": "soil-001"},
            "soil-002": {
                "sensor_enabled": True,
                "sensor_calibration_dry_raw": 12650,
                "sensor_calibration_wet_raw": 505,
            },
        }
    )
    assert publisher.calls == [("soil-002", True, 12650, 505)]

    service._publish_saved_sensor_configs(
        {
            "soil-002": {
                "sensor_enabled": True,
                "sensor_calibration_dry_raw": 12650,
                "sensor_calibration_wet_raw": 505,
            },
        }
    )
    assert len(publisher.calls) == 1

    service._publish_saved_sensor_configs(
        {
            "soil-002": {
                "sensor_enabled": False,
                "sensor_calibration_dry_raw": 12000,
                "sensor_calibration_wet_raw": 600,
            },
        }
    )
    assert publisher.calls[-1] == ("soil-002", False, 12000, 600)
    print("[PASS] Firebase-to-ESP32 sensor configuration bridge.")


if __name__ == "__main__":
    main()
