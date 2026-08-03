"""
Prediction accuracy engine.
"""

from __future__ import annotations

from datetime import datetime

from models.moisture_prediction import MoisturePrediction
from models.prediction_accuracy import PredictionAccuracy


class PredictionAccuracyEngine:
    """
    Evaluates how accurate the prediction engine has been.

    Observation mode only.
    """

    MINIMUM_REQUIRED_PREDICTIONS = 10

    SUCCESSFUL_ERROR_PERCENT = 2.0

    def analyze(
        self,
        *,
        predictions: list[tuple[MoisturePrediction, float]],
    ) -> PredictionAccuracy:
        """
        Evaluate prediction accuracy.

        Each tuple contains:

            (
                prediction,
                actual_moisture
            )
        """

        if len(predictions) < self.MINIMUM_REQUIRED_PREDICTIONS:

            return PredictionAccuracy(
                prediction_count=len(predictions),
                successful_predictions=0,
                average_error=0.0,
                maximum_error=0.0,
                minimum_error=0.0,
                accuracy_percent=0.0,
                confidence_multiplier=1.0,
                status="INSUFFICIENT_DATA",
                generated_at=datetime.now().isoformat(),
            )

        errors: list[float] = []

        successful = 0

        for prediction, actual in predictions:

            error = abs(
                prediction.predicted_moisture_1_hour
                - actual
            )

            errors.append(error)

            if error <= self.SUCCESSFUL_ERROR_PERCENT:
                successful += 1

        average_error = (
            sum(errors)
            / len(errors)
        )

        accuracy = (
            successful
            / len(errors)
        ) * 100.0

        multiplier = self._confidence_multiplier(
            accuracy,
        )

        return PredictionAccuracy(
            prediction_count=len(errors),

            successful_predictions=successful,

            average_error=round(
                average_error,
                2,
            ),

            maximum_error=round(
                max(errors),
                2,
            ),

            minimum_error=round(
                min(errors),
                2,
            ),

            accuracy_percent=round(
                accuracy,
                2,
            ),

            confidence_multiplier=multiplier,

            status="READY",

            generated_at=datetime.now().isoformat(),
        )

    def _confidence_multiplier(
        self,
        accuracy: float,
    ) -> float:
        """
        Convert prediction accuracy into
        an AI confidence multiplier.
        """

        if accuracy >= 95:
            return 1.10

        if accuracy >= 90:
            return 1.05

        if accuracy >= 80:
            return 1.00

        if accuracy >= 70:
            return 0.95

        return 0.90