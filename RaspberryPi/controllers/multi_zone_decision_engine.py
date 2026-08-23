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
        hardware_ready: bool = True,
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
                hardware_ready=hardware_ready,
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

    def get_safety_state(self, sensor_id: str) -> dict:
        engine = self._engines.get(sensor_id)
        if engine is None:
            return {
                "completed_watering_cycles": 0,
                "waiting_for_moisture_recovery": False,
            }
        return engine.get_safety_state()

    def get_current_trend(self, sensor_id: str):
        """Return the independent moisture trend for one sensor."""
        engine = self._engines.setdefault(sensor_id, SmartIrrigationEngine())
        return engine.get_current_trend()

    def restore_safety_state(
        self,
        sensor_id: str,
        *,
        completed_watering_cycles: object,
        waiting_for_moisture_recovery: object,
    ) -> None:
        if not sensor_id:
            return
        engine = self._engines.setdefault(
            sensor_id,
            SmartIrrigationEngine(),
        )
        engine.restore_safety_state(
            completed_watering_cycles=completed_watering_cycles,
            waiting_for_moisture_recovery=waiting_for_moisture_recovery,
        )

    def reset(self, sensor_id: str) -> bool:
        """Reset only one sensor's transient decision window."""

        if not sensor_id:
            return False

        engine = self._engines.setdefault(
            sensor_id,
            SmartIrrigationEngine(),
        )
        engine.reset()
        return True
