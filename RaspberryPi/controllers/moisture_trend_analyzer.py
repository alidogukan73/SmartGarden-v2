"""
Moisture trend analyzer.
"""

from __future__ import annotations

from models.moisture_history import MoistureHistory
from models.moisture_trend import MoistureTrend


class MoistureTrendAnalyzer:
    """
    Calculates moisture trends from a shared MoistureHistory instance.
    """

    DEFAULT_MIN_SAMPLES = 5
    DEFAULT_STABLE_SPREAD = 2
    MINIMUM_ANALYSIS_DURATION_SECONDS = 300

    def __init__(
        self,
        *,
        history: MoistureHistory,
        min_samples: int = DEFAULT_MIN_SAMPLES,
        stable_spread: int = DEFAULT_STABLE_SPREAD,
        minimum_duration_seconds: int = MINIMUM_ANALYSIS_DURATION_SECONDS,

    ) -> None:

        if min_samples < 2:
            raise ValueError(
                "min_samples must be at least 2.",
            )

        if stable_spread < 0:
            raise ValueError(
                "stable_spread cannot be negative.",
            )

        self._minimum_duration_seconds = minimum_duration_seconds
        self._history = history
        self._min_samples = min_samples
        self._stable_spread = stable_spread

    def analyze(
        self,
    ) -> MoistureTrend:
        """
        Analyze the shared moisture history.
        """

        samples = self._history.samples()
        sample_count = len(samples)

        if sample_count == 0:

            return MoistureTrend(
                classification="INSUFFICIENT_DATA",
                sample_count=0,
                first_moisture=0,
                latest_moisture=0,
                minimum_moisture=0,
                maximum_moisture=0,
                average_moisture=0.0,
                total_change=0,
                change_per_minute=0.0,
                duration_seconds=0.0,
                is_stable=False,
            )

        moisture_values = [
            sample.moisture
            for sample in samples
        ]

        first_sample = samples[0]
        latest_sample = samples[-1]

        first_moisture = first_sample.moisture
        latest_moisture = latest_sample.moisture

        minimum_moisture = min(
            moisture_values,
        )

        maximum_moisture = max(
            moisture_values,
        )

        average_moisture = (
            sum(moisture_values)
            / sample_count
        )

        total_change = (
            latest_moisture
            - first_moisture
        )

        duration_seconds = max(
            0.0,
            latest_sample.timestamp
            - first_sample.timestamp,
        )

        change_per_minute = (
            self._calculate_change_per_minute(
                total_change=total_change,
                duration_seconds=duration_seconds,
            )
        )

        spread = (
            maximum_moisture
            - minimum_moisture
        )

        has_enough_samples = (
            sample_count
            >= self._min_samples
        )

        is_stable = (
            has_enough_samples
            and spread <= self._stable_spread
            and total_change == 0
        )

        classification = self._classify(
            sample_count=sample_count,
            duration_seconds=duration_seconds,
            total_change=total_change,
            change_per_minute=change_per_minute,
            is_stable=is_stable,
        )

        return MoistureTrend(
            classification=classification,
            sample_count=sample_count,
            first_moisture=first_moisture,
            latest_moisture=latest_moisture,
            minimum_moisture=minimum_moisture,
            maximum_moisture=maximum_moisture,
            average_moisture=round(
                average_moisture,
                2,
            ),
            total_change=total_change,
            change_per_minute=round(
                change_per_minute,
                3,
            ),
            duration_seconds=round(
                duration_seconds,
                2,
            ),
            is_stable=is_stable,
        )

    def _calculate_change_per_minute(
        self,
        *,
        total_change: int,
        duration_seconds: float,
    ) -> float:
        """
        Calculate moisture change per minute.
        """

        if duration_seconds <= 0:
            return 0.0

        duration_minutes = (
            duration_seconds
            / 60.0
        )

        return (
            total_change
            / duration_minutes
        )

    def _classify(
        self,
        *,
        sample_count: int,
        duration_seconds: float,
        total_change: int,
        change_per_minute: float,
        is_stable: bool,
    ) -> str:
        """
        Classify the current moisture trend.
        """

        if sample_count < self._min_samples:
            return "INSUFFICIENT_DATA"
        
        if duration_seconds < self._minimum_duration_seconds:
            return "INSUFFICIENT_DATA"

        if total_change > 0:
            return "RISING"

        if is_stable:
            return "STABLE"

        drying_rate = abs(
            change_per_minute,
        )

        if drying_rate < 0.25:
            return "SLOW_DRYING"

        if drying_rate <= 0.75:
            return "NORMAL_DRYING"

        if drying_rate < 1.50:
            return "FAST_DRYING"

        return "VERY_FAST_DRYING"