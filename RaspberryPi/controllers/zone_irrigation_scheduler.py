"""
Deterministic scheduler for a shared pump and multiple garden valves.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ZoneIrrigationCandidate:
    zone_id: str
    sensor_id: str
    valve_id: str
    order: int
    moisture: int
    moisture_limit: int
    irrigation_enabled: bool
    should_water: bool
    reason: str

    @property
    def moisture_deficit(self) -> int:
        return max(
            0,
            self.moisture_limit - self.moisture,
        )


class ZoneIrrigationScheduler:
    """
    Select at most one eligible zone for the shared pump.

    The driest eligible zone wins. Garden order is used as
    a deterministic tie breaker.
    """

    def select(
        self,
        candidates: list[ZoneIrrigationCandidate],
    ) -> ZoneIrrigationCandidate | None:
        eligible = [
            candidate
            for candidate in candidates
            if (
                candidate.irrigation_enabled
                and candidate.should_water
                and candidate.valve_id
            )
        ]

        if not eligible:
            return None

        return min(
            eligible,
            key=lambda candidate: (
                -candidate.moisture_deficit,
                candidate.order,
                candidate.zone_id,
            ),
        )
