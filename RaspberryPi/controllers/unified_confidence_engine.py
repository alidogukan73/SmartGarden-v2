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

    SOIL_WEIGHT = 0.35

    PREDICTION_WEIGHT = 0.30

    SENSOR_WEIGHT = 0.20

    TREND_WEIGHT = 0.15

    def analyze(
        self,
        *,
        soil_profile: SoilLearningProfile,
        prediction_accuracy: PredictionAccuracy,
        sensor_confidence: float,
        trend_confidence: float,
    ) -> UnifiedConfidence:
        """
        Produce one unified AI confidence score.
        """

        sensor_confidence = self._bounded(
            sensor_confidence,
        )
        trend_confidence = self._bounded(
            trend_confidence,
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

            sensor_confidence
            * self.SENSOR_WEIGHT

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

            sensor_confidence=round(
                sensor_confidence,
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
