"""
SmartGarden v2

Application configuration.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class AppConfig:
    """General application configuration."""

    APP_NAME: str = "SmartGarden v2"
    VERSION: str = "2.0.0"
    DEVICE_ID: str = "raspberrypi-01"


@dataclass(frozen=True)
class SensorConfig:
    """Soil moisture sensor configuration."""

    # ADS1115
    I2C_ADDRESS: int = 0x48
    ADS_CHANNEL: int = 0
    GAIN: int = 1

    # Calibration values
    SOIL_DRY_VALUE: int = 30700
    SOIL_WET_VALUE: int = 13300

    # Watering threshold (%)
    MOISTURE_LIMIT: int = 40

    # Read interval (seconds)
    READ_INTERVAL_SECONDS: int = 2

    # Sampling
    SAMPLE_COUNT: int = 10
    SAMPLE_DELAY_MS: int = 20


@dataclass(frozen=True)
class RelayConfig:
    """Relay configuration."""

    GPIO_PIN: int = 17
    ACTIVE_LOW: bool = True


@dataclass(frozen=True)
class FirebaseConfig:
    """Firebase configuration."""

<<<<<<< HEAD:core/config.py
    DATABASE_URL: str = (
        "https://smartgarden-v2-default-rtdb.europe-west1.firebasedatabase.app/"
    )
    CREDENTIALS_FILE: str = "firebase_key.json"

    STATUS_UPDATE_INTERVAL_SECONDS: int = 30

    COMMAND_SYNC_INTERVAL_SECONDS = 0.5

=======
    DATABASE_URL: str = "https://smartgarden-v2-default-rtdb.europe-west1.firebasedatabase.app/"
    CREDENTIALS_FILE: str = "firebase_key.json"

    STATUS_UPDATE_INTERVAL_SECONDS: int = 30
>>>>>>> f830855 (v2.1.2 tamam):RaspberryPi/core/config.py

@dataclass(frozen=True)
class LogConfig:
    """Logging configuration."""

    LOG_FOLDER: str = "logs"
    LOG_FILE: str = f"{LOG_FOLDER}/smartgarden.log"
    LOG_LEVEL: str = "INFO"
