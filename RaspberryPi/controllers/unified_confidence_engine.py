"""
Unified AI confidence engine.
"""

from __future__ import annotations

from datetime import datetime

from models.prediction_accuracy import (
    PredictionAccuracy,
)

from models.soil_learning_profile import (
    SoilLearningProfile,
)

from models.unified_confidence import (
    UnifiedConfidence,
)


class UnifiedConfidenceEngine:
    """
    Combines every AI confidence source into one score.

    Observation mode only.
    """

    SOIL_WEIGHT = 0.25

    PREDICTION_WEIGHT = 0.20

    CONNECTION_WEIGHT = 0.10

    MEASUREMENT_WEIGHT = 0.20

    DECISION_WEIGHT = 0.15

    TREND_WEIGHT = 0.10

    def analyze(
        self,
        *,
        soil_profile: SoilLearningProfile,
        prediction_accuracy: PredictionAccuracy,
        trend_confidence: float,
        sensor_confidence: float | None = None,
        connection_confidence: float | None = None,
        measurement_confidence: float | None = None,
        decision_confidence: float | None = None,
    ) -> UnifiedConfidence:
        """
        Produce one unified AI confidence score.
        """

        legacy_sensor = self._bounded(
            sensor_confidence if sensor_confidence is not None else 0.0,
        )
        connection_confidence = self._bounded(
            legacy_sensor
            if connection_confidence is None
            else connection_confidence,
        )
        measurement_confidence = self._bounded(
            legacy_sensor
            if measurement_confidence is None
            else measurement_confidence,
        )
        trend_confidence = self._bounded(
            trend_confidence,
        )
        decision_confidence = self._bounded(
            min(measurement_confidence, trend_confidence)
            if decision_confidence is None
            else decision_confidence,
        )

        prediction_score = (
            prediction_accuracy.accuracy_percent
            / 100.0
        )
        prediction_score = self._bounded(
            prediction_score,
        )

        weighted = (

            soil_profile.confidence
            * self.SOIL_WEIGHT

            +

            prediction_score
            * self.PREDICTION_WEIGHT

            +

            connection_confidence
            * self.CONNECTION_WEIGHT

            +

            measurement_confidence
            * self.MEASUREMENT_WEIGHT

            +

            decision_confidence
            * self.DECISION_WEIGHT

            +

            trend_confidence
            * self.TREND_WEIGHT

        )

        weighted = max(
            0.0,
            min(
                weighted,
                1.0,
            ),
        )

        return UnifiedConfidence(

            overall_confidence=round(
                weighted,
                2,
            ),

            confidence_level=self._level(
                weighted,
            ),

            soil_learning_confidence=round(
                soil_profile.confidence,
                2,
            ),

            prediction_accuracy=round(
                prediction_score,
                2,
            ),

            connection_confidence=round(
                connection_confidence,
                2,
            ),

            measurement_confidence=round(
                measurement_confidence,
                2,
            ),

            decision_confidence=round(
                decision_confidence,
                2,
            ),

            # Backward-compatible alias for existing Android installations.
            sensor_confidence=round(
                measurement_confidence,
                2,
            ),

            trend_confidence=round(
                trend_confidence,
                2,
            ),

            weighted_score=round(
                weighted,
                4,
            ),

            status=(
                "READY"
                if (
                    soil_profile.profile_status == "READY"
                    and prediction_accuracy.status == "READY"
                )
                else "LEARNING"
            ),

            generated_at=datetime.now().isoformat(),
        )

    @staticmethod
    def _bounded(
        value: float,
    ) -> float:
        return max(
            0.0,
            min(
                float(value),
                1.0,
            ),
        )

    def _level(
        self,
        confidence: float,
    ) -> str:

        if confidence >= 0.90:
            return "VERY_HIGH"

        if confidence >= 0.75:
            return "HIGH"

        if confidence >= 0.55:
            return "MEDIUM"

        return "LOW"
