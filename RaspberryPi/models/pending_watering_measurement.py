"""Persistent automatic-watering measurement awaiting a post-cooldown sample."""

from __future__ import annotations

from dataclasses import asdict
from dataclasses import dataclass

from models.watering_record import WateringRecord
from models.watering_result import WateringResult


@dataclass(frozen=True)
class PendingWateringMeasurement:
    pending_key: str
    finalize_after_epoch: int
    result: WateringResult
    record: WateringRecord

    def to_payload(self) -> dict:
        return {
            "pending_key": self.pending_key,
            "finalize_after_epoch": self.finalize_after_epoch,
            "result": asdict(self.result),
            "record": asdict(self.record),
        }

    @classmethod
    def from_payload(
        cls,
        payload: object,
    ) -> "PendingWateringMeasurement | None":
        if not isinstance(payload, dict):
            return None
        result_data = payload.get("result")
        record_data = payload.get("record")
        if not isinstance(result_data, dict) or not isinstance(record_data, dict):
            return None

        try:
            result = WateringResult(
                completed=bool(result_data.get("completed", False)),
                stop_reason=str(result_data.get("stop_reason", "")),
                duration=max(0, int(result_data.get("duration", 0))),
            )
            record = WateringRecord(
                started_at=str(record_data["started_at"]),
                finished_at=str(record_data["finished_at"]),
                duration=max(0, int(record_data.get("duration", 0))),
                moisture_before=int(record_data.get("moisture_before", 0)),
                moisture_after=int(record_data.get("moisture_after", 0)),
                moisture_delta=int(record_data.get("moisture_delta", 0)),
                moisture_limit=int(record_data.get("moisture_limit", 0)),
                restart_delta=int(record_data.get("restart_delta", 0)),
                cooldown_seconds=max(
                    0,
                    int(record_data.get("cooldown_seconds", 0)),
                ),
                completed=bool(record_data.get("completed", False)),
                stop_reason=str(record_data.get("stop_reason", "")),
                mode=str(record_data.get("mode", "AUTO")),
                firmware=str(record_data.get("firmware", "")),
                zone_id=str(record_data.get("zone_id", "")),
                sensor_id=str(record_data.get("sensor_id", "")),
                season_id=str(record_data.get("season_id", "")),
            )
            pending_key = str(
                payload.get("pending_key") or record.firebase_key
            )
            finalize_after_epoch = int(payload.get("finalize_after_epoch", 0))
        except (KeyError, TypeError, ValueError):
            return None

        if not pending_key or finalize_after_epoch <= 0:
            return None
        return cls(
            pending_key=pending_key,
            finalize_after_epoch=finalize_after_epoch,
            result=result,
            record=record,
        )

