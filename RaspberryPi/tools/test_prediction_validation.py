"""
Manual test for PredictionValidationEngine.

Run from the project root:

    python tools/test_prediction_validation.py
"""

from __future__ import annotations

import sys
from datetime import datetime, timedelta
from pathlib import Path


# ---------------------------------------------------------
# Allow imports from the project root.
# ---------------------------------------------------------

PROJECT_ROOT = Path(__file__).resolve().parent.parent

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(
        0,
        str(PROJECT_ROOT),
    )


from controllers.prediction_validation_engine import (
    PredictionValidationEngine,
)

from models.moisture_prediction import (
    MoisturePrediction,
)

from models.pending_prediction_validation import (
    PendingPredictionValidation,
)


def create_prediction() -> MoisturePrediction:
    """
    Create a sample moisture prediction.
    """

    return MoisturePrediction(
        prediction_status="READY",
        prediction_method="LINEAR_TREND",

        current_moisture=55.0,
        moisture_limit=40.0,

        drying_rate_per_minute=0.05,

        predicted_moisture_1_hour=52.0,
        predicted_moisture_3_hours=46.0,
        predicted_moisture_6_hours=37.0,

        estimated_minutes_until_limit=300.0,

        estimated_limit_reached_at=(
            datetime.now()
            + timedelta(minutes=300)
        ).isoformat(),

        confidence=0.80,
        confidence_level="HIGH",

        generated_at=datetime.now().isoformat(),
    )


def test_due_prediction() -> None:
    """
    Test a prediction whose validation time has arrived.
    """

    engine = PredictionValidationEngine()

    pending = PendingPredictionValidation(
        prediction=create_prediction(),
        target_minutes=60,
        validate_at=(
            datetime.now()
            - timedelta(seconds=1)
        ).isoformat(),
    )

    result = engine.validate(
        pending=pending,
        actual_moisture=49.0,
    )

    expected = (
        52.0,
        3.0,
    )

    assert result == expected, (
        f"Expected {expected}, received {result}"
    )

    print(
        "[PASS] Due prediction validation:",
        result,
    )


def test_not_due_prediction() -> None:
    """
    Test a prediction whose validation time
    has not arrived yet.
    """

    engine = PredictionValidationEngine()

    pending = PendingPredictionValidation(
        prediction=create_prediction(),
        target_minutes=60,
        validate_at=(
            datetime.now()
            + timedelta(hours=1)
        ).isoformat(),
    )

    result = engine.validate(
        pending=pending,
        actual_moisture=49.0,
    )

    assert result is None, (
        f"Expected None, received {result}"
    )

    print(
        "[PASS] Not-due prediction returned None.",
    )


def test_three_hour_prediction() -> None:
    """
    Test selection of the three-hour prediction.
    """

    engine = PredictionValidationEngine()

    pending = PendingPredictionValidation(
        prediction=create_prediction(),
        target_minutes=180,
        validate_at=(
            datetime.now()
            - timedelta(seconds=1)
        ).isoformat(),
    )

    result = engine.validate(
        pending=pending,
        actual_moisture=44.0,
    )

    expected = (
        46.0,
        2.0,
    )

    assert result == expected, (
        f"Expected {expected}, received {result}"
    )

    print(
        "[PASS] Three-hour prediction validation:",
        result,
    )


def test_six_hour_prediction() -> None:
    """
    Test selection of the six-hour prediction.
    """

    engine = PredictionValidationEngine()

    pending = PendingPredictionValidation(
        prediction=create_prediction(),
        target_minutes=360,
        validate_at=(
            datetime.now()
            - timedelta(seconds=1)
        ).isoformat(),
    )

    result = engine.validate(
        pending=pending,
        actual_moisture=39.0,
    )

    expected = (
        37.0,
        2.0,
    )

    assert result == expected, (
        f"Expected {expected}, received {result}"
    )

    print(
        "[PASS] Six-hour prediction validation:",
        result,
    )


def test_unsupported_target() -> None:
    """
    Test rejection of an unsupported validation period.
    """

    engine = PredictionValidationEngine()

    pending = PendingPredictionValidation(
        prediction=create_prediction(),
        target_minutes=120,
        validate_at=(
            datetime.now()
            - timedelta(seconds=1)
        ).isoformat(),
    )

    try:
        engine.validate(
            pending=pending,
            actual_moisture=49.0,
        )

    except ValueError as exc:
        print(
            "[PASS] Unsupported target rejected:",
            exc,
        )

        return

    raise AssertionError(
        "Unsupported target did not raise ValueError."
    )


def main() -> None:
    """
    Run all PredictionValidationEngine tests.
    """

    print(
        "\nPredictionValidationEngine tests started.\n"
    )

    test_due_prediction()
    test_not_due_prediction()
    test_three_hour_prediction()
    test_six_hour_prediction()
    test_unsupported_target()

    print(
        "\nAll PredictionValidationEngine "
        "tests passed successfully.\n"
    )


if __name__ == "__main__":
    main()