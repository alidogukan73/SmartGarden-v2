"""Focused safety checks for weather-aware automatic irrigation."""

from controllers.weather_irrigation_policy import WeatherIrrigationPolicy


NOW_EPOCH = 1_700_000_000


def fresh_forecast(**values) -> dict:
    return {
        "updated_at_epoch": NOW_EPOCH,
        **values,
    }


def main() -> None:
    policy = WeatherIrrigationPolicy()

    rain_delay = policy.evaluate(
        forecast=fresh_forecast(
            today_rain_probability=85,
            today_rain_mm=4,
        ),
        moisture_deficit=6,
        now_epoch=NOW_EPOCH,
    )
    assert rain_delay.postpone
    assert rain_delay.reason == "WEATHER_RAIN_DELAY"

    serious_dryness = policy.evaluate(
        forecast=fresh_forecast(
            today_rain_probability=90,
            today_rain_mm=8,
        ),
        moisture_deficit=15,
        now_epoch=NOW_EPOCH,
    )
    assert not serious_dryness.postpone

    heat = policy.evaluate(
        forecast=fresh_forecast(current_temperature=37),
        moisture_deficit=12,
        now_epoch=NOW_EPOCH,
    )
    assert not heat.postpone
    assert heat.duration_multiplier == 1.05
    assert heat.reason == "WEATHER_DRIP_HEAT_DURATION"

    wind_delay = policy.evaluate(
        forecast=fresh_forecast(today_wind_max=40),
        moisture_deficit=4,
        now_epoch=NOW_EPOCH,
    )
    assert not wind_delay.postpone

    sprinkler_heat = policy.evaluate(
        forecast=fresh_forecast(current_temperature=37),
        moisture_deficit=12,
        now_epoch=NOW_EPOCH,
        irrigation_method="SPRINKLER",
    )
    assert sprinkler_heat.duration_multiplier == 1.15
    assert sprinkler_heat.reason == "WEATHER_HEAT_DURATION"

    sprinkler_wind_delay = policy.evaluate(
        forecast=fresh_forecast(today_wind_max=40),
        moisture_deficit=4,
        now_epoch=NOW_EPOCH,
        irrigation_method="SPRINKLER",
    )
    assert sprinkler_wind_delay.postpone
    assert sprinkler_wind_delay.reason == "WEATHER_WIND_DELAY"

    stale = policy.evaluate(
        forecast=fresh_forecast(
            today_rain_probability=100,
            today_rain_mm=20,
        ),
        moisture_deficit=5,
        now_epoch=(
            NOW_EPOCH
            + WeatherIrrigationPolicy.MAX_FORECAST_AGE_SECONDS
            + 1
        ),
    )
    assert not stale.postpone
    assert stale.duration_multiplier == 1.0
    assert stale.reason == "WEATHER_FORECAST_STALE"

    missing_timestamp = policy.evaluate(
        forecast={"today_rain_probability": 100, "today_rain_mm": 20},
        moisture_deficit=5,
        now_epoch=NOW_EPOCH,
    )
    assert not missing_timestamp.postpone
    assert missing_timestamp.reason == "WEATHER_FORECAST_STALE"

    policy.configure({
        "rain_delay_enabled": True,
        "rain_probability_threshold": 60,
        "rain_mm_threshold": 1,
    })
    custom_threshold = policy.evaluate(
        forecast=fresh_forecast(
            today_rain_probability=65,
            today_rain_mm=1.5,
        ),
        moisture_deficit=5,
        now_epoch=NOW_EPOCH,
    )
    assert custom_threshold.postpone

    policy.configure({"rain_delay_enabled": False})
    disabled = policy.evaluate(
        forecast=fresh_forecast(
            today_rain_probability=100,
            today_rain_mm=20,
        ),
        moisture_deficit=5,
        now_epoch=NOW_EPOCH,
    )
    assert not disabled.postpone

    print("[PASS] Weather irrigation policy safety scenarios.")


if __name__ == "__main__":
    main()
