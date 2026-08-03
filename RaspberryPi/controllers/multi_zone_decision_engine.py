"""
Independent smart-irrigation decisions for multiple garden zones.
"""

from __future__ import annotations

from dataclasses import dataclass

from controllers.smart_irrigation_engine import SmartIrrigationEngine
from controllers.zone_irrigation_scheduler import (
    ZoneIrrigationCandidate,
)
from models.command_state import CommandState
from models.irrigation_decision import IrrigationDecision
from models.sensor_reading import SensorReading


@dataclass(frozen=True)
class ZoneDecisionResult:
    candidate: ZoneIrrigationCandidate
    decision: IrrigationDecision


class MultiZoneDecisionEngine:
    """
    Keeps a separate learning/history engine for every sensor.
    """

    def __init__(self) -> None:
        self._engines: dict[str, SmartIrrigationEngine] = {}

    def evaluate(
        self,
        *,
        zone_id: str,
        valve_id: str,
        order: int,
        irrigation_enabled: bool,
        reading: SensorReading,
        commands: CommandState,
        cooldown_active: bool,
    ) -> ZoneDecisionResult:
        engine = self._engines.setdefault(
            reading.sensor_id,
            SmartIrrigationEngine(),
        )

        decision = engine.evaluate(
            reading=reading,
            commands=commands,
            cooldown_active=cooldown_active,
        )

        return ZoneDecisionResult(
            candidate=ZoneIrrigationCandidate(
                zone_id=zone_id,
                sensor_id=reading.sensor_id,
                valve_id=valve_id,
                order=order,
                moisture=decision.moisture,
                moisture_limit=decision.moisture_limit,
                irrigation_enabled=irrigation_enabled,
                should_water=decision.should_water,
                reason=decision.reason,
            ),
            decision=decision,
        )

    def mark_watering_completed(
        self,
        sensor_id: str,
    ) -> None:
        engine = self._engines.get(sensor_id)
        if engine is not None:
            engine.mark_watering_completed()
