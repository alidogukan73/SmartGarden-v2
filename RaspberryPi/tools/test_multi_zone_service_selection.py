"""
Service-level selection test for multi-zone automatic irrigation.
"""

from __future__ import annotations

import logging
import sys
import types
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

try:
    import RPi.GPIO  # noqa: F401
except ModuleNotFoundError:
    rpi = types.ModuleType("RPi")
    gpio = types.ModuleType("RPi.GPIO")
    rpi.GPIO = gpio
    sys.modules["RPi"] = rpi
    sys.modules["RPi.GPIO"] = gpio

from controllers.multi_zone_decision_engine import (
    MultiZoneDecisionEngine,
)
from controllers.zone_irrigation_scheduler import (
    ZoneIrrigationScheduler,
)
from models.command_state import CommandState
from models.sensor_reading import SensorReading
from services.irrigation_service import IrrigationService


class FakeFirebase:
    def __init__(self) -> None:
        self.states = {}
        self.configs = {
            "soil-001": self._zone(
                "zone-001",
                "valve-001",
                1,
            ),
            "soil-002": self._zone(
                "zone-002",
                "valve-002",
                2,
            ),
        }

    @staticmethod
    def _zone(
        zone_id: str,
        valve_id: str,
        order: int,
    ) -> dict:
        return {
            "zone_id": zone_id,
            "valve_id": valve_id,
            "order": order,
            "enabled": True,
            "irrigation_enabled": True,
            "moisture_limit": 40,
            "pump_duration": 10,
            "cooldown_seconds": 600,
            "restart_delta": 10,
        }

    def get_all_zone_configs_by_sensor(self) -> dict:
        return self.configs

    def update_zone_irrigation_decisions(
        self,
        states: dict,
    ) -> None:
        self.states = states


class FakeExecutor:
    def is_cooldown_active(self, _zone_id: str) -> bool:
        return False

    def cooldown_remaining_for(self, _zone_id: str) -> int:
        return 0

    def cooldown_until_epoch_for(self, _zone_id: str) -> int:
        return 0


def reading(sensor_id: str, moisture: int) -> SensorReading:
    return SensorReading(
        raw=2000,
        voltage=0.25,
        moisture=moisture,
        sensor_id=sensor_id,
        firmware="test",
        rssi=-60,
        uptime_seconds=100,
    )


def main() -> None:
    service = IrrigationService.__new__(IrrigationService)
    service._firebase = FakeFirebase()
    service._multi_zone_engine = MultiZoneDecisionEngine()
    service._zone_scheduler = ZoneIrrigationScheduler()
    service._zone_executor = FakeExecutor()
    service._last_multi_zone_status_signature = None
    service._last_zone_config_signature = None
    service._logger = logging.getLogger("zone-selection-test")

    commands = CommandState(
        enabled=True,
        auto_mode=True,
        moisture_limit=40,
        pump_duration=10,
        cooldown_seconds=600,
        restart_delta=10,
    )
    readings = {
        "soil-001": reading("soil-001", 32),
        "soil-002": reading("soil-002", 24),
    }

    selected = None
    for _ in range(5):
        selected = service._update_multi_zone_decisions(
            readings=readings,
            global_commands=commands,
        )

    assert selected is not None
    assert selected.candidate.zone_id == "zone-002"
    assert service._firebase.states["zone-002"][
        "selected_for_watering"
    ] is True
    assert service._firebase.states["zone-002"][
        "queue_position"
    ] == 1

    repeated = service._update_multi_zone_decisions(
        readings=readings,
        global_commands=commands,
    )
    assert repeated is not None
    assert repeated.candidate.zone_id == "zone-002"

    print(
        "[PASS] Service selected the driest eligible zone.",
    )


if __name__ == "__main__":
    main()
