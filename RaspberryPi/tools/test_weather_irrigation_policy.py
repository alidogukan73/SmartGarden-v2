"""Focused safety checks for weather-aware automatic irrigation."""

from controllers.weather_irrigation_policy import WeatherIrrigationPolicy


def main() -> None:
    policy = WeatherIrrigationPolicy()

    rain_delay = policy.evaluate(
        forecast={
            "today_rain_probability": 85,
            "today_rain_mm": 4,
        },
        moisture_deficit=6,
    )
    assert rain_delay.postpone
    assert rain_delay.reason == "WEATHER_RAIN_DELAY"

    serious_dryness = policy.evaluate(
        forecast={
            "today_rain_probability": 90,
            "today_rain_mm": 8,
        },
        moisture_deficit=15,
    )
    assert not serious_dryness.postpone

    heat = policy.evaluate(
        forecast={"current_temperature": 37},
        moisture_deficit=12,
    )
    assert not heat.postpone
    assert heat.duration_multiplier == 1.15

    wind_delay = policy.evaluate(
        forecast={"today_wind_max": 40},
        moisture_deficit=4,
    )
    assert wind_delay.postpone
    assert wind_delay.reason == "WEATHER_WIND_DELAY"

    print("[PASS] Weather irrigation policy safety scenarios.")


if __name__ == "__main__":
    main()
