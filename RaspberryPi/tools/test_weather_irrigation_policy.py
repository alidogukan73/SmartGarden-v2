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


    policy.configure({
        "rain_delay_enabled": True,
        "rain_probability_threshold": 60,
        "rain_mm_threshold": 1,
    })
    custom_threshold = policy.evaluate(
        forecast={"today_rain_probability": 65, "today_rain_mm": 1.5},
        moisture_deficit=5,
    )
    assert custom_threshold.postpone

    policy.configure({"rain_delay_enabled": False})
    disabled = policy.evaluate(
        forecast={"today_rain_probability": 100, "today_rain_mm": 20},
        moisture_deficit=5,
    )
    assert not disabled.postpone

    print("[PASS] Weather irrigation policy safety scenarios.")


if __name__ == "__main__":
    main()
