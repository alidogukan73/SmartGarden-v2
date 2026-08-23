"""Weather forecast clients used by AVORA recommendations and irrigation timing."""

from __future__ import annotations

import json
import os
import time
from datetime import datetime, timedelta, timezone
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
        latitude, longitude = self._resolve_coordinates(
            city, district, latitude, longitude
        )
        preference = str(source_preference or "auto").strip().lower()
        if preference == "open_meteo":
            return self._forecast_open_meteo(city, district, latitude, longitude)
        if preference == "openweather":
            if not self._openweather_api_key:
                raise RuntimeError(
                    "OpenWeather is selected but its API key is not configured."
                )
            return self._forecast_openweather(city, district, latitude, longitude)
        if not self._openweather_api_key:
            return self._forecast_open_meteo(city, district, latitude, longitude)
        try:
            return self._forecast_openweather(city, district, latitude, longitude)
        except Exception:
            try:
                return self._forecast_open_meteo(
                    city, district, latitude, longitude
                )
            except Exception as fallback_error:
                raise RuntimeError(
                    "Both OpenWeather and Open-Meteo forecast requests failed."
                ) from fallback_error

    def _resolve_coordinates(
        self,
        city: str,
        district: str,
        latitude: float | None,
        longitude: float | None,
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

    def _forecast_open_meteo(
        self, city: str, district: str, latitude: float, longitude: float
    ) -> dict:
        forecast = self._get_json(
            self.FORECAST_URL,
            {
                "latitude": latitude,
                "longitude": longitude,
                "current": (
                    "temperature_2m,relative_humidity_2m,wind_speed_10m,"
                    "surface_pressure,weather_code,is_day"
                ),
                "hourly": (
                    "temperature_2m,relative_humidity_2m,"
                    "precipitation_probability,precipitation,wind_speed_10m,"
                    "weather_code,is_day,shortwave_radiation,"
                    "et0_fao_evapotranspiration"
                ),
                "daily": (
                    "temperature_2m_max,temperature_2m_min,"
                    "precipitation_probability_max,precipitation_sum,"
                    "wind_speed_10m_max,weather_code,sunrise,sunset"
                ),
                "timezone": "auto",
                "forecast_days": 7,
            },
        )
        daily = forecast.get("daily", {})
        current = forecast.get("current", {})
        utc_offset_seconds = int(forecast.get("utc_offset_seconds", 0) or 0)
        return self._normalized_forecast(
            city,
            district,
            latitude,
            longitude,
            daily,
            current,
            "open_meteo",
            hourly=self._open_meteo_hourly(
                forecast.get("hourly", {}), utc_offset_seconds
            ),
            timezone_name=str(forecast.get("timezone", "")),
            utc_offset_seconds=utc_offset_seconds,
        )

    def _forecast_openweather(
        self, city: str, district: str, latitude: float, longitude: float
    ) -> dict:
        params = {
            "lat": latitude,
            "lon": longitude,
            "appid": self._openweather_api_key,
            "units": "metric",
            "lang": "tr",
        }
        current_raw = self._get_json(self.OPENWEATHER_CURRENT_URL, params)
        forecast_raw = self._get_json(self.OPENWEATHER_FORECAST_URL, params)
        utc_offset_seconds = int(
            forecast_raw.get("city", {}).get(
                "timezone", current_raw.get("timezone", 0)
            )
            or 0
        )
        grouped: dict[str, list[dict]] = {}
        for item in forecast_raw.get("list", []):
            epoch = int(item.get("dt", 0) or 0)
            date = self._epoch_iso(epoch, utc_offset_seconds)
            date = str(date or item.get("dt_txt", ""))[:10]
            if date:
                grouped.setdefault(date, []).append(item)
        days = [
            self._openweather_day(date, entries)
            for date, entries in sorted(grouped.items())
        ]
        daily = {
            "time": [item["date"] for item in days],
            "temperature_2m_max": [item["temperature_max"] for item in days],
            "temperature_2m_min": [item["temperature_min"] for item in days],
            "precipitation_probability_max": [
                item["rain_probability"] for item in days
            ],
            "precipitation_sum": [item["rain_mm"] for item in days],
            "wind_speed_10m_max": [item["wind_max"] for item in days],
            "weather_code": [item["weather_code"] for item in days],
            "sunrise": [
                self._epoch_iso(
                    current_raw.get("sys", {}).get("sunrise"),
                    utc_offset_seconds,
                )
                if index == 0
                else None
                for index in range(len(days))
            ],
            "sunset": [
                self._epoch_iso(
                    current_raw.get("sys", {}).get("sunset"),
                    utc_offset_seconds,
                )
                if index == 0
                else None
                for index in range(len(days))
            ],
        }
        weather = current_raw.get("weather", [{}])
        current = {
            "temperature_2m": self._number(
                current_raw.get("main", {}).get("temp")
            ),
            "relative_humidity_2m": self._number(
                current_raw.get("main", {}).get("humidity")
            ),
            "wind_speed_10m": self._ms_to_kmh(
                current_raw.get("wind", {}).get("speed")
            ),
            "surface_pressure": self._number(
                current_raw.get("main", {}).get("pressure")
            ),
            "weather_code": self._openweather_code(
                weather[0].get("id") if weather else None
            ),
            "is_day": self._openweather_is_day(current_raw),
        }
        return self._normalized_forecast(
            city,
            district,
            latitude,
            longitude,
            daily,
            current,
            "openweather",
            hourly=self._openweather_hourly(
                forecast_raw.get("list", []), utc_offset_seconds
            ),
            utc_offset_seconds=utc_offset_seconds,
        )

    def _normalized_forecast(
        self,
        city: str,
        district: str,
        latitude: float,
        longitude: float,
        daily: dict,
        current: dict,
        source: str,
        *,
        hourly: list[dict] | None = None,
        timezone_name: str = "",
        utc_offset_seconds: int = 0,
    ) -> dict:
        return {
            "city": city,
            "district": district,
            "latitude": latitude,
            "longitude": longitude,
            "source": source,
            "updated_at_epoch": int(time.time()),
            "timezone": timezone_name,
            "utc_offset_seconds": int(utc_offset_seconds),
            "tomorrow_temperature_max": self._daily_value(
                daily, "temperature_2m_max", 1
            ),
            "tomorrow_rain_probability": self._daily_value(
                daily, "precipitation_probability_max", 1
            ),
            "tomorrow_rain_mm": self._daily_value(daily, "precipitation_sum", 1),
            "tomorrow_wind_max": self._daily_value(
                daily, "wind_speed_10m_max", 1
            ),
            "today_temperature_max": self._daily_value(
                daily, "temperature_2m_max", 0
            ),
            "today_rain_probability": self._daily_value(
                daily, "precipitation_probability_max", 0
            ),
            "today_rain_mm": self._daily_value(daily, "precipitation_sum", 0),
            "today_wind_max": self._daily_value(
                daily, "wind_speed_10m_max", 0
            ),
            "today_weather_code": self._daily_value(daily, "weather_code", 0),
            "tomorrow_weather_code": self._daily_value(
                daily, "weather_code", 1
            ),
            "current_temperature": current.get("temperature_2m"),
            "current_humidity": current.get("relative_humidity_2m"),
            "current_wind": current.get("wind_speed_10m"),
            "current_pressure": current.get("surface_pressure"),
            "current_weather_code": current.get("weather_code"),
            "current_is_day": current.get("is_day"),
            "hourly": list(hourly or []),
            "days": [
                {
                    "date": self._daily_value(daily, "time", index),
                    "temperature_max": self._daily_value(
                        daily, "temperature_2m_max", index
                    ),
                    "temperature_min": self._daily_value(
                        daily, "temperature_2m_min", index
                    ),
                    "rain_probability": self._daily_value(
                        daily, "precipitation_probability_max", index
                    ),
                    "rain_mm": self._daily_value(
                        daily, "precipitation_sum", index
                    ),
                    "wind_max": self._daily_value(
                        daily, "wind_speed_10m_max", index
                    ),
                    "weather_code": self._daily_value(
                        daily, "weather_code", index
                    ),
                    "sunrise": self._daily_value(daily, "sunrise", index),
                    "sunset": self._daily_value(daily, "sunset", index),
                }
                for index in range(len(daily.get("time", [])))
            ],
        }

    def _open_meteo_hourly(
        self, hourly: dict, utc_offset_seconds: int
    ) -> list[dict]:
        times = hourly.get("time", []) if isinstance(hourly, dict) else []
        result = []
        for index, value in enumerate(times):
            epoch = self._local_iso_epoch(value, utc_offset_seconds)
            if epoch <= 0:
                continue
            result.append(
                {
                    "time": value,
                    "epoch": epoch,
                    "local_hour": self._local_hour(epoch, utc_offset_seconds),
                    "temperature": self._hourly_value(
                        hourly, "temperature_2m", index
                    ),
                    "humidity": self._hourly_value(
                        hourly, "relative_humidity_2m", index
                    ),
                    "rain_probability": self._hourly_value(
                        hourly, "precipitation_probability", index
                    ),
                    "rain_mm": self._hourly_value(
                        hourly, "precipitation", index
                    ),
                    "wind_kmh": self._hourly_value(
                        hourly, "wind_speed_10m", index
                    ),
                    "weather_code": self._hourly_value(
                        hourly, "weather_code", index
                    ),
                    "is_day": self._hourly_value(hourly, "is_day", index),
                    "shortwave_radiation": self._hourly_value(
                        hourly, "shortwave_radiation", index
                    ),
                    "et0": self._hourly_value(
                        hourly, "et0_fao_evapotranspiration", index
                    ),
                }
            )
        return result

    def _openweather_hourly(
        self, entries: list[dict], utc_offset_seconds: int
    ) -> list[dict]:
        result = []
        for item in entries:
            epoch = int(item.get("dt", 0) or 0)
            if epoch <= 0:
                continue
            weather = item.get("weather") or [{}]
            local_hour = self._local_hour(epoch, utc_offset_seconds)
            result.append(
                {
                    "time": self._epoch_iso(epoch, utc_offset_seconds),
                    "epoch": epoch,
                    "local_hour": local_hour,
                    "temperature": self._number(item.get("main", {}).get("temp")),
                    "humidity": self._number(item.get("main", {}).get("humidity")),
                    "rain_probability": round(
                        (self._number(item.get("pop")) or 0) * 100
                    ),
                    "rain_mm": self._number(item.get("rain", {}).get("3h")) or 0,
                    "wind_kmh": self._ms_to_kmh(
                        item.get("wind", {}).get("speed")
                    ),
                    "weather_code": self._openweather_code(weather[0].get("id")),
                    "is_day": 1 if 6 <= local_hour < 19 else 0,
                    "shortwave_radiation": None,
                    "et0": None,
                }
            )
        return result

    def _openweather_day(self, date: str, entries: list[dict]) -> dict:
        temperatures = [
            self._number(item.get("main", {}).get("temp")) for item in entries
        ]
        temperatures = [value for value in temperatures if value is not None]
        rain_probabilities = [
            self._number(item.get("pop")) or 0 for item in entries
        ]
        rain_values = [
            self._number(item.get("rain", {}).get("3h")) or 0 for item in entries
        ]
        winds = [
            self._ms_to_kmh(item.get("wind", {}).get("speed")) or 0
            for item in entries
        ]
        codes = [
            self._openweather_code((item.get("weather") or [{}])[0].get("id"))
            for item in entries
        ]
        return {
            "date": date,
            "temperature_max": round(max(temperatures), 1)
            if temperatures
            else None,
            "temperature_min": round(min(temperatures), 1)
            if temperatures
            else None,
            "rain_probability": int(round(max(rain_probabilities) * 100)),
            "rain_mm": round(sum(rain_values), 1),
            "wind_max": round(max(winds), 1),
            "weather_code": max(codes, default=3),
        }

    @staticmethod
    def _openweather_is_day(current_raw: dict) -> int:
        now = int(current_raw.get("dt", 0) or 0)
        sunrise = int(current_raw.get("sys", {}).get("sunrise", 0) or 0)
        sunset = int(current_raw.get("sys", {}).get("sunset", 0) or 0)
        return 1 if sunrise and sunset and sunrise <= now < sunset else 0

    @staticmethod
    def _local_iso_epoch(value, utc_offset_seconds: int) -> int:
        try:
            local = datetime.fromisoformat(str(value))
            utc_value = local.replace(tzinfo=timezone.utc) - timedelta(
                seconds=int(utc_offset_seconds)
            )
            return int(utc_value.timestamp())
        except (TypeError, ValueError):
            return 0

    @staticmethod
    def _local_hour(epoch: int, utc_offset_seconds: int) -> int:
        return datetime.fromtimestamp(
            int(epoch) + int(utc_offset_seconds), tz=timezone.utc
        ).hour

    @staticmethod
    def _epoch_iso(value, utc_offset_seconds: int) -> str | None:
        try:
            epoch = int(value)
        except (TypeError, ValueError):
            return None
        return datetime.fromtimestamp(
            epoch + int(utc_offset_seconds), tz=timezone.utc
        ).replace(tzinfo=None).isoformat(timespec="minutes")

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
    def _hourly_value(hourly: dict, key: str, index: int):
        values = hourly.get(key, [])
        return values[index] if len(values) > index else None

    @staticmethod
    def _get_json(url: str, params: dict) -> dict:
        with urlopen(url + "?" + urlencode(params), timeout=12) as response:
            return json.loads(response.read().decode("utf-8"))
