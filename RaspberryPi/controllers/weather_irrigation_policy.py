"""Safe weather influence for automatic irrigation.

Weather can refine an already-valid soil-moisture decision, but it never
replaces sensor stability, cooldown, valve or shared-pump safety checks.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class WeatherIrrigationAdjustment:
    postpone: bool = False
    duration_multiplier: float = 1.0
    reason: str = "WEATHER_NEUTRAL"
    detail: str = ""


class WeatherIrrigationPolicy:
    """Apply deliberately conservative and safety-bounded adjustments."""

    RAIN_DELAY_PROBABILITY = 80.0
    RAIN_DELAY_MM = 2.0
    MAX_DEFICIT_FOR_RAIN_DELAY = 8
    HEAT_TEMPERATURE_C = 35.0
    HEAT_DURATION_MULTIPLIER = 1.15
    HIGH_WIND_KMH = 35.0
    MAX_DEFICIT_FOR_WIND_DELAY = 5

    def __init__(self) -> None:
        self.rain_delay_enabled = True
        self.rain_delay_probability = self.RAIN_DELAY_PROBABILITY
        self.rain_delay_mm = self.RAIN_DELAY_MM

    def configure(self, settings: dict | None) -> None:
        """Apply user values while keeping hard safety limits fixed."""
        values = settings if isinstance(settings, dict) else {}
        self.rain_delay_enabled = values.get("rain_delay_enabled", True) is not False
        self.rain_delay_probability = self._clamp(
            self._number(values.get("rain_probability_threshold")),
            50.0,
            100.0,
            self.RAIN_DELAY_PROBABILITY,
        )
        self.rain_delay_mm = self._clamp(
            self._number(values.get("rain_mm_threshold")),
            0.5,
            10.0,
            self.RAIN_DELAY_MM,
        )

    def evaluate(
        self,
        *,
        forecast: dict | None,
        moisture_deficit: int,
    ) -> WeatherIrrigationAdjustment:
        if not isinstance(forecast, dict) or moisture_deficit <= 0:
            return WeatherIrrigationAdjustment()

        rain_probability = self._number(forecast.get("today_rain_probability"))
        rain_mm = self._number(forecast.get("today_rain_mm"))
        wind = self._number(forecast.get("today_wind_max"))
        current_temperature = self._number(forecast.get("current_temperature"))
        today_temperature = self._number(forecast.get("today_temperature_max"))
        temperatures = [
            value for value in (current_temperature, today_temperature)
            if value is not None
        ]
        temperature = max(temperatures) if temperatures else None

        # Rain can postpone only a small moisture deficit. This hard limit is
        # intentionally not user-configurable.
        if (
            self.rain_delay_enabled
            and rain_probability is not None
            and rain_probability >= self.rain_delay_probability
            and rain_mm is not None
            and rain_mm >= self.rain_delay_mm
            and moisture_deficit <= self.MAX_DEFICIT_FOR_RAIN_DELAY
        ):
            return WeatherIrrigationAdjustment(
                postpone=True,
                reason="WEATHER_RAIN_DELAY",
                detail=(
                    f"Today rain probability is %{round(rain_probability)} "
                    f"with {rain_mm:.1f} mm expected."
                ),
            )

        if (
            wind is not None
            and wind >= self.HIGH_WIND_KMH
            and moisture_deficit <= self.MAX_DEFICIT_FOR_WIND_DELAY
        ):
            return WeatherIrrigationAdjustment(
                postpone=True,
                reason="WEATHER_WIND_DELAY",
                detail=f"Today wind is {round(wind)} km/h.",
            )

        if temperature is not None and temperature >= self.HEAT_TEMPERATURE_C:
            return WeatherIrrigationAdjustment(
                duration_multiplier=self.HEAT_DURATION_MULTIPLIER,
                reason="WEATHER_HEAT_DURATION",
                detail=f"Today temperature is {round(temperature)} C.",
            )

        return WeatherIrrigationAdjustment()

    @staticmethod
    def _clamp(value, minimum: float, maximum: float, fallback: float) -> float:
        if value is None:
            return fallback
        return max(minimum, min(maximum, value))

    @staticmethod
    def _number(value) -> float | None:
        try:
            return float(value)
        except (TypeError, ValueError):
            return None