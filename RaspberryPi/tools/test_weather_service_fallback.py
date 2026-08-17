"""Checks that Open-Meteo is used after an OpenWeather failure."""

from services.weather_service import WeatherService


def main() -> None:
    service = WeatherService(openweather_api_key="test-key")

    def fake_get_json(url: str, params: dict) -> dict:
        if url == service.OPENWEATHER_CURRENT_URL:
            assert params["appid"] == "test-key"
            raise OSError("OpenWeather temporarily unavailable")
        if url == service.FORECAST_URL:
            return {
                "current": {
                    "temperature_2m": 29.0,
                    "relative_humidity_2m": 42,
                    "wind_speed_10m": 18.0,
                    "surface_pressure": 1009,
                    "weather_code": 0,
                },
                "daily": {
                    "time": ["2026-08-06", "2026-08-07"],
                    "temperature_2m_max": [33.0, 31.0],
                    "temperature_2m_min": [22.0, 21.0],
                    "precipitation_probability_max": [70, 0],
                    "precipitation_sum": [2.4, 0],
                    "wind_speed_10m_max": [18.0, 7.2],
                    "weather_code": [61, 0],
                },
            }
        raise AssertionError(f"Unexpected URL: {url}")

    service._get_json = fake_get_json
    forecast = service.forecast_for("Düzce", "Merkez", 40.84, 31.16)
    assert forecast["source"] == "open_meteo"
    assert forecast["current_temperature"] == 29.0
    assert forecast["today_rain_probability"] == 70
    assert forecast["today_rain_mm"] == 2.4
    assert forecast["today_wind_max"] == 18.0
    assert len(forecast["days"]) == 2
    try:
        WeatherService(openweather_api_key="").forecast_for(
            "Düzce", "Merkez", 40.84, 31.16, "openweather"
        )
        raise AssertionError("OpenWeather without an API key must not be used.")
    except RuntimeError:
        pass
    print("[PASS] Open-Meteo fallback normalized successfully.")


if __name__ == "__main__":
    main()
