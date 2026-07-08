"""
Soil moisture sensor module.
"""

from __future__ import annotations

import time

import board
import busio
from adafruit_ads1x15 import ads1x15
from adafruit_ads1x15.ads1115 import ADS1115
from adafruit_ads1x15.analog_in import AnalogIn

from core.config import SensorConfig
from core.logger import AppLogger
from models.sensor_reading import SensorReading


class SoilMoistureSensor:
    """
    Reads soil moisture values from ADS1115.
    """

    def __init__(self) -> None:
        self._logger = AppLogger().logger

        self._i2c = None
        self._ads = None
        self._channel = None

        self._initialized = False

    def initialize(self) -> None:
        """
        Initialize ADS1115.
        """

        try:
            self._i2c = busio.I2C(board.SCL, board.SDA)

            self._ads = ADS1115(
                self._i2c,
                address=SensorConfig.I2C_ADDRESS,
            )

            self._ads.gain = SensorConfig.GAIN

            self._channel = AnalogIn(
                self._ads,
                ads1x15.Pin.A0,
            )

            self._initialized = True

            self._logger.info("ADS1115 initialized successfully.")

        except Exception as exc:
            self._logger.exception(exc)
            raise

    def read_raw(self) -> int:
        """
        Return averaged raw ADC value.
        """

        if not self._initialized:
            raise RuntimeError("Sensor is not initialized.")

        total = 0

        for _ in range(SensorConfig.SAMPLE_COUNT):
            total += self._channel.value

            time.sleep(
                SensorConfig.SAMPLE_DELAY_MS / 1000,
            )

        return round(total / SensorConfig.SAMPLE_COUNT)

    def _calculate_percentage(
        self,
        raw: int,
    ) -> int:
        """
        Convert raw ADC value to soil moisture percentage.
        """

        dry = SensorConfig.SOIL_DRY_VALUE
        wet = SensorConfig.SOIL_WET_VALUE

        if dry == wet:
            raise ValueError("Invalid calibration values.")

        percentage = ((dry - raw) / (dry - wet)) * 100

        percentage = max(
            0.0,
            min(100.0, percentage),
        )

        return round(percentage)

    def read_voltage(self) -> float:
        """
        Return sensor voltage.
        """

        if not self._initialized:
            raise RuntimeError("Sensor is not initialized.")

        return self._channel.voltage

    def read_percentage(self) -> int:
        """
        Return soil moisture percentage.
        """

        raw = self.read_raw()

        return self._calculate_percentage(raw)

    def read(self) -> SensorReading:
        """
        Read all sensor values.
        """

        if not self._initialized:
            raise RuntimeError("Sensor is not initialized.")

        raw = self.read_raw()

        return SensorReading(
            raw=raw,
            voltage=self._channel.voltage,
            moisture=self._calculate_percentage(raw),
        )