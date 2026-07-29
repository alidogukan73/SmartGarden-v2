"""
Prediction validation engine.
"""

from __future__ import annotations

from datetime import datetime

from models.pending_prediction_validation import (
    PendingPredictionValidation,
)


class PredictionValidationEngine:
    """
    Validates moisture predictions against future
    sensor measurements.

    Observation mode only.

    This engine never controls irrigation hardware
    and never changes irrigation settings.
    """

    SUPPORTED_TARGET_MINUTES = (
        60,
        180,
        360,
    )

    def is_due(
        self,
        *,
        pending: PendingPredictionValidation,
        current_time: datetime | None = None,
    ) -> bool:
        """
        Return True when the pending prediction has
        reached its validation time.
        """

        now = current_time or datetime.now()

        try:
            validate_at = datetime.fromisoformat(
                pending.validate_at,
            )

        except ValueError as exc:
            raise ValueError(
                "Invalid validation timestamp: "
                f"{pending.validate_at}"
            ) from exc

        return now >= validate_at

    def get_predicted_moisture(
        self,
        *,
        pending: PendingPredictionValidation,
    ) -> float:
        """
        Return the prediction value matching the
        requested validation period.
        """

        target_minutes = pending.target_minutes

        if target_minutes == 60:
            return (
                pending.prediction
                .predicted_moisture_1_hour
            )

        if target_minutes == 180:
            return (
                pending.prediction
                .predicted_moisture_3_hours
            )

        if target_minutes == 360:
            return (
                pending.prediction
                .predicted_moisture_6_hours
            )

        raise ValueError(
            "Unsupported prediction validation target: "
            f"{target_minutes} minutes. "
            "Supported targets are 60, 180 and 360."
        )

    def calculate_error(
        self,
        *,
        pending: PendingPredictionValidation,
        actual_moisture: float,
    ) -> float:
        """
        Calculate the absolute difference between
        predicted and measured moisture.
        """

        predicted_moisture = (
            self.get_predicted_moisture(
                pending=pending,
            )
        )

        return round(
            abs(
                predicted_moisture
                - actual_moisture
            ),
            2,
        )

    def validate(
        self,
        *,
        pending: PendingPredictionValidation,
        actual_moisture: float,
        current_time: datetime | None = None,
    ) -> tuple[float, float] | None:
        """
        Validate a prediction when its target time arrives.

        Returns:

            (
                predicted_moisture,
                absolute_error,
            )

        Returns None when validation time has not arrived.
        """

        if not self.is_due(
            pending=pending,
            current_time=current_time,
        ):
            return None

        predicted_moisture = (
            self.get_predicted_moisture(
                pending=pending,
            )
        )

        error = self.calculate_error(
            pending=pending,
            actual_moisture=actual_moisture,
        )

        return (
            predicted_moisture,
            error,
        )