"""
Application configuration.
"""

from __future__ import annotations


class AppConfig:
    """
    General application configuration.
    """

    DEVICE_ID = "smartgarden-001"

    VERSION = "2.4.0-dev"

    LOOP_DELAY_SECONDS = 2.0


class FirebaseConfig:
    """
    Firebase configuration.
    """

    DATABASE_URL = (
        "https://smartgarden-v2-default-rtdb.europe-west1.firebasedatabase.app/"
    )

    CREDENTIALS_FILE = "firebase_key.json"

    STATUS_UPDATE_INTERVAL_SECONDS = 10

    COMMAND_SYNC_INTERVAL_SECONDS = 0.5


class SensorConfig:
    """
    Soil moisture sensor configuration.
    """

    # -------------------------------------------------
    # Sensor source
    # -------------------------------------------------

    # "wired" → ADS1115 doğrudan Raspberry Pi üzerinde
    # "mqtt"  → ESP32 üzerinden kablosuz MQTT sensörü
    SENSOR_MODE = "mqtt"

    # -------------------------------------------------
    # Wired ADS1115 configuration
    # -------------------------------------------------

    I2C_ADDRESS = 0x48

    GAIN = 1

    SAMPLE_COUNT = 10

    SAMPLE_DELAY_MS = 50

    SOIL_DRY_VALUE = 13850

    SOIL_WET_VALUE = 4442

    RESTART_DELTA = 10

    MIN_WATERING_INTERVAL_SECONDS = 120

    # -------------------------------------------------
    # Wireless MQTT sensor configuration
    # -------------------------------------------------

    MQTT_BROKER = "127.0.0.1"

    MQTT_PORT = 1883

    MQTT_TOPIC = (
        "smartgarden/sensors/+"
    )

    MQTT_SENSOR_ID = "soil-001"

    MQTT_STALE_AFTER_SECONDS = 30.0

    MQTT_STARTUP_TIMEOUT_SECONDS = 20.0


class RelayConfig:
    """
    Relay configuration.
    """

    GPIO_PIN = 17

    ACTIVE_LOW = False


class ValveConfig:
    """
    Two-wire, power-open / power-off-close zone valves.

    Keep simulation enabled until every physical valve and
    its separate 12 V supply have been installed and tested.
    """

    SIMULATION_MODE = True

    ACTIVE_LOW = False

    GPIO_PINS = {
        "valve-001": 5,
        "valve-002": 6,
        "valve-003": 13,
        "valve-004": 19,
        "valve-005": 26,
    }

    OPENING_DELAY_SECONDS = 8.0
    CLOSING_DELAY_SECONDS = 8.0

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

    MAX_BYTES = 5 * 1024 * 1024

    BACKUP_COUNT = 3

class IrrigationConfig:
    """
    Irrigation configuration.
    """

    # Toprak nem eşiği (%)
    DEFAULT_MOISTURE_LIMIT = 40

    # Varsayılan sulama süresi (saniye)
    DEFAULT_PUMP_DURATION_SECONDS = 20

    # Sulama sonrası bekleme süresi (v2.3.6/v2.4'te kullanılacak)
    COOLDOWN_SECONDS = 600

    DEFAULT_RESTART_DELTA = 10

    DEFAULT_COOLDOWN_SECONDS = 600

    MIN_MOISTURE_LIMIT = 5
    MAX_MOISTURE_LIMIT = 95

    MIN_PUMP_DURATION_SECONDS = 0
    MAX_PUMP_DURATION_SECONDS = 10800

    MIN_RESTART_DELTA = 1
    MAX_RESTART_DELTA = 30

    MIN_COOLDOWN_SECONDS = 60
    MAX_COOLDOWN_SECONDS = 86400

    MAX_MANUAL_PUMP_DURATION_SECONDS = 120
