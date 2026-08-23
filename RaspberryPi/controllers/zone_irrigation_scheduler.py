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
    hardware_ready: bool = True

    @property
    def moisture_deficit(self) -> int:
        return max(
            0,
            self.moisture_limit - self.moisture,
        )


class ZoneIrrigationScheduler:
    """
    Select at most one eligible zone for the shared pump.

    The driest eligible zone wins initially. After completed watering
    cycles, a bounded waiting bonus prevents another dry zone from being
    starved forever. Garden order remains the deterministic tie breaker.
    """

    FAIRNESS_BONUS_PER_WAITING_TURN = 4
    MAXIMUM_FAIRNESS_BONUS = 12

    def __init__(self) -> None:
        self._service_sequence = 0
        self._last_served_sequence: dict[str, int] = {}

    def select(
        self,
        candidates: list[ZoneIrrigationCandidate],
    ) -> ZoneIrrigationCandidate | None:
        ordered = self.ordered(candidates)
        return ordered[0] if ordered else None

    def ordered(
        self,
        candidates: list[ZoneIrrigationCandidate],
    ) -> list[ZoneIrrigationCandidate]:
        """Return the executable queue in fair, deterministic order."""

        eligible = [
            candidate
            for candidate in candidates
            if (
                candidate.irrigation_enabled
                and candidate.should_water
                and candidate.valve_id
                and candidate.hardware_ready
            )
        ]

        return sorted(
            eligible,
            key=lambda candidate: (
                -self._priority_score(candidate),
                -candidate.moisture_deficit,
                candidate.order,
                candidate.zone_id,
            ),
        )

    def mark_served(self, zone_id: str) -> None:
        """Record only a completed physical watering cycle."""

        normalized = str(zone_id or "").strip()
        if not normalized:
            return
        self._service_sequence += 1
        self._last_served_sequence[normalized] = self._service_sequence

    def waiting_turns(self, zone_id: str) -> int:
        """Return completed service turns elapsed since this zone ran."""

        if self._service_sequence <= 0:
            return 0
        last_served = self._last_served_sequence.get(zone_id)
        if last_served is None:
            return self._service_sequence
        return max(0, self._service_sequence - last_served)

    def _priority_score(self, candidate: ZoneIrrigationCandidate) -> int:
        waiting_bonus = min(
            self.MAXIMUM_FAIRNESS_BONUS,
            self.waiting_turns(candidate.zone_id)
            * self.FAIRNESS_BONUS_PER_WAITING_TURN,
        )
        return candidate.moisture_deficit + waiting_bonus
