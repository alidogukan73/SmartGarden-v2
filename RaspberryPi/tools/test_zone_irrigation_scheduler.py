"""
Scenarios for the shared-pump garden-zone scheduler.
"""

from __future__ import annotations

import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from controllers.zone_irrigation_scheduler import (
    ZoneIrrigationCandidate,
    ZoneIrrigationScheduler,
)


def candidate(
    zone_id: str,
    *,
    moisture: int,
    limit: int = 40,
    order: int = 1,
    enabled: bool = True,
    should_water: bool = True,
    valve_id: str = "valve-001",
) -> ZoneIrrigationCandidate:
    return ZoneIrrigationCandidate(
        zone_id=zone_id,
        sensor_id=zone_id.replace("zone", "soil"),
        valve_id=valve_id,
        order=order,
        moisture=moisture,
        moisture_limit=limit,
        irrigation_enabled=enabled,
        should_water=should_water,
        reason="MOISTURE_BELOW_LIMIT",
    )


def main() -> None:
    scheduler = ZoneIrrigationScheduler()

    assert scheduler.select([]) is None
    assert scheduler.select([
        candidate("zone-001", moisture=20, enabled=False),
    ]) is None
    assert scheduler.select([
        candidate("zone-001", moisture=20, valve_id=""),
    ]) is None

    selected = scheduler.select([
        candidate("zone-001", moisture=35, order=1),
        candidate("zone-002", moisture=25, order=2),
        candidate("zone-003", moisture=30, order=3),
    ])
    assert selected is not None
    assert selected.zone_id == "zone-002"

    tie = scheduler.select([
        candidate("zone-002", moisture=25, order=2),
        candidate("zone-001", moisture=25, order=1),
    ])
    assert tie is not None
    assert tie.zone_id == "zone-001"

    print(
        "[PASS] Shared-pump zone scheduling scenarios.",
    )


if __name__ == "__main__":
    main()
