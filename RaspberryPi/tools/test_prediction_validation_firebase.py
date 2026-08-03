"""
Manual Firebase test for prediction validation status.

Run from the RaspberryPi project root:

    python tools/test_prediction_validation_firebase.py
"""

from __future__ import annotations

import sys
from datetime import datetime
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(
        0,
        str(PROJECT_ROOT),
    )


from core.firebase_service import FirebaseService

from models.prediction_validation_status import (
    PredictionValidationStatus,
)


def main() -> None:
    """
    Upload WAITING and IDLE test statuses.
    """

    firebase = FirebaseService()

    try:
        firebase.initialize()

        now = datetime.now().isoformat()

        waiting_status = PredictionValidationStatus(
            validation_status="WAITING",
            pending_count=1,
            target_minutes=60,
            next_validation_at=now,
            remaining_seconds=1800,
            updated_at=now,
        )

        firebase.update_prediction_validation_status(
            waiting_status,
        )

        waiting_data = (
            firebase._device_ref()
            .child("ai")
            .child("prediction_validation")
            .get()
        )

        assert waiting_data is not None
        assert (
            waiting_data["validation_status"]
            == "WAITING"
        )
        assert waiting_data["pending_count"] == 1
        assert waiting_data["target_minutes"] == 60
        assert waiting_data["remaining_seconds"] == 1800

        print(
            "[PASS] WAITING validation status uploaded.",
        )

        idle_time = datetime.now().isoformat()

        idle_status = PredictionValidationStatus(
            validation_status="IDLE",
            pending_count=0,
            target_minutes=0,
            next_validation_at="",
            remaining_seconds=0,
            updated_at=idle_time,
        )

        firebase.update_prediction_validation_status(
            idle_status,
        )

        idle_data = (
            firebase._device_ref()
            .child("ai")
            .child("prediction_validation")
            .get()
        )

        assert idle_data is not None
        assert idle_data["validation_status"] == "IDLE"
        assert idle_data["pending_count"] == 0
        assert idle_data["target_minutes"] == 0
        assert idle_data["next_validation_at"] == ""
        assert idle_data["remaining_seconds"] == 0

        print(
            "[PASS] IDLE validation status uploaded.",
        )

        print(
            "\nAll prediction validation Firebase "
            "tests passed successfully.\n"
        )

    finally:
        firebase.stop_command_sync()


if __name__ == "__main__":
    main()