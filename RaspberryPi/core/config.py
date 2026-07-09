"""
Application configuration.
"""

from __future__ import annotations


class AppConfig:
    """
    General application configuration.
    """

    DEVICE_ID = "smartgarden-001"

    VERSION = "2.3.0"

    LOOP_DELAY_SECONDS = 2.0


class FirebaseConfig:
    """
    Firebase configuration.
    """

    DATABASE_URL = (
        "https://smartgarden-v2-default-rtdb.europe-west1.firebasedatabase.app/"
    )

    CREDENTIALS_FILE = "firebase_key.json"

    STATUS_UPDATE_INTERVAL_SECONDS = 30

    COMMAND_SYNC_INTERVAL_SECONDS = 0.5


class SensorConfig:
    """
    Soil moisture sensor configuration.
    """

    I2C_ADDRESS = 0x48

    GAIN = 1

    SAMPLE_COUNT = 10

    SAMPLE_DELAY_MS = 20

    SOIL_DRY_VALUE = 32767

    SOIL_WET_VALUE = 12000


class RelayConfig:
    """
    Relay configuration.
    """

    GPIO_PIN = 17

    ACTIVE_LOW = True
    
class LogConfig:
    """
    Logging configuration.
    """

    LEVEL = "INFO"

    FORMAT = (
        "%(asctime)s | %(levelname)-8s | %(message)s"
    )

    DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

    LOG_FILE = "smartgarden.log"