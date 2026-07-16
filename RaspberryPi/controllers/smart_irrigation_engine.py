"""
Smart irrigation decision engine.
"""

from __future__ import annotations

from controllers.moisture_trend_analyzer import (
    MoistureTrendAnalyzer,
)
from core.logger import AppLogger
from models.command_state import CommandState
from models.irrigation_decision import IrrigationDecision
from models.moisture_history import MoistureHistory
from models.moisture_trend import MoistureTrend
from models.sensor_reading import SensorReading


class SmartIrrigationEngine:
    """
    Evaluates moisture readings and decides whether watering should start.
    """

    DEFAULT_SAMPLE_WINDOW = 5
    DEFAULT_MAX_SENSOR_SPREAD = 4
    DEFAULT_HISTORY_SIZE = 20

    def __init__(
        self,
        *,
        sample_window: int = DEFAULT_SAMPLE_WINDOW,
        max_sensor_spread: int = DEFAULT_MAX_SENSOR_SPREAD,
        history_size: int = DEFAULT_HISTORY_SIZE,
    ) -> None:

        if sample_window < 3:
            raise ValueError(
                "sample_window must be at least 3.",
            )

        if max_sensor_spread < 0:
            raise ValueError(
                "max_sensor_spread cannot be negative.",
            )

        if history_size < sample_window:
            raise ValueError(
                "history_size cannot be smaller than sample_window.",
            )

        self._logger = AppLogger().logger

        self._sample_window = sample_window
        self._max_sensor_spread = max_sensor_spread

        # Nem ölçümlerinin tek ortak kaynağı.
        self._history = MoistureHistory(
            max_samples=history_size,
        )

        # Trend motoru aynı geçmişi yalnızca analiz eder.
        self._trend_analyzer = MoistureTrendAnalyzer(
            history=self._history,
        )

    def evaluate(
        self,
        *,
        reading: SensorReading,
        commands: CommandState,
        cooldown_active: bool,
    ) -> IrrigationDecision:
        """
        Evaluate the latest sensor reading.
        """

        moisture = int(
            reading.moisture,
        )

        moisture_limit = int(
            commands.moisture_limit,
        )

        # Ölçüm yalnızca bir kez ortak geçmişe eklenir.
        self._history.add(
            moisture,
        )

        sensor_stable = (
            self._is_sensor_stable()
        )

        trend = (
            self._trend_analyzer.analyze()
        )

        if not commands.enabled:

            return self._decision(
                should_water=False,
                reason="SYSTEM_DISABLED",
                moisture=moisture,
                moisture_limit=moisture_limit,
                sensor_stable=sensor_stable,
                cooldown_active=cooldown_active,
                trend=trend,
            )

        if not commands.auto_mode:

            return self._decision(
                should_water=False,
                reason="AUTO_MODE_DISABLED",
                moisture=moisture,
                moisture_limit=moisture_limit,
                sensor_stable=sensor_stable,
                cooldown_active=cooldown_active,
                trend=trend,
            )

        if not self._has_enough_samples():

            return self._decision(
                should_water=False,
                reason="INSUFFICIENT_SENSOR_SAMPLES",
                moisture=moisture,
                moisture_limit=moisture_limit,
                sensor_stable=False,
                cooldown_active=cooldown_active,
                trend=trend,
            )

        if not sensor_stable:

            return self._decision(
                should_water=False,
                reason="SENSOR_UNSTABLE",
                moisture=moisture,
                moisture_limit=moisture_limit,
                sensor_stable=False,
                cooldown_active=cooldown_active,
                trend=trend,
            )

        if cooldown_active:

            return self._decision(
                should_water=False,
                reason="COOLDOWN_ACTIVE",
                moisture=moisture,
                moisture_limit=moisture_limit,
                sensor_stable=True,
                cooldown_active=True,
                trend=trend,
            )

        if moisture >= moisture_limit:

            return self._decision(
                should_water=False,
                reason="MOISTURE_SUFFICIENT",
                moisture=moisture,
                moisture_limit=moisture_limit,
                sensor_stable=True,
                cooldown_active=False,
                trend=trend,
            )

        return self._decision(
            should_water=True,
            reason="MOISTURE_BELOW_LIMIT",
            moisture=moisture,
            moisture_limit=moisture_limit,
            sensor_stable=True,
            cooldown_active=False,
            trend=trend,
        )

    def get_current_trend(
        self,
    ) -> MoistureTrend:
        """
        Return the latest moisture trend from shared history.
        """

        return self._trend_analyzer.analyze()

    def reset(
        self,
    ) -> None:
        """
        Clear all stored moisture samples.
        """

        self._history.clear()

    def _has_enough_samples(
        self,
    ) -> bool:
        """
        Return whether enough readings exist for a safe decision.
        """

        return self._history.has_at_least(
            self._sample_window,
        )

    def _is_sensor_stable(
        self,
    ) -> bool:
        """
        Check whether the most recent readings are consistent.
        """

        if not self._has_enough_samples():
            return False

        recent_values = (
            self._history.last_values(
                self._sample_window,
            )
        )

        minimum = min(
            recent_values,
        )

        maximum = max(
            recent_values,
        )

        spread = (
            maximum
            - minimum
        )

        return (
            spread
            <= self._max_sensor_spread
        )

    def _decision(
        self,
        *,
        should_water: bool,
        reason: str,
        moisture: int,
        moisture_limit: int,
        sensor_stable: bool,
        cooldown_active: bool,
        trend: MoistureTrend,
    ) -> IrrigationDecision:
        """
        Build and log an irrigation decision.
        """

        effective_change_per_minute = (
            0.0
            if trend.classification == "INSUFFICIENT_DATA"
            else trend.change_per_minute
        )

        decision = IrrigationDecision(
            should_water=should_water,
            reason=reason,
            moisture=moisture,
            moisture_limit=moisture_limit,
            sensor_stable=sensor_stable,
            cooldown_active=cooldown_active,
            trend_classification=(
                trend.classification
            ),
            trend_sample_count=(
                trend.sample_count
            ),
            moisture_change_per_minute=(
                effective_change_per_minute
            ),
            trend_duration_seconds=(
                trend.duration_seconds
            ),

            average_moisture=(
                trend.average_moisture
            ),
        )

        self._logger.debug(
            "Decision=%s "
            "Reason=%s "
            "Trend=%s "
            "TrendSamples=%d "
            "Speed=%.3f/min "
            "Moisture=%d%% "
            "Limit=%d%% "
            "SensorStable=%s "
            "Cooldown=%s",
            decision.should_water,
            decision.reason,
            decision.trend_classification,
            decision.trend_sample_count,
            decision.moisture_change_per_minute,
            decision.moisture,
            decision.moisture_limit,
            decision.sensor_stable,
            decision.cooldown_active,
        )

        return decision