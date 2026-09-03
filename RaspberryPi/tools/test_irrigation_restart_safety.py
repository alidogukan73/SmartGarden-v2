"""Restart-safety checks for automatic irrigation runtime state."""

from controllers.smart_irrigation_engine import SmartIrrigationEngine
from models.command_state import CommandState
from models.pending_watering_measurement import (
    PendingWateringMeasurement,
)
from models.sensor_reading import SensorReading
from models.watering_record import WateringRecord
from models.watering_result import WateringResult


def commands() -> CommandState:
    return CommandState(
        auto_mode=True,
        relay=False,
        enabled=True,
        moisture_limit=40,
        pump_duration=10,
        restart_delta=10,
        cooldown_seconds=600,
    )


def reading(moisture: int) -> SensorReading:
    return SensorReading(
        raw=1000,
        voltage=1.0,
        moisture=moisture,
        sensor_id="soil-001",
    )


def verify_cycle_guard_restore() -> None:
    original = SmartIrrigationEngine()
    original.mark_watering_completed()
    original.mark_watering_completed()
    persisted = original.get_safety_state()

    restored = SmartIrrigationEngine()
    restored.restore_safety_state(
        completed_watering_cycles=persisted[
            "completed_watering_cycles"
        ],
        waiting_for_moisture_recovery=persisted[
            "waiting_for_moisture_recovery"
        ],
    )
    assert restored.get_safety_state() == persisted

    decision = None
    for _ in range(5):
        decision = restored.evaluate(
            reading=reading(30),
            commands=commands(),
            cooldown_active=False,
        )
    assert decision is not None
    assert decision.should_water

    restored.mark_watering_completed()
    for _ in range(5):
        decision = restored.evaluate(
            reading=reading(30),
            commands=commands(),
            cooldown_active=False,
        )
    assert decision is not None
    assert not decision.should_water
    assert decision.reason == "WAITING_FOR_MOISTURE_RECOVERY"

    bounded = SmartIrrigationEngine()
    bounded.restore_safety_state(
        completed_watering_cycles=99,
        waiting_for_moisture_recovery=False,
    )
    bounded_state = bounded.get_safety_state()
    assert bounded_state["completed_watering_cycles"] == 3
    assert bounded_state["waiting_for_moisture_recovery"]


def verify_pending_measurement_round_trip() -> None:
    result = WateringResult(
        completed=True,
        stop_reason="COMPLETED",
        duration=10,
    )
    record = WateringRecord(
        started_at="2026-08-20T10:00:00",
        finished_at="2026-08-20T10:00:10",
        duration=10,
        moisture_before=30,
        moisture_after=30,
        moisture_delta=0,
        moisture_limit=40,
        restart_delta=10,
        cooldown_seconds=600,
        completed=True,
        stop_reason="COMPLETED",
        mode="AUTO",
        firmware="2.9.0",
        zone_id="zone-001",
        sensor_id="soil-001",
        season_id="season-tomato",
        season_ids=("season-tomato", "season-pepper"),
    )
    pending = PendingWateringMeasurement(
        pending_key=record.firebase_key,
        finalize_after_epoch=1_800_000_000,
        result=result,
        record=record,
    )

    restored = PendingWateringMeasurement.from_payload(
        pending.to_payload()
    )
    assert restored == pending
    assert restored.record.season_id == "season-tomato"
    assert restored.record.season_ids == ("season-tomato", "season-pepper")
    assert PendingWateringMeasurement.from_payload({}) is None


def main() -> None:
    verify_cycle_guard_restore()
    verify_pending_measurement_round_trip()
    print("[PASS] Irrigation restart-safety scenarios.")


if __name__ == "__main__":
    main()
