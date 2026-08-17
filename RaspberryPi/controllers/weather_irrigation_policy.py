"""
Safe weather influence for automatic irrigation.

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
    """Apply deliberately conservative forecast adjustments."""

    RAIN_DELAY_PROBABILITY = 80.0
    RAIN_DELAY_MM = 2.0
    MAX_DEFICIT_FOR_RAIN_DELAY = 8
    HEAT_TEMPERATURE_C = 35.0
    HEAT_DURATION_MULTIPLIER = 1.15
    HIGH_WIND_KMH = 35.0
    MAX_DEFICIT_FOR_WIND_DELAY = 5

    def evaluate(
        self,
        *,
        forecast: dict | None,
        moisture_deficit: int,
    ) -> WeatherIrrigationAdjustment:
        if not isinstance(forecast, dict) or moisture_deficit <= 0:
            return WeatherIrrigationAdjustment()

        rain_probability = self._number(
            forecast.get("today_rain_probability"),
        )
        rain_mm = self._number(forecast.get("today_rain_mm"))
        wind = self._number(forecast.get("today_wind_max"))
        current_temperature = self._number(
            forecast.get("current_temperature"),
        )
        today_temperature = self._number(
            forecast.get("today_temperature_max"),
        )
        temperature = max(
            value
            for value in (current_temperature, today_temperature)
            if value is not None
        ) if any(
            value is not None
            for value in (current_temperature, today_temperature)
        ) else None

        # Rain is allowed to postpone only a small moisture deficit. A dry
        # zone must never be left thirsty merely because rain is predicted.
        if (
            rain_probability is not None
            and rain_probability >= self.RAIN_DELAY_PROBABILITY
            and rain_mm is not None
            and rain_mm >= self.RAIN_DELAY_MM
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

        # Strong wind may waste a very small corrective watering. It never
        # postpones a meaningful moisture deficit.
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
                detail=f"Today temperature is {round(temperature)}°C.",
            )

        return WeatherIrrigationAdjustment()

    @staticmethod
    def _number(value) -> float | None:
        try:
            return float(value)
        except (TypeError, ValueError):
            return None
