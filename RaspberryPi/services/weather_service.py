"""Weather forecast clients used only for AVORA recommendations."""

from __future__ import annotations

import json
import os
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import urlopen


class WeatherService:
    """OpenWeather first, with Open-Meteo as the automatic fallback."""

    GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
    FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
    OPENWEATHER_CURRENT_URL = "https://api.openweathermap.org/data/2.5/weather"
    OPENWEATHER_FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast"

    def __init__(self, openweather_api_key: str | None = None) -> None:
        self._openweather_api_key = (
            openweather_api_key
            if openweather_api_key is not None
            else self._load_openweather_api_key()
        )

    def forecast_for(
        self,
        city: str,
        district: str,
        latitude: float | None = None,
        longitude: float | None = None,
        source_preference: str = "auto",
    ) -> dict:
        latitude, longitude = self._resolve_coordinates(city, district, latitude, longitude)
        preference = str(source_preference or "auto").strip().lower()
        if preference == "open_meteo":
            return self._forecast_open_meteo(city, district, latitude, longitude)
        if preference == "openweather":
            if not self._openweather_api_key:
                raise RuntimeError("OpenWeather is selected but its API key is not configured.")
            return self._forecast_openweather(city, district, latitude, longitude)
        if not self._openweather_api_key:
            return self._forecast_open_meteo(city, district, latitude, longitude)
        try:
            return self._forecast_openweather(city, district, latitude, longitude)
        except Exception as primary_error:
            try:
                return self._forecast_open_meteo(city, district, latitude, longitude)
            except Exception as fallback_error:
                raise RuntimeError(
                    "Both OpenWeather and Open-Meteo forecast requests failed."
                ) from fallback_error

    def _resolve_coordinates(
        self, city: str, district: str, latitude: float | None, longitude: float | None
    ) -> tuple[float, float]:
        if latitude is not None and longitude is not None:
            return latitude, longitude
        query = ", ".join(value for value in (district, city, "Turkey") if value)
        locations = self._get_json(
            self.GEOCODING_URL,
            {"name": query, "count": 1, "language": "tr", "format": "json"},
        ).get("results", [])
        if not locations:
            raise ValueError("Weather location could not be found.")
        place = locations[0]
        return place["latitude"], place["longitude"]

    def _forecast_open_meteo(self, city: str, district: str, latitude: float, longitude: float) -> dict:
        forecast = self._get_json(
            self.FORECAST_URL,
            {
                "latitude": latitude,
                "longitude": longitude,
                "current": "temperature_2m,relative_humidity_2m,wind_speed_10m,surface_pressure,weather_code",
                "daily": "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,weather_code",
                "timezone": "auto",
                "forecast_days": 7,
            },
        )
        daily = forecast.get("daily", {})
        current = forecast.get("current", {})
        return self._normalized_forecast(
            city, district, latitude, longitude, daily, current, "open_meteo"
        )

    def _forecast_openweather(self, city: str, district: str, latitude: float, longitude: float) -> dict:
        params = {
            "lat": latitude,
            "lon": longitude,
            "appid": self._openweather_api_key,
            "units": "metric",
            "lang": "tr",
        }
        current_raw = self._get_json(self.OPENWEATHER_CURRENT_URL, params)
        forecast_raw = self._get_json(self.OPENWEATHER_FORECAST_URL, params)
        grouped: dict[str, list[dict]] = {}
        for item in forecast_raw.get("list", []):
            date = str(item.get("dt_txt", ""))[:10]
            if date:
                grouped.setdefault(date, []).append(item)
        days = [self._openweather_day(date, entries) for date, entries in sorted(grouped.items())]
        daily = {
            "time": [item["date"] for item in days],
            "temperature_2m_max": [item["temperature_max"] for item in days],
            "temperature_2m_min": [item["temperature_min"] for item in days],
            "precipitation_probability_max": [item["rain_probability"] for item in days],
            "precipitation_sum": [item["rain_mm"] for item in days],
            "wind_speed_10m_max": [item["wind_max"] for item in days],
            "weather_code": [item["weather_code"] for item in days],
        }
        weather = current_raw.get("weather", [{}])
        current = {
            "temperature_2m": self._number(current_raw.get("main", {}).get("temp")),
            "relative_humidity_2m": self._number(current_raw.get("main", {}).get("humidity")),
            "wind_speed_10m": self._ms_to_kmh(current_raw.get("wind", {}).get("speed")),
            "surface_pressure": self._number(current_raw.get("main", {}).get("pressure")),
            "weather_code": self._openweather_code(weather[0].get("id") if weather else None),
        }
        return self._normalized_forecast(
            city, district, latitude, longitude, daily, current, "openweather"
        )

    def _normalized_forecast(
        self, city: str, district: str, latitude: float, longitude: float,
        daily: dict, current: dict, source: str
    ) -> dict:
        return {
            "city": city, "district": district,
            "latitude": latitude, "longitude": longitude, "source": source,
            "tomorrow_temperature_max": self._daily_value(daily, "temperature_2m_max", 1),
            "tomorrow_rain_probability": self._daily_value(daily, "precipitation_probability_max", 1),
            "tomorrow_rain_mm": self._daily_value(daily, "precipitation_sum", 1),
            "tomorrow_wind_max": self._daily_value(daily, "wind_speed_10m_max", 1),
            "today_temperature_max": self._daily_value(daily, "temperature_2m_max", 0),
            "today_rain_probability": self._daily_value(daily, "precipitation_probability_max", 0),
            "today_rain_mm": self._daily_value(daily, "precipitation_sum", 0),
            "today_wind_max": self._daily_value(daily, "wind_speed_10m_max", 0),
            "today_weather_code": self._daily_value(daily, "weather_code", 0),
            "tomorrow_weather_code": self._daily_value(daily, "weather_code", 1),
            "current_temperature": current.get("temperature_2m"),
            "current_humidity": current.get("relative_humidity_2m"),
            "current_wind": current.get("wind_speed_10m"),
            "current_pressure": current.get("surface_pressure"),
            "current_weather_code": current.get("weather_code"),
            "days": [
                {
                    "date": self._daily_value(daily, "time", index),
                    "temperature_max": self._daily_value(daily, "temperature_2m_max", index),
                    "temperature_min": self._daily_value(daily, "temperature_2m_min", index),
                    "rain_probability": self._daily_value(daily, "precipitation_probability_max", index),
                    "rain_mm": self._daily_value(daily, "precipitation_sum", index),
                    "wind_max": self._daily_value(daily, "wind_speed_10m_max", index),
                    "weather_code": self._daily_value(daily, "weather_code", index),
                }
                for index in range(len(daily.get("time", [])))
            ],
        }

    def _openweather_day(self, date: str, entries: list[dict]) -> dict:
        temperatures = [self._number(item.get("main", {}).get("temp")) for item in entries]
        temperatures = [value for value in temperatures if value is not None]
        rain_probabilities = [self._number(item.get("pop")) or 0 for item in entries]
        rain_values = [
            self._number(item.get("rain", {}).get("3h")) or 0 for item in entries
        ]
        winds = [self._ms_to_kmh(item.get("wind", {}).get("speed")) or 0 for item in entries]
        codes = [
            self._openweather_code((item.get("weather") or [{}])[0].get("id"))
            for item in entries
        ]
        return {
            "date": date,
            "temperature_max": round(max(temperatures), 1) if temperatures else None,
            "temperature_min": round(min(temperatures), 1) if temperatures else None,
            "rain_probability": int(round(max(rain_probabilities) * 100)),
            "rain_mm": round(sum(rain_values), 1),
            "wind_max": round(max(winds), 1),
            "weather_code": max(codes, default=3),
        }

    @staticmethod
    def _load_openweather_api_key() -> str:
        env_key = os.getenv("SMARTGARDEN_OPENWEATHER_API_KEY", "").strip()
        if env_key:
            return env_key
        key_file = Path(__file__).resolve().parent.parent / "weather_api_key.txt"
        try:
            return key_file.read_text(encoding="utf-8").strip()
        except OSError:
            return ""

    @staticmethod
    def _openweather_code(value) -> int:
        code = int(value or 0)
        if 200 <= code < 300:
            return 95
        if 300 <= code < 500:
            return 51
        if 500 <= code < 600:
            return 61
        if 600 <= code < 700:
            return 71
        if code == 800:
            return 0
        return 3

    @staticmethod
    def _number(value):
        try:
            return float(value)
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _ms_to_kmh(value):
        number = WeatherService._number(value)
        return round(number * 3.6, 1) if number is not None else None

    @staticmethod
    def _daily_value(daily: dict, key: str, index: int):
        values = daily.get(key, [])
        return values[index] if len(values) > index else None

    @staticmethod
    def _get_json(url: str, params: dict) -> dict:
        with urlopen(url + "?" + urlencode(params), timeout=12) as response:
            return json.loads(response.read().decode("utf-8"))
