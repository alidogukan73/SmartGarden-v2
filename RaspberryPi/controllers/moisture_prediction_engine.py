"""
Moisture prediction engine.
"""

from __future__ import annotations

from datetime import datetime, timedelta

from models.moisture_prediction import MoisturePrediction
from models.moisture_trend import MoistureTrend


class MoisturePredictionEngine:
    """
    Predict future soil moisture using the observed drying trend.

    Observation mode only.

    This engine never changes irrigation parameters and never
    controls irrigation hardware.
    """

    MINIMUM_REQUIRED_SAMPLES = 20

    MINIMUM_REQUIRED_DURATION_SECONDS = 300.0

    def analyze(
        self,
        *,
        trend: MoistureTrend,
        moisture: float,
        moisture_limit: float,
    ) -> MoisturePrediction:
        """
        Predict future moisture values.
        """

        if (
            trend.sample_count < self.MINIMUM_REQUIRED_SAMPLES
            or trend.duration_seconds < self.MINIMUM_REQUIRED_DURATION_SECONDS
        ):

            return self._prediction(
                prediction_status="INSUFFICIENT_DATA",
                prediction_method="LINEAR_TREND_V1",
                moisture=moisture,
                moisture_limit=moisture_limit,
                drying_rate=0.0,
            )

        if trend.classification == "RISING":

            return self._prediction(
                prediction_status="RISING",
                prediction_method="LINEAR_TREND_V1",
                moisture=moisture,
                moisture_limit=moisture_limit,
                drying_rate=0.0,
            )

        drying_rate = max(
            0.0,
            -trend.change_per_minute,
        )

        if drying_rate <= 0.0001:

            return self._prediction(
                prediction_status="STABLE",
                prediction_method="LINEAR_TREND_V1",
                moisture=moisture,
                moisture_limit=moisture_limit,
                drying_rate=0.0,
            )

        return self._prediction(
            prediction_status="READY",
            prediction_method="LINEAR_TREND_V1",
            moisture=moisture,
            moisture_limit=moisture_limit,
            drying_rate=drying_rate,
        )

    def _prediction(
        self,
        *,
        prediction_status: str,
        prediction_method: str,
        moisture: float,
        moisture_limit: float,
        drying_rate: float,
    ) -> MoisturePrediction:

        one_hour = max(
            0.0,
            moisture - drying_rate * 60,
        )

        three_hours = max(
            0.0,
            moisture - drying_rate * 180,
        )

        six_hours = max(
            0.0,
            moisture - drying_rate * 360,
        )

        if drying_rate <= 0:

            minutes_until_limit = 0.0
            limit_time = ""

        elif moisture <= moisture_limit:

            minutes_until_limit = 0.0
            limit_time = datetime.now().isoformat()

        else:

            minutes_until_limit = (
                moisture - moisture_limit
            ) / drying_rate

            limit_time = (
                datetime.now()
                + timedelta(
                    minutes=minutes_until_limit,
                )
            ).isoformat()

        confidence = 0.70 if prediction_status == "READY" else 0.0

        confidence_level = (
            "MEDIUM"
            if confidence >= 0.65
            else "LOW"
        )

        return MoisturePrediction(
            prediction_status=prediction_status,
            prediction_method=prediction_method,
            current_moisture=round(moisture, 2),
            moisture_limit=round(moisture_limit, 2),
            drying_rate_per_minute=round(drying_rate, 4),
            predicted_moisture_1_hour=round(one_hour, 2),
            predicted_moisture_3_hours=round(three_hours, 2),
            predicted_moisture_6_hours=round(six_hours, 2),
            estimated_minutes_until_limit=round(
                minutes_until_limit,
                2,
            ),
            estimated_limit_reached_at=limit_time,
            confidence=confidence,
            confidence_level=confidence_level,
            generated_at=datetime.now().isoformat(),
        )