"""
Manual test for PredictionValidationStatus.

Run from the RaspberryPi project root:

    python tools/test_prediction_validation_status.py
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


from models.prediction_validation_status import (
    PredictionValidationStatus,
)


def test_waiting_status() -> None:
    """
    Test a waiting validation status.
    """

    now = datetime.now().isoformat()

    status = PredictionValidationStatus(
        validation_status="WAITING",
        pending_count=1,
        target_minutes=60,
        next_validation_at=now,
        remaining_seconds=1800,
        updated_at=now,
    )

    assert status.validation_status == "WAITING"
    assert status.pending_count == 1
    assert status.target_minutes == 60
    assert status.remaining_seconds == 1800

    print(
        "[PASS] Waiting validation status created.",
    )


def test_idle_status() -> None:
    """
    Test an empty validation queue status.
    """

    now = datetime.now().isoformat()

    status = PredictionValidationStatus(
        validation_status="IDLE",
        pending_count=0,
        target_minutes=0,
        next_validation_at="",
        remaining_seconds=0,
        updated_at=now,
    )

    assert status.validation_status == "IDLE"
    assert status.pending_count == 0
    assert status.target_minutes == 0
    assert status.next_validation_at == ""
    assert status.remaining_seconds == 0

    print(
        "[PASS] Idle validation status created.",
    )


def test_frozen_model() -> None:
    """
    Test that the model cannot be modified.
    """

    now = datetime.now().isoformat()

    status = PredictionValidationStatus(
        validation_status="IDLE",
        pending_count=0,
        target_minutes=0,
        next_validation_at="",
        remaining_seconds=0,
        updated_at=now,
    )

    try:
        status.pending_count = 1

    except Exception:
        print(
            "[PASS] Validation status model is immutable.",
        )

        return

    raise AssertionError(
        "PredictionValidationStatus must be immutable."
    )


def main() -> None:
    """
    Run all model tests.
    """

    print(
        "\nPredictionValidationStatus tests started.\n"
    )

    test_waiting_status()
    test_idle_status()
    test_frozen_model()

    print(
        "\nAll PredictionValidationStatus "
        "tests passed successfully.\n"
    )


if __name__ == "__main__":
    main()