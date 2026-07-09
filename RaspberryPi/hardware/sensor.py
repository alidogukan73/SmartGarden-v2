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
            self._i2c = busio.I2C(
                board.SCL,
                board.SDA,
            )

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

            self._logger.info(
                "ADS1115 initialized successfully.",
            )

        except Exception as exc:
            self._logger.exception(exc)
            raise

    def _ensure_initialized(self) -> None:
        """
        Ensure sensor is initialized.
        """

        if not self._initialized:
            raise RuntimeError(
                "Sensor is not initialized.",
            )

    def read_raw(self) -> int:
        """
        Return averaged raw ADC value.
        """

        self._ensure_initialized()

        total = 0

        for _ in range(SensorConfig.SAMPLE_COUNT):

            total += self._channel.value

            time.sleep(
                SensorConfig.SAMPLE_DELAY_MS / 1000,
            )

        return round(
            total / SensorConfig.SAMPLE_COUNT,
        )

    def read_voltage(self) -> float:
        """
        Return sensor voltage.
        """

        self._ensure_initialized()

        return self._channel.voltage

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
            raise ValueError(
                "Invalid sensor calibration.",
            )

        percentage = (
            (dry - raw)
            / (dry - wet)
        ) * 100

        percentage = max(
            0.0,
            min(
                100.0,
                percentage,
            ),
        )

        return round(percentage)

    def read_percentage(self) -> int:
        """
        Return soil moisture percentage.
        """

        raw = self.read_raw()

        return self._calculate_percentage(
            raw,
        )

    def read(self) -> SensorReading:
        """
        Read complete sensor data.
        """

        raw = self.read_raw()

        voltage = self.read_voltage()

        moisture = self._calculate_percentage(
            raw,
        )

        return SensorReading(
            raw=raw,
            voltage=voltage,
            moisture=moisture,
        )