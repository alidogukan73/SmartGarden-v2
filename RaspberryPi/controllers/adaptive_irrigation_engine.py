"""
Adaptive irrigation recommendation engine.
"""

from __future__ import annotations

from collections.abc import Iterable

from models.adaptive_irrigation_recommendation import (
    AdaptiveIrrigationRecommendation,
)
from models.watering_record import WateringRecord


class AdaptiveIrrigationEngine:
    """
    Produces safe irrigation-setting recommendations.

    Observation mode only:
    - Reads completed automatic watering records.
    - Produces recommendations.
    - Never changes Firebase commands automatically.
    """

    MINIMUM_COMPLETED_WATERINGS = 5

    MINIMUM_RECOMMENDED_PUMP_DURATION_SECONDS = 60
    MAXIMUM_RECOMMENDED_PUMP_DURATION_SECONDS = 7200

    MINIMUM_RECOMMENDED_COOLDOWN_SECONDS = 300
    MAXIMUM_RECOMMENDED_COOLDOWN_SECONDS = 3600

    MINIMUM_EFFECTIVE_MOISTURE_GAIN = 3.0
    HIGH_MOISTURE_GAIN = 12.0

    LOW_CONFIDENCE = 0.40
    MEDIUM_CONFIDENCE = 0.65
    HIGH_CONFIDENCE = 0.85

    MAXIMUM_CHANGE_RATIO = 0.20

    MINIMUM_VALID_MOISTURE_GAIN = 1
    MAXIMUM_VALID_MOISTURE_GAIN = 30

    SUPPORTED_FIRMWARE_PREFIX = "3."    

    def analyze(
        self,
        *,
        records: Iterable[WateringRecord],
        current_pump_duration_seconds: int,
        current_cooldown_seconds: int,
    ) -> AdaptiveIrrigationRecommendation:
        """
        Analyze watering history and produce one safe recommendation.
        """

        eligible_records = self._eligible_records(
            records
        )

        watering_count = len(
            eligible_records
        )

        if watering_count < self.MINIMUM_COMPLETED_WATERINGS:

            return self._recommendation(
                recommendation_type="INSUFFICIENT_DATA",
                reason="NOT_ENOUGH_COMPLETED_AUTO_WATERINGS",
                confidence=0.0,
                current_pump_duration_seconds=(
                    current_pump_duration_seconds
                ),
                recommended_pump_duration_seconds=(
                    current_pump_duration_seconds
                ),
                current_cooldown_seconds=(
                    current_cooldown_seconds
                ),
                recommended_cooldown_seconds=(
                    current_cooldown_seconds
                ),
                watering_count_analyzed=watering_count,
                average_moisture_delta=0.0,
                average_watering_duration_seconds=0.0,
            )

        average_moisture_delta = self._average(
            record.moisture_delta
            for record in eligible_records
        )

        average_duration = self._average(
            record.duration
            for record in eligible_records
        )

        confidence = self._calculate_confidence(
            watering_count=watering_count,
            average_moisture_delta=(
                average_moisture_delta
            ),
        )

        if (
            average_moisture_delta
            < self.MINIMUM_EFFECTIVE_MOISTURE_GAIN
        ):

            recommended_pump_duration = (
                self._increase_with_limit(
                    current_pump_duration_seconds
                )
            )

            return self._recommendation(
                recommendation_type=(
                    "INCREASE_PUMP_DURATION"
                ),
                reason=(
                    "AVERAGE_MOISTURE_GAIN_TOO_LOW"
                ),
                confidence=confidence,
                current_pump_duration_seconds=(
                    current_pump_duration_seconds
                ),
                recommended_pump_duration_seconds=(
                    recommended_pump_duration
                ),
                current_cooldown_seconds=(
                    current_cooldown_seconds
                ),
                recommended_cooldown_seconds=(
                    current_cooldown_seconds
                ),
                watering_count_analyzed=watering_count,
                average_moisture_delta=(
                    average_moisture_delta
                ),
                average_watering_duration_seconds=(
                    average_duration
                ),
            )

        if average_moisture_delta > self.HIGH_MOISTURE_GAIN:

            recommended_pump_duration = (
                self._decrease_with_limit(
                    current_pump_duration_seconds
                )
            )

            return self._recommendation(
                recommendation_type=(
                    "DECREASE_PUMP_DURATION"
                ),
                reason=(
                    "AVERAGE_MOISTURE_GAIN_HIGH"
                ),
                confidence=confidence,
                current_pump_duration_seconds=(
                    current_pump_duration_seconds
                ),
                recommended_pump_duration_seconds=(
                    recommended_pump_duration
                ),
                current_cooldown_seconds=(
                    current_cooldown_seconds
                ),
                recommended_cooldown_seconds=(
                    current_cooldown_seconds
                ),
                watering_count_analyzed=watering_count,
                average_moisture_delta=(
                    average_moisture_delta
                ),
                average_watering_duration_seconds=(
                    average_duration
                ),
            )

        return self._recommendation(
            recommendation_type="KEEP_CURRENT_SETTINGS",
            reason="MOISTURE_GAIN_IN_TARGET_RANGE",
            confidence=confidence,
            current_pump_duration_seconds=(
                current_pump_duration_seconds
            ),
            recommended_pump_duration_seconds=(
                current_pump_duration_seconds
            ),
            current_cooldown_seconds=(
                current_cooldown_seconds
            ),
            recommended_cooldown_seconds=(
                current_cooldown_seconds
            ),
            watering_count_analyzed=watering_count,
            average_moisture_delta=(
                average_moisture_delta
            ),
            average_watering_duration_seconds=(
                average_duration
            ),
        )

    def _eligible_records(
        self,
        records: Iterable[WateringRecord],
    ) -> list[WateringRecord]:
        """
        Keep only records safe for adaptive learning.
        """

        eligible_records: list[WateringRecord] = []

        for record in records:

            if not record.completed:
                continue

            if record.mode != "AUTO":
                continue

            if record.duration <= 0:
                continue

            if not record.firmware.startswith(
                self.SUPPORTED_FIRMWARE_PREFIX
            ):
                continue

            if (
                record.moisture_delta
                < self.MINIMUM_VALID_MOISTURE_GAIN
            ):
                continue

            if (
                record.moisture_delta
                > self.MAXIMUM_VALID_MOISTURE_GAIN
            ):
                continue

            if (
                record.moisture_after
                < record.moisture_before
            ):
                continue

            eligible_records.append(
                record
            )

        return eligible_records

    def _increase_with_limit(
        self,
        current_value: int,
    ) -> int:
        """
        Increase by at most 20 percent.
        """

        increased = round(
            current_value
            * (
                1.0
                + self.MAXIMUM_CHANGE_RATIO
            )
        )

        return self._clamp(
            increased,
            self.MINIMUM_RECOMMENDED_PUMP_DURATION_SECONDS,
            self.MAXIMUM_RECOMMENDED_PUMP_DURATION_SECONDS,
        )

    def _decrease_with_limit(
        self,
        current_value: int,
    ) -> int:
        """
        Decrease by at most 20 percent.
        """

        decreased = round(
            current_value
            * (
                1.0
                - self.MAXIMUM_CHANGE_RATIO
            )
        )

        return self._clamp(
            decreased,
            self.MINIMUM_RECOMMENDED_PUMP_DURATION_SECONDS,
            self.MAXIMUM_RECOMMENDED_PUMP_DURATION_SECONDS,
        )

    def _calculate_confidence(
        self,
        *,
        watering_count: int,
        average_moisture_delta: float,
    ) -> float:
        """
        Calculate a conservative confidence score.
        """

        sample_score = min(
            watering_count
            / 15.0,
            1.0,
        )

        if average_moisture_delta <= 0:

            effect_score = 0.0

        elif (
            average_moisture_delta
            < self.MINIMUM_EFFECTIVE_MOISTURE_GAIN
        ):

            effect_score = 0.50

        elif average_moisture_delta <= self.HIGH_MOISTURE_GAIN:

            effect_score = 1.0

        else:

            effect_score = 0.75

        confidence = (
            sample_score * 0.70
            + effect_score * 0.30
        )

        return round(
            min(
                max(
                    confidence,
                    0.0,
                ),
                1.0,
            ),
            2,
        )

    def _recommendation(
        self,
        *,
        recommendation_type: str,
        reason: str,
        confidence: float,
        current_pump_duration_seconds: int,
        recommended_pump_duration_seconds: int,
        current_cooldown_seconds: int,
        recommended_cooldown_seconds: int,
        watering_count_analyzed: int,
        average_moisture_delta: float,
        average_watering_duration_seconds: float,
    ) -> AdaptiveIrrigationRecommendation:
        """
        Build an observation-mode recommendation.
        """

        return AdaptiveIrrigationRecommendation(
            recommendation_type=(
                recommendation_type
            ),
            should_apply=False,
            reason=reason,
            confidence=confidence,
            confidence_level=(
                self._confidence_level(
                    confidence
                )
            ),
            current_pump_duration_seconds=(
                current_pump_duration_seconds
            ),
            recommended_pump_duration_seconds=(
                recommended_pump_duration_seconds
            ),
            current_cooldown_seconds=(
                current_cooldown_seconds
            ),
            recommended_cooldown_seconds=(
                recommended_cooldown_seconds
            ),
            watering_count_analyzed=(
                watering_count_analyzed
            ),
            average_moisture_delta=round(
                average_moisture_delta,
                2,
            ),
            average_watering_duration_seconds=round(
                average_watering_duration_seconds,
                2,
            ),
        )

    def _average(
        self,
        values: Iterable[int | float],
    ) -> float:
        """
        Return the arithmetic mean.
        """

        values_list = list(
            values
        )

        if not values_list:
            return 0.0

        return (
            sum(values_list)
            / len(values_list)
        )

    def _clamp(
        self,
        value: int,
        minimum: int,
        maximum: int,
    ) -> int:
        """
        Clamp a value to a safe range.
        """

        return max(
            minimum,
            min(
                value,
                maximum,
            ),
        )

    def _confidence_level(
        self,
        confidence: float,
    ) -> str:
        """
        Convert confidence score to a readable level.
        """

        if confidence >= self.HIGH_CONFIDENCE:
            return "HIGH"

        if confidence >= self.MEDIUM_CONFIDENCE:
            return "MEDIUM"

        return "LOW"