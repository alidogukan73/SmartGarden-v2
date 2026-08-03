"""
Verify that zone sensor histories remain independent.
"""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from controllers.multi_zone_decision_engine import (
    MultiZoneDecisionEngine,
)
from controllers.zone_irrigation_scheduler import (
    ZoneIrrigationScheduler,
)
from models.command_state import CommandState
from models.sensor_reading import SensorReading


def main() -> None:
    engine = MultiZoneDecisionEngine()
    scheduler = ZoneIrrigationScheduler()
    commands = CommandState(
        moisture_limit=40,
        pump_duration=10,
        cooldown_seconds=600,
        restart_delta=10,
    )

    latest = {}
    for moisture in (31, 30, 31, 29, 30):
        latest["zone-001"] = engine.evaluate(
            zone_id="zone-001",
            valve_id="valve-001",
            order=1,
            irrigation_enabled=True,
            reading=SensorReading(
                raw=0,
                voltage=0.0,
                moisture=moisture,
                sensor_id="soil-001",
            ),
            commands=commands,
            cooldown_active=False,
        )

    for moisture in (55, 54, 55, 54):
        latest["zone-002"] = engine.evaluate(
            zone_id="zone-002",
            valve_id="valve-002",
            order=2,
            irrigation_enabled=True,
            reading=SensorReading(
                raw=0,
                voltage=0.0,
                moisture=moisture,
                sensor_id="soil-002",
            ),
            commands=commands,
            cooldown_active=False,
        )

    assert latest["zone-001"].decision.should_water
    assert not latest["zone-002"].decision.should_water
    assert (
        latest["zone-002"].decision.reason
        == "INSUFFICIENT_SENSOR_SAMPLES"
    )

    selected = scheduler.select([
        result.candidate
        for result in latest.values()
    ])
    assert selected is not None
    assert selected.zone_id == "zone-001"

    print(
        "[PASS] Multi-zone histories and selection are independent.",
    )


if __name__ == "__main__":
    main()
