"""
Firebase service.

Handles Firebase Realtime Database communication.
"""

from __future__ import annotations
import threading
import time
from datetime import datetime
from pathlib import Path

import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

from core.config import AppConfig
from core.config import FirebaseConfig
from core.config import IrrigationConfig
from core.config import SensorConfig
from core.logger import AppLogger
from core.device_control import DeviceControl
from hardware.esp32_sensor_config_publisher import (
    Esp32SensorConfigPublisher,
)

from models.command_state import CommandState
from models.sensor_reading import SensorReading
from models.watering_record import WateringRecord
from models.watering_result import WateringResult
from models.watering_statistics import WateringStatistics
from models.health_status import HealthStatus
from models.irrigation_decision import IrrigationDecision
from models.sensor_history_entry import SensorHistoryEntry
from models.adaptive_irrigation_recommendation import AdaptiveIrrigationRecommendation
from models.soil_learning_profile import SoilLearningProfile
from models.ai_decision_summary import AIDecisionSummary
from models.ai_explanation import AIExplanation
from models.moisture_prediction import MoisturePrediction
from models.prediction_accuracy import PredictionAccuracy
from models.unified_confidence import UnifiedConfidence
from models.prediction_validation_status import PredictionValidationStatus

class FirebaseService:

    """
    Firebase Realtime Database service.
    """

    # -------------------------------------------------
    # Initialization
    # -------------------------------------------------
    
    def __init__(self) -> None:

        self._logger = AppLogger().logger

        self._initialized = False

        self._command_state = CommandState(
            auto_mode=True,
            relay=False,
            enabled=False,
            moisture_limit=IrrigationConfig.DEFAULT_MOISTURE_LIMIT,
            pump_duration=IrrigationConfig.DEFAULT_PUMP_DURATION_SECONDS,
            restart_delta=IrrigationConfig.DEFAULT_RESTART_DELTA,
            cooldown_seconds=IrrigationConfig.DEFAULT_COOLDOWN_SECONDS,
        )

        self._command_lock = threading.Lock()

        self._sync_thread: threading.Thread | None = None
        self._running = False
        self._stop_event = threading.Event()

        self._retry_delay = 0.5
        self._max_retry_delay = 30.0

        self._zone_by_sensor_id: dict[str, str] = {}
        self._zone_config_by_sensor_id: dict[str, dict] = {}
        self._zone_map_refreshed_at = 0.0
        self._zone_map_refresh_seconds = 10.0
        self._published_sensor_configs: dict[
            str,
            tuple[bool, int, int],
        ] = {}
        self._published_valve_hardware_map: dict[
            str,
            tuple[int, int],
        ] = {}
        self._sensor_config_publisher = (
            Esp32SensorConfigPublisher(
                broker=SensorConfig.MQTT_BROKER,
                port=SensorConfig.MQTT_PORT,
            )
        )

        self.device_control = DeviceControl()

    def initialize(self) -> None:
        """
        Initialize Firebase.
        """

        if self._initialized:
            return

        try:

            credentials_path = Path(
                FirebaseConfig.CREDENTIALS_FILE,
            )

            if not credentials_path.is_absolute():
                credentials_path = (
                    Path(__file__).resolve().parents[1]
                    / credentials_path
                )

            credential = credentials.Certificate(
                credentials_path,
            )

            firebase_admin.initialize_app(
                credential,
                {
                    "databaseURL": FirebaseConfig.DATABASE_URL,
                },
            )

            self._initialized = True

            self.initialize_commands()
            self._sensor_config_publisher.start()
            self._refresh_zone_sensor_map()

            initial_command_state = self.get_commands()

            with self._command_lock:
                self._command_state = initial_command_state

            self.check_restart_command(
                initial_command_state,
            )

            self.increment_restart_count()

            self.start_command_sync()

            self._logger.info(
                "Firebase initialized successfully.",
            )

        except Exception as exc:

            self._logger.exception(exc)

            raise
        
    def initialize_commands(
        self,
    ) -> None:
        """
        Create default commands only once.
        """

        commands_ref = self._device_ref().child(
            "commands",
        )

        if commands_ref.get() is not None:

            return

        commands_ref.set(
            {
                "auto_mode": True,
                "relay": False,
                "enabled": True,
                "moisture_limit": IrrigationConfig.DEFAULT_MOISTURE_LIMIT,
                "pump_duration": IrrigationConfig.DEFAULT_PUMP_DURATION_SECONDS,
                "restart_delta": IrrigationConfig.DEFAULT_RESTART_DELTA,
                "cooldown_seconds": IrrigationConfig.DEFAULT_COOLDOWN_SECONDS,

                # Device restart command
                "restart_device": False,
            },
        )

        self._logger.info(
            "Default commands created.",
        )

    def start_command_sync(self) -> None:
        """
        Start background synchronization.
        """

        if self._running:
            return

        self._running = True
        self._stop_event.clear()

        self._sync_thread = threading.Thread(
            target=self._sync_commands,
            daemon=True,
            name="FirebaseSync",
        )

        self._sync_thread.start()

    def stop_command_sync(self) -> None:
        """
        Stop background synchronization.
        """

        self._running = False
        self._stop_event.set()

        if self._sync_thread is None:
            return

        self._sync_thread.join(
            timeout=2,
        )

        if self._sync_thread.is_alive():

            self._logger.warning(
                "Command synchronization thread did not stop gracefully.",
            )

        self._sync_thread = None

        self._sensor_config_publisher.stop()

    # -------------------------------------------------
    # Device status
    # -------------------------------------------------

    def update_status(self) -> None:
        """
        Update device status.
        """

        self._device_ref().child(
            "status",
        ).update(
            {
                "online": True,
                "version": AppConfig.VERSION,
                "last_seen": datetime.now().isoformat(),
                "last_seen_epoch": int(time.time()),
                "last_error": "",
            },
        )

    def set_online(
        self,
        online: bool,
    ) -> None:
        """
        Update device online status.
        """

        self._device_ref().child(
            "status",
        ).update(
            {
                "online": online,
                "last_seen": datetime.now().isoformat(),
                "last_seen_epoch": int(time.time()),
            },
        )

    def update_runtime_status(
        self,
        *,
        relay: bool,
        uptime: int,
        sensor_time: str,
        watering_state: str,
        cooldown_remaining: int,
    ) -> None:
        """
        Update runtime status information.
        """

        self._device_ref().child(
            "status",
        ).update(
            {
                "relay": relay,
                "uptime_seconds": uptime,
                "last_sensor_read": sensor_time,
                "watering_state": watering_state,
                "cooldown_remaining": cooldown_remaining,
            },
        )

    def update_relay_status(
        self,
        relay: bool,
    ) -> None:
        """
        Update physical relay status immediately.
        """

        self._device_ref().child(
            "status",
        ).update(
            {
                "relay": relay,
                "last_seen": datetime.now().isoformat(),
            },
        )

    def update_active_zone_valve(
        self,
        valve_id: str | None,
        is_open: bool,
        zone_id: str | None = None,
        hardware_valve_id: str | None = None,
        is_physical: bool | None = None,
    ) -> None:
        """
        Publish the selected valve state for UI and simulation tests.
        """

        from core.config import ValveConfig

        physical = is_physical
        if physical is None:
            # Compatibility fallback for callers that do not yet provide the
            # live controller result.
            physical = (
                (valve_id or hardware_valve_id)
                in ValveConfig.PHYSICAL_VALVE_IDS
                and not ValveConfig.SIMULATION_MODE
            )

        valve_mode = "PHYSICAL" if physical else "SIMULATION"
        updated_at = datetime.now().isoformat()
        valve_status = {
            "active_valve_id": valve_id or "",
            "valve_open": is_open,
            "valve_mode": valve_mode,
        }

        self._device_ref().child("irrigation_hardware").update(
            {
                **valve_status,
                "pump_interlock": (
                    "READY" if physical else "BLOCKED"
                ),
                "updated_at": updated_at,
            },
        )

        # WateringControlActivity observes /status. Mirror the authoritative
        # state so a real open valve is never rendered as simulated.
        self._device_ref().child("status").update(
            {
                **valve_status,
                "last_seen": updated_at,
            },
        )

        if zone_id:
            self._device_ref().child(
                f"zones/{zone_id}/irrigation_status",
            ).update(
                {
                    "watering_active": is_open,
                    "updated_at": datetime.now().isoformat(),
                },
            )
            self._device_ref().child(
                f"zones/{zone_id}",
            ).update(
                {
                    "valve_mode": (
                        "PHYSICAL" if physical else "SIMULATION"
                    ),
                },
            )

    def update_health_status(
        self,
        health: HealthStatus,
    ) -> None:
        """
        Upload Raspberry Pi health information.
        """

        self._device_ref().child(
            "health",
        ).set(
            {
                "cpu_temperature":
                    health.cpu_temperature,

                "cpu_usage":
                    health.cpu_usage,

                "memory_usage":
                    health.memory_usage,

                "disk_usage":
                    health.disk_usage,

                "uptime_seconds":
                    health.uptime_seconds,

                "ip_address":
                    health.ip_address,

                "wifi_signal":
                    health.wifi_signal,

                # Genel aktif throttling durumu
                "is_throttled":
                    health.is_throttled,

                # Ham vcgencmd get_throttled değeri
                "throttled_raw":
                    health.throttled_raw,

                # Şu anda aktif olan durumlar
                "under_voltage_now":
                    health.under_voltage_now,

                "frequency_capped_now":
                    health.frequency_capped_now,

                "throttled_now":
                    health.throttled_now,

                "soft_temperature_limit_now":
                    health.soft_temperature_limit_now,

                # Geçmişte oluşmuş durumlar
                "under_voltage_history":
                    health.under_voltage_history,

                "frequency_capped_history":
                    health.frequency_capped_history,

                "throttled_history":
                    health.throttled_history,

                "soft_temperature_limit_history":
                    health.soft_temperature_limit_history,

                "firmware":
                    AppConfig.VERSION,

                "updated_at":
                    datetime.now().isoformat(),
            },
        )

    def increment_restart_count(self) -> None:
        """
        Increment device restart counter.
        """

        status_ref = self._device_ref().child(
            "status",
        )

        status = status_ref.get() or {}

        restart_count = int(
            status.get(
                "restart_count",
                0,
            ),
        )

        status_ref.update(
            {
                "restart_count": restart_count + 1,
            },
        )

    def report_error(
        self,
        message: str,
    ) -> None:
        """
        Save last application error.
        """

        self._device_ref().child(
            "status",
        ).update(
            {
                "last_error": message,
                "last_seen": datetime.now().isoformat(),
            },
        )

    def clear_error(self) -> None:
        """
        Clear the active application error after a successful
        recovery cycle.
        """

        self._device_ref().child(
            "status",
        ).update(
            {
                "last_error": "",
                "last_seen": datetime.now().isoformat(),
            },
        )

    # -------------------------------------------------
    # Sensor
    # -------------------------------------------------

    def update_zone_sensors(
        self,
        readings: dict[str, SensorReading],
    ) -> None:
        """
        Upload all fresh MQTT readings to their garden zones.

        The legacy top-level sensor node remains managed by
        update_sensor() for the primary irrigation sensor.
        """

        current_time = time.monotonic()

        if (
            not self._zone_by_sensor_id
            or (
                current_time
                - self._zone_map_refreshed_at
                >= self._zone_map_refresh_seconds
            )
        ):
            self._refresh_zone_sensor_map()

        if not readings:
            return

        updates: dict[str, object] = {}
        updated_at = datetime.now().isoformat()
        updated_at_epoch = int(time.time())

        for sensor_id, reading in readings.items():
            zone_id = self._zone_by_sensor_id.get(
                sensor_id,
            )

            if zone_id is None:
                continue

            prefix = f"zones/{zone_id}"

            updates.update(
                {
                    f"{prefix}/raw": reading.raw,
                    f"{prefix}/voltage": round(
                        reading.voltage,
                        3,
                    ),
                    f"{prefix}/moisture": reading.moisture,
                    f"{prefix}/sensor_id": reading.sensor_id,
                    f"{prefix}/firmware": reading.firmware,
                    f"{prefix}/rssi": reading.rssi,
                    f"{prefix}/uptime_seconds":
                        reading.uptime_seconds,
                    f"{prefix}/updated_at": updated_at,
                    f"{prefix}/updated_at_epoch":
                        updated_at_epoch,
                },
            )

        if updates:
            self._device_ref().update(
                updates,
            )

    def _refresh_zone_sensor_map(self) -> None:
        """
        Cache the Firebase zone assigned to each sensor ID.
        """

        zones = (
            self._device_ref()
            .child("zones")
            .get()
        )

        zone_by_sensor_id: dict[str, str] = {}
        zone_config_by_sensor_id: dict[str, dict] = {}

        if isinstance(zones, dict):
            for zone_id, zone in zones.items():
                if not isinstance(zone, dict):
                    continue

                sensor_id = str(
                    zone.get(
                        "sensor_id",
                        "",
                    ),
                ).strip()

                if sensor_id:
                    zone_by_sensor_id[
                        sensor_id
                    ] = str(zone_id)
                    zone_config_by_sensor_id[
                        sensor_id
                    ] = {
                        **zone,
                        "zone_id": str(zone_id),
                    }

        self._zone_by_sensor_id = (
            zone_by_sensor_id
        )
        self._zone_config_by_sensor_id = (
            zone_config_by_sensor_id
        )
        self._zone_map_refreshed_at = (
            time.monotonic()
        )

        self._publish_saved_sensor_configs(
            zone_config_by_sensor_id,
        )
        self._publish_valve_hardware_map(
            zone_config_by_sensor_id,
        )

        self._logger.info(
            "Garden zone sensor map refreshed. count=%d",
            len(self._zone_by_sensor_id),
        )

    def _publish_saved_sensor_configs(
        self,
        zones_by_sensor: dict[str, dict],
    ) -> None:
        """Send only explicitly saved app settings to the ESP32."""
        current_configs: dict[str, tuple[bool, int, int]] = {}

        for sensor_id, zone in zones_by_sensor.items():
            required_fields = {
                "sensor_enabled",
                "sensor_calibration_dry_raw",
                "sensor_calibration_wet_raw",
            }
            if not required_fields.issubset(zone):
                # Existing zones keep the ESP32 firmware defaults until the
                # user saves their sensor settings from Android.
                continue

            try:
                enabled = bool(zone["sensor_enabled"])
                dry_raw = int(zone["sensor_calibration_dry_raw"])
                wet_raw = int(zone["sensor_calibration_wet_raw"])
            except (TypeError, ValueError):
                self._logger.warning(
                    "Invalid saved sensor configuration ignored. sensor_id=%s",
                    sensor_id,
                )
                continue

            if dry_raw <= wet_raw:
                self._logger.warning(
                    "Unsafe saved sensor calibration ignored. sensor_id=%s",
                    sensor_id,
                )
                continue

            current_configs[sensor_id] = (
                enabled,
                dry_raw,
                wet_raw,
            )

        for sensor_id, config in current_configs.items():
            if self._published_sensor_configs.get(sensor_id) == config:
                continue

            try:
                self._sensor_config_publisher.publish(
                    sensor_id=sensor_id,
                    enabled=config[0],
                    dry_raw=config[1],
                    wet_raw=config[2],
                )
                self._published_sensor_configs[sensor_id] = config
                self._logger.info(
                    "ESP32 sensor configuration published. sensor_id=%s enabled=%s",
                    sensor_id,
                    config[0],
                )
            except Exception as exc:
                self._logger.warning(
                    "ESP32 sensor configuration publish failed. sensor_id=%s error=%s",
                    sensor_id,
                    exc,
                )

    def _publish_valve_hardware_map(
        self,
        zones_by_sensor: dict[str, dict],
    ) -> None:
        """Expose the fixed Pi wiring map to the app without making it editable."""
        from core.config import ValveConfig

        updates: dict[str, object] = {}
        for zone in zones_by_sensor.values():
            zone_id = str(zone.get("zone_id", "")).strip()
            valve_id = str(zone.get("valve_id", "")).strip()
            gpio = ValveConfig.GPIO_PINS.get(valve_id)
            physical_pin = ValveConfig.GPIO_PHYSICAL_PINS.get(valve_id)
            if not zone_id or gpio is None or physical_pin is None:
                continue

            current = (gpio, physical_pin)
            if self._published_valve_hardware_map.get(zone_id) == current:
                continue

            path = f"zones/{zone_id}/"
            updates[path + "valve_gpio_bcm"] = gpio
            updates[path + "valve_gpio_physical_pin"] = physical_pin
            self._published_valve_hardware_map[zone_id] = current

        if updates:
            self._device_ref().update(updates)

    def get_zone_config_for_sensor(
        self,
        sensor_id: str,
    ) -> dict | None:
        """
        Return the cached irrigation configuration for a sensor.
        """

        zone = self._zone_config_by_sensor_id.get(
            sensor_id,
        )

        if zone is None:
            return None

        return dict(zone)

    def get_all_zone_configs_by_sensor(
        self,
    ) -> dict[str, dict]:
        """
        Return a copy of all cached sensor-to-zone settings.
        """

        return {
            sensor_id: dict(zone)
            for sensor_id, zone
            in self._zone_config_by_sensor_id.items()
        }

    def get_physical_valve_ids(self) -> set[str]:
        """Return only zones explicitly approved for real valve control."""
        return {
            str(zone.get("valve_id", "")).strip()
            for zone in self._zone_config_by_sensor_id.values()
            if str(zone.get("valve_mode", "SIMULATION")).upper()
            == "PHYSICAL"
            and str(zone.get("valve_id", "")).strip()
        }

    def update_zone_irrigation_decisions(
        self,
        states: dict[str, dict],
    ) -> None:
        """
        Publish independent irrigation decisions under each zone.
        """

        if not states:
            return

        updates: dict[str, object] = {}
        updated_at = datetime.now().isoformat()

        for zone_id, state in states.items():
            prefix = f"zones/{zone_id}/irrigation_status"
            for field, value in state.items():
                updates[f"{prefix}/{field}"] = value
            updates[f"{prefix}/updated_at"] = updated_at

        self._device_ref().update(updates)

    def update_zone_cooldown(
        self,
        *,
        zone_id: str,
        cooldown_until_epoch: int,
        cooldown_remaining: int,
    ) -> None:
        """
        Persist a zone cooldown immediately after watering.
        """

        self._device_ref().child(
            f"zones/{zone_id}/irrigation_status",
        ).update(
            {
                "cooldown_active": cooldown_remaining > 0,
                "cooldown_remaining": max(
                    0,
                    int(cooldown_remaining),
                ),
                "cooldown_until_epoch": max(
                    0,
                    int(cooldown_until_epoch),
                ),
                "updated_at": datetime.now().isoformat(),
            },
        )
    
    def save_sensor_history(
        self,
        entry: SensorHistoryEntry,
    ) -> None:
        """
        Save one sensor history entry.
        """

        history_ref = (
            self._device_ref()
            .child("sensor_history")
            .push()
        )

        history_ref.set(
            {
                "moisture":
                    entry.moisture,

                "voltage":
                    round(
                        entry.voltage,
                        3,
                    ),

                "raw":
                    entry.raw,

                "trend_classification":
                    entry.trend_classification,

                "moisture_change_per_minute":
                    round(
                        entry.moisture_change_per_minute,
                        3,
                    ),

                "trend_sample_count":
                    entry.trend_sample_count,

                "trend_duration_seconds":
                    round(
                        entry.trend_duration_seconds,
                        2,
                    ),

                "average_moisture":
                    round(
                        entry.average_moisture,
                        2,
                    ),

                "recorded_at":
                    entry.recorded_at,
            },
        )

    # -------------------------------------------------
    # Watering
    # -------------------------------------------------

    def save_watering(
        self,
        result: WateringResult,
        record: WateringRecord,
    ) -> None:
        """
        Save all watering related data.
        """

        statistics = self.get_statistics()

        today = datetime.now().date().isoformat()

        if statistics.statistics_date != today:

            statistics.statistics_date = today
            statistics.waterings_today = 0
            statistics.watering_seconds_today = 0

        statistics.total_waterings += 1

        if result.completed:
            statistics.completed_waterings += 1
        else:
            statistics.interrupted_waterings += 1

        statistics.total_watering_seconds += result.duration

        statistics.waterings_today += 1

        statistics.watering_seconds_today += result.duration

        statistics.last_watering_duration = result.duration

        statistics.last_stop_reason = result.stop_reason

        statistics.before_moisture = record.moisture_before

        statistics.after_moisture = record.moisture_after

        statistics.moisture_delta = record.moisture_delta

        if statistics.total_waterings > 0:

            statistics.average_duration = int(
                statistics.total_watering_seconds
                / statistics.total_waterings
            )

            statistics.success_rate = int(
                statistics.completed_waterings
                * 100
                / statistics.total_waterings
            )

        else:

            statistics.average_duration = 0
            statistics.success_rate = 0

        statistics_data = {
            "total_waterings":
                statistics.total_waterings,

            "completed_waterings":
                statistics.completed_waterings,

            "interrupted_waterings":
                statistics.interrupted_waterings,

            "total_watering_seconds":
                statistics.total_watering_seconds,

            "last_watering_duration":
                statistics.last_watering_duration,

            "last_stop_reason":
                statistics.last_stop_reason,

            "success_rate":
                statistics.success_rate,

            "waterings_today":
                statistics.waterings_today,

            "watering_seconds_today":
                statistics.watering_seconds_today,

            "statistics_date":
                statistics.statistics_date,

            "average_duration":
                statistics.average_duration,

            "before_moisture":
                statistics.before_moisture,

            "after_moisture":
                statistics.after_moisture,

            "moisture_delta":
                statistics.moisture_delta,
        }

        history_data = {
            "started_at":
                record.started_at,

            "finished_at":
                record.finished_at,

            "duration":
                record.duration,

            "moisture_before":
                record.moisture_before,

            "moisture_after":
                record.moisture_after,

            "moisture_delta":
                record.moisture_delta,

            "moisture_limit":
                record.moisture_limit,

            "restart_delta":
                record.restart_delta,

            "cooldown_seconds":
                record.cooldown_seconds,

            "completed":
                record.completed,

            "stop_reason":
                record.stop_reason,

            "mode":
                record.mode,

            "firmware":
                record.firmware,

            "zone_id":
                record.zone_id,

            "sensor_id":
                record.sensor_id,
        }

        updates = {
            f"watering_history/{record.firebase_key}":
                history_data,

            "statistics":
                statistics_data,

            "status/last_watering":
                record.finished_at,
        }

        self._device_ref().update(
            updates,
        )

    def get_statistics(
        self,
    ) -> WateringStatistics:
        """
        Read watering statistics.
        """

        data = (
            self._device_ref()
            .child("statistics")
            .get()
        ) or {}

        return WateringStatistics(
            total_waterings=int(
                data.get(
                    "total_waterings",
                    0,
                ),
            ),
            completed_waterings=int(
                data.get(
                    "completed_waterings",
                    0,
                ),
            ),
            interrupted_waterings=int(
                data.get(
                    "interrupted_waterings",
                    0,
                ),
            ),
            total_watering_seconds=int(
                data.get(
                    "total_watering_seconds",
                    0,
                ),
            ),
            last_watering_duration=int(
                data.get(
                    "last_watering_duration",
                    0,
                ),
            ),

            last_stop_reason=str(
                data.get(
                    "last_stop_reason",
                    "",
                ),
            ),

            success_rate=int(
                data.get(
                    "success_rate",
                    0,
                ),
            ),
            waterings_today=int(
                data.get(
                    "waterings_today",
                    0,
                ),
            ),

            watering_seconds_today=int(
                data.get(
                    "watering_seconds_today",
                    0,
                ),
            ),

            statistics_date=str(
                data.get(
                    "statistics_date",
                    "",
                ),
            ),

            average_duration=int(
                data.get(
                    "average_duration",
                    0,
                ),
            ),

            before_moisture=int(
                data.get(
                    "before_moisture",
                    0,
                ),
            ),

            after_moisture=int(
                data.get(
                    "after_moisture",
                    0,
                ),
            ),

            moisture_delta=int(
                data.get(
                    "moisture_delta",
                    0,
                ),
            ),
        )
            
    def get_recent_watering_records(
        self,
        *,
        limit: int = 20,
    ) -> list[WateringRecord]:
        """
        Read recent watering history records.
        """

        if limit <= 0:
            return []

        data = (
            self._device_ref()
            .child("watering_history")
            .order_by_key()
            .limit_to_last(limit)
            .get()
        ) or {}

        records: list[WateringRecord] = []

        for firebase_key, item in data.items():

            if not isinstance(item, dict):
                continue

            try:

                record = WateringRecord(
                    started_at=str(
                        item.get(
                            "started_at",
                            "",
                        ),
                    ),

                    finished_at=str(
                        item.get(
                            "finished_at",
                            "",
                        ),
                    ),

                    duration=int(
                        item.get(
                            "duration",
                            0,
                        ),
                    ),

                    moisture_before=int(
                        item.get(
                            "moisture_before",
                            0,
                        ),
                    ),

                    moisture_after=int(
                        item.get(
                            "moisture_after",
                            0,
                        ),
                    ),

                    moisture_delta=int(
                        item.get(
                            "moisture_delta",
                            0,
                        ),
                    ),

                    moisture_limit=int(
                        item.get(
                            "moisture_limit",
                            0,
                        ),
                    ),

                    restart_delta=int(
                        item.get(
                            "restart_delta",
                            0,
                        ),
                    ),

                    cooldown_seconds=int(
                        item.get(
                            "cooldown_seconds",
                            0,
                        ),
                    ),

                    completed=bool(
                        item.get(
                            "completed",
                            False,
                        ),
                    ),

                    stop_reason=str(
                        item.get(
                            "stop_reason",
                            "",
                        ),
                    ),

                    mode=str(
                        item.get(
                            "mode",
                            "",
                        ),
                    ),

                    firmware=str(
                        item.get(
                            "firmware",
                            "",
                        ),
                    ),

                    zone_id=str(
                        item.get(
                            "zone_id",
                            "",
                        ),
                    ),

                    sensor_id=str(
                        item.get(
                            "sensor_id",
                            "",
                        ),
                    ),
                )

                records.append(
                    record
                )

            except (
                TypeError,
                ValueError,
            ) as exc:

                self._logger.warning(
                    "Invalid watering history record skipped. "
                    "key=%s error=%s",
                    firebase_key,
                    exc,
                )

        records.sort(
            key=lambda record: record.finished_at
        )

        return records
        
    # -------------------------------------------------
    # Commands
    # -------------------------------------------------

    def set_relay_command(
        self,
        relay: bool,
    ) -> None:
        """
        Update the manual relay command.
        """

        self._device_ref().child(
            "commands",
        ).update(
            {
                "relay": relay,
            },
        )

    def _bounded_command_int(
        self,
        *,
        commands: dict,
        field: str,
        default: int,
        minimum: int,
        maximum: int,
    ) -> int:
        """
        Parse and constrain one numeric Firebase command.
        """

        raw_value = commands.get(
            field,
            default,
        )

        try:
            value = int(raw_value)
        except (TypeError, ValueError):
            self._logger.warning(
                "Invalid Firebase command ignored. "
                "field=%s value=%r default=%d",
                field,
                raw_value,
                default,
            )
            return default

        bounded_value = max(
            minimum,
            min(
                maximum,
                value,
            ),
        )

        if bounded_value != value:
            self._logger.warning(
                "Firebase command constrained. "
                "field=%s value=%d bounded=%d range=%d..%d",
                field,
                value,
                bounded_value,
                minimum,
                maximum,
            )

        return bounded_value

    def _boolean_command(
        self,
        *,
        commands: dict,
        field: str,
        default: bool,
    ) -> bool:
        """
        Accept only real boolean Firebase command values.
        """

        value = commands.get(
            field,
            default,
        )

        if isinstance(
            value,
            bool,
        ):
            return value

        self._logger.warning(
            "Invalid boolean Firebase command ignored. "
            "field=%s value=%r default=%s",
            field,
            value,
            default,
        )

        return default

    def get_commands(self) -> CommandState:
        """
        Read commands from Firebase.
        """

        commands = (
            self._device_ref()
            .child("commands")
            .get()
        )

        if commands is None:

            return CommandState(
                auto_mode=True,
                relay=False,
                enabled=True,
                moisture_limit=IrrigationConfig.DEFAULT_MOISTURE_LIMIT,
                pump_duration=IrrigationConfig.DEFAULT_PUMP_DURATION_SECONDS,
                restart_delta=IrrigationConfig.DEFAULT_RESTART_DELTA,
                cooldown_seconds=IrrigationConfig.DEFAULT_COOLDOWN_SECONDS,

            )

        if not isinstance(
            commands,
            dict,
        ):
            raise ValueError(
                "Firebase commands must be a JSON object.",
            )

        zone_test = commands.get("zone_test", {})
        if not isinstance(zone_test, dict):
            zone_test = {}

        return CommandState(
            auto_mode=self._boolean_command(
                commands=commands,
                field="auto_mode",
                default=True,
            ),
            relay=self._boolean_command(
                commands=commands,
                field="relay",
                default=False,
            ),
            relay_requested_at_ms=int(
                commands.get("relay_requested_at", 0) or 0
            ),
            enabled=self._boolean_command(
                commands=commands,
                field="enabled",
                default=True,
            ),
            moisture_limit=self._bounded_command_int(
                commands=commands,
                field="moisture_limit",
                default=IrrigationConfig.DEFAULT_MOISTURE_LIMIT,
                minimum=IrrigationConfig.MIN_MOISTURE_LIMIT,
                maximum=IrrigationConfig.MAX_MOISTURE_LIMIT,
            ),
            pump_duration=self._bounded_command_int(
                commands=commands,
                field="pump_duration",
                default=IrrigationConfig.DEFAULT_PUMP_DURATION_SECONDS,
                minimum=IrrigationConfig.MIN_PUMP_DURATION_SECONDS,
                maximum=IrrigationConfig.MAX_PUMP_DURATION_SECONDS,
            ),
            restart_delta=self._bounded_command_int(
                commands=commands,
                field="restart_delta",
                default=IrrigationConfig.DEFAULT_RESTART_DELTA,
                minimum=IrrigationConfig.MIN_RESTART_DELTA,
                maximum=IrrigationConfig.MAX_RESTART_DELTA,
            ),
            cooldown_seconds=self._bounded_command_int(
                commands=commands,
                field="cooldown_seconds",
                default=IrrigationConfig.DEFAULT_COOLDOWN_SECONDS,
                minimum=IrrigationConfig.MIN_COOLDOWN_SECONDS,
                maximum=IrrigationConfig.MAX_COOLDOWN_SECONDS,
            ),
            restart_device=self._boolean_command(
                commands=commands,
                field="restart_device",
                default=False,
            ),
            zone_test_requested=self._boolean_command(
                commands=zone_test,
                field="requested",
                default=False,
            ),
            zone_test_request_id=str(
                zone_test.get("request_id", ""),
            ),
            zone_test_zone_id=str(
                zone_test.get("zone_id", ""),
            ),
            zone_test_valve_id=str(
                zone_test.get("valve_id", ""),
            ),
            zone_test_duration=self._bounded_command_int(
                commands=zone_test,
                field="duration",
                default=10,
                minimum=1,
                maximum=IrrigationConfig.MAX_PUMP_DURATION_SECONDS,
            ),
            zone_test_cancel_requested=self._boolean_command(
                commands=zone_test,
                field="cancel_requested",
                default=False,
            ),
            zone_test_requested_at_ms=int(
                zone_test.get("requested_at", 0) or 0
            ),
        )

    def acknowledge_zone_test(
        self,
        *,
        request_id: str,
        result: str,
        active: bool = False,
        remaining_seconds: int = 0,
    ) -> None:
        """
        Complete a one-shot Android zone test command.
        """

        self._device_ref().child("commands").child(
            "zone_test",
        ).update(
            {
                "requested": False,
                "cancel_requested": False,
                "active": active,
                "remaining_seconds": max(0, remaining_seconds),
                "result": result,
                "completed_request_id": request_id,
                "completed_at": datetime.now().isoformat(),
            },
        )

    @property
    def command_state(self) -> CommandState:
        """
        Return cached commands.
        """

        with self._command_lock:

            return self._command_state

    # -------------------------------------------------
    # Private
    # -------------------------------------------------

    def _device_ref(self) -> db.Reference:
        """
        Return device database reference.
        """

        if not self._initialized:
            raise RuntimeError(
                "Firebase is not initialized.",
            )

        return db.reference(
            f"devices/{AppConfig.DEVICE_ID}",
        )

    def check_restart_command(
            self,
            command: CommandState,
    ) -> None:
        """
        Handle a one-time device restart request.

        The Firebase command is acknowledged before rebooting
        to prevent an endless restart loop.
        """

        if not command.restart_device:
            return

        self._logger.warning(
            "Device restart request detected.",
        )

        # Önce komutu tüketiyoruz.
        # Raspberry yeniden açıldığında tekrar restart etmesin.
        self._device_ref().child(
            "commands",
        ).update(
            {
                "restart_device": False,
            },
        )

        self._logger.warning(
            "Restart command acknowledged in Firebase.",
        )

        self.device_control.restart_device()

    def _sync_commands(self) -> None:
        """
        Background command synchronization.
        """

        self._logger.info(
            "Command synchronization started.",
        )

        while (
            self._running
            and not self._stop_event.is_set()
        ):

            try:

                new_state = self.get_commands()

                with self._command_lock:
                    self._command_state = new_state

                self.check_restart_command(
                    new_state
                )

                # Connection recovered
                self._retry_delay = 0.5

            except Exception as exc:

                self._logger.warning(
                    "Firebase unavailable. Retrying in %.1f seconds.",
                    self._retry_delay,
                )

                self._logger.debug(
                    "Firebase error: %s",
                    exc,
                )

                if self._stop_event.wait(
                    self._retry_delay,
                ):
                    break

                self._retry_delay = min(
                    self._retry_delay * 2,
                    self._max_retry_delay,
                )

                continue

            if self._stop_event.wait(
                FirebaseConfig.COMMAND_SYNC_INTERVAL_SECONDS,
            ):
                break

        self._logger.info(
            "Command synchronization stopped.",
        )

    def update_irrigation_decision(
        self,
        decision: IrrigationDecision,
    ) -> None:
        """
        Upload the latest smart irrigation decision.
        """

        self._device_ref().child(
            "decision",
        ).set(
            {
                "should_water":
                    decision.should_water,

                "reason":
                    decision.reason,

                "moisture":
                    decision.moisture,

                "moisture_limit":
                    decision.moisture_limit,

                "sensor_stable":
                    decision.sensor_stable,

                "cooldown_active":
                    decision.cooldown_active,

                "trend_classification":
                    decision.trend_classification,

                "trend_sample_count":
                    decision.trend_sample_count,

                "moisture_change_per_minute":
                    decision.moisture_change_per_minute,

                "trend_duration_seconds":
                    round(
                        decision.trend_duration_seconds,
                        2,
                    ),

                "average_moisture":
                    round(
                        decision.average_moisture,
                        2,
                    ),

                "updated_at":
                    datetime.now().isoformat(),
            },
        )

    def update_adaptive_recommendation(
        self,
        recommendation: AdaptiveIrrigationRecommendation,
    ) -> None:
        """
        Upload the latest adaptive irrigation recommendation.

        Observation mode only.
        This method does not modify Firebase commands.
        """

        self._device_ref().child(
            "adaptive_recommendation",
        ).set(
            {
                "recommendation_type":
                    recommendation.recommendation_type,

                "reason":
                    recommendation.reason,

                "should_apply":
                    recommendation.should_apply,

                "confidence":
                    recommendation.confidence,

                "confidence_level":
                    recommendation.confidence_level,

                "current_pump_duration_seconds":
                    recommendation.current_pump_duration_seconds,

                "recommended_pump_duration_seconds":
                    recommendation.recommended_pump_duration_seconds,

                "current_cooldown_seconds":
                    recommendation.current_cooldown_seconds,

                "recommended_cooldown_seconds":
                    recommendation.recommended_cooldown_seconds,

                "watering_count_analyzed":
                    recommendation.watering_count_analyzed,

                "average_moisture_delta":
                    recommendation.average_moisture_delta,

                "average_watering_duration_seconds":
                    recommendation.average_watering_duration_seconds,

                "updated_at":
                    datetime.now().isoformat(),
            },
        ) 

    def update_soil_learning_profile(
        self,
        profile: SoilLearningProfile,
    ) -> None:
        """
        Upload the latest learned soil behaviour profile.

        Observation mode only.
        This method does not modify irrigation commands.
        """

        self._device_ref().child(
            "soil_learning_profile",
        ).set(
            {
                "profile_status":
                    profile.profile_status,

                "soil_classification":
                    profile.soil_classification,

                "confidence":
                    profile.confidence,

                "confidence_level":
                    profile.confidence_level,

                "learning_stage":
                    profile.learning_stage,

                "next_milestone_code":
                    profile.next_milestone_code,

                "next_milestone_text":
                    profile.next_milestone_text,

                "remaining_sensor_samples":
                    profile.remaining_sensor_samples,

                "remaining_auto_waterings":
                    profile.remaining_auto_waterings,

                "sensor_history_count":
                    profile.sensor_history_count,

                "watering_count_analyzed":
                    profile.watering_count_analyzed,

                "average_moisture":
                    round(
                        profile.average_moisture,
                        2,
                    ),

                "average_drying_rate_per_minute":
                    round(
                        profile.average_drying_rate_per_minute,
                        3,
                    ),

                "average_moisture_gain_per_watering":
                    round(
                        profile.average_moisture_gain_per_watering,
                        2,
                    ),

                "average_watering_duration_seconds":
                    round(
                        profile.average_watering_duration_seconds,
                        2,
                    ),

                "estimated_water_retention_minutes":
                    round(
                        profile.estimated_water_retention_minutes,
                        2,
                    ),

                "irrigation_efficiency":
                    round(
                        profile.irrigation_efficiency,
                        3,
                    ),

                "learned_at":
                    profile.learned_at,

                "updated_at":
                    datetime.now().isoformat(),
            },
        )

    def update_ai_decision(
        self,
        summary: AIDecisionSummary,
    ) -> None:
        """
        Upload the latest unified AI decision summary.

        Observation mode only.
        This method does not modify irrigation commands.
        """

        self._device_ref().child(
            "ai_decision",
        ).set(
            {
                "decision_code":
                    summary.decision_code,

                "decision_title":
                    summary.decision_title,

                "decision_message":
                    summary.decision_message,

                "severity":
                    summary.severity,

                "confidence":
                    round(
                        summary.confidence,
                        2,
                    ),

                "confidence_level":
                    summary.confidence_level,

                "should_water":
                    summary.should_water,

                "recommendation_type":
                    summary.recommendation_type,

                "soil_classification":
                    summary.soil_classification,

                "trend_classification":
                    summary.trend_classification,

                "primary_reason":
                    summary.primary_reason,

                "secondary_reason":
                    summary.secondary_reason,

                "generated_at":
                    summary.generated_at,

                "updated_at":
                    datetime.now().isoformat(),
            },
        )
    def update_ai_explanation(
        self,
        explanation: AIExplanation,
    ) -> None:
        """
        Upload the latest user-friendly AI explanation.

        Observation mode only.
        This method does not modify irrigation commands.
        """

        self._device_ref().child(
            "ai_explanation",
        ).set(
            {
                "explanation_code":
                    explanation.explanation_code,

                "title":
                    explanation.title,

                "summary":
                    explanation.summary,

                "reason_lines":
                    list(
                        explanation.reason_lines
                    ),

                "next_step":
                    explanation.next_step,

                "progress_percent":
                    max(
                        0,
                        min(
                            explanation.progress_percent,
                            100,
                        ),
                    ),

                "severity":
                    explanation.severity,

                "generated_at":
                    explanation.generated_at,

                "decision_flow": {
                    "sensor": explanation.decision_flow.sensor,
                    "sensor_status": (
                        explanation.decision_flow.sensor_status
                    ),

                    "moisture": explanation.decision_flow.moisture,
                    "moisture_status": (
                        explanation.decision_flow.moisture_status
                    ),

                    "soil": explanation.decision_flow.soil,
                    "soil_status": (
                        explanation.decision_flow.soil_status
                    ),

                    "history": explanation.decision_flow.history,
                    "history_status": (
                        explanation.decision_flow.history_status
                    ),

                    "result": explanation.decision_flow.result,
                    "result_status": (
                        explanation.decision_flow.result_status
                    ),
                },

                "updated_at":
                    datetime.now().isoformat(),
            },
        )

    def update_prediction_validation_status(
        self,
        status: PredictionValidationStatus,
    ) -> None:
        """
        Upload prediction-validation queue status.

        Observation mode only.
        """

        self._device_ref().child(
            "ai",
        ).child(
            "prediction_validation",
        ).set(
            {
                "validation_status":
                    status.validation_status,

                "pending_count":
                    status.pending_count,

                "target_minutes":
                    status.target_minutes,

                "next_validation_at":
                    status.next_validation_at,

                "remaining_seconds":
                    status.remaining_seconds,

                "updated_at":
                    status.updated_at,
            }
        )

        self._logger.debug(
            "Prediction validation status updated. "
            "status=%s pending=%d remaining=%d",
            status.validation_status,
            status.pending_count,
            status.remaining_seconds,
        )

    def update_moisture_prediction(
        self,
        prediction: MoisturePrediction,
    ) -> None:
        """
        Upload the latest moisture prediction.

        Observation mode only.
        """

        self._device_ref().child(
            "moisture_prediction",
        ).set(
            {
                "prediction_status":
                    prediction.prediction_status,

                "prediction_method":
                    prediction.prediction_method,

                "current_moisture":
                    round(
                        prediction.current_moisture,
                        2,
                    ),

                "moisture_limit":
                    round(
                        prediction.moisture_limit,
                        2,
                    ),

                "drying_rate_per_minute":
                    round(
                        prediction.drying_rate_per_minute,
                        3,
                    ),

                "predicted_moisture_1_hour":
                    round(
                        prediction.predicted_moisture_1_hour,
                        2,
                    ),

                "predicted_moisture_3_hours":
                    round(
                        prediction.predicted_moisture_3_hours,
                        2,
                    ),

                "predicted_moisture_6_hours":
                    round(
                        prediction.predicted_moisture_6_hours,
                        2,
                    ),

                "estimated_minutes_until_limit":
                    round(
                        prediction.estimated_minutes_until_limit,
                        2,
                    ),

                "estimated_limit_reached_at":
                    prediction.estimated_limit_reached_at,

                "confidence":
                    round(
                        prediction.confidence,
                        2,
                    ),

                "confidence_level":
                    prediction.confidence_level,

                "generated_at":
                    prediction.generated_at,

                "updated_at":
                    datetime.now().isoformat(),
            },
        )

    def update_prediction_accuracy(
        self,
        accuracy: PredictionAccuracy,
    ) -> None:
        """
        Upload prediction accuracy statistics.

        Observation mode only.
        """

        self._device_ref().child(
            "prediction_accuracy",
        ).set(
            {
                "prediction_count":
                    accuracy.prediction_count,

                "successful_predictions":
                    accuracy.successful_predictions,

                "average_error":
                    round(
                        accuracy.average_error,
                        2,
                    ),

                "maximum_error":
                    round(
                        accuracy.maximum_error,
                        2,
                    ),

                "minimum_error":
                    round(
                        accuracy.minimum_error,
                        2,
                    ),

                "accuracy_percent":
                    round(
                        accuracy.accuracy_percent,
                        2,
                    ),

                "confidence_multiplier":
                    round(
                        accuracy.confidence_multiplier,
                        3,
                    ),

                "status":
                    accuracy.status,

                "generated_at":
                    accuracy.generated_at,

                "updated_at":
                    datetime.now().isoformat(),
            },
        )

    def update_unified_confidence(
        self,
        confidence: UnifiedConfidence,
    ) -> None:
        """
        Upload unified AI confidence.

        Observation mode only.
        """

        self._device_ref().child(
            "unified_confidence",
        ).set(
            {
                "overall_confidence":
                    round(
                        confidence.overall_confidence,
                        2,
                    ),

                "confidence_level":
                    confidence.confidence_level,

                "soil_learning_confidence":
                    round(
                        confidence.soil_learning_confidence,
                        2,
                    ),

                "prediction_accuracy":
                    round(
                        confidence.prediction_accuracy,
                        2,
                    ),

                "sensor_confidence":
                    round(
                        confidence.sensor_confidence,
                        2,
                    ),

                "trend_confidence":
                    round(
                        confidence.trend_confidence,
                        2,
                    ),

                "weighted_score":
                    round(
                        confidence.weighted_score,
                        3,
                    ),

                "status":
                    confidence.status,

                "generated_at":
                    confidence.generated_at,

                "updated_at":
                    datetime.now().isoformat(),
            },
        )

    def save_prediction_history(
        self,
        history: list[
            tuple[
                MoisturePrediction,
                float,
            ]
        ],
    ) -> None:
        """
        Save prediction history to Firebase.

        Each history item contains:

            (
                prediction,
                actual_moisture,
            )

        Observation mode only.
        """

        history_data: dict[str, dict] = {}

        for index, (
            prediction,
            actual_moisture,
        ) in enumerate(
            history,
            start=1,
        ):
            item_key = f"item_{index:04d}"

            history_data[item_key] = {
                "prediction_status":
                    prediction.prediction_status,

                "prediction_method":
                    prediction.prediction_method,

                "current_moisture":
                    prediction.current_moisture,

                "moisture_limit":
                    prediction.moisture_limit,

                "drying_rate_per_minute":
                    prediction.drying_rate_per_minute,

                "predicted_moisture_1_hour":
                    prediction.predicted_moisture_1_hour,

                "predicted_moisture_3_hours":
                    prediction.predicted_moisture_3_hours,

                "predicted_moisture_6_hours":
                    prediction.predicted_moisture_6_hours,

                "estimated_minutes_until_limit":
                    prediction.estimated_minutes_until_limit,

                "estimated_limit_reached_at":
                    prediction.estimated_limit_reached_at,

                "confidence":
                    prediction.confidence,

                "confidence_level":
                    prediction.confidence_level,

                "generated_at":
                    prediction.generated_at,

                "actual_moisture":
                    actual_moisture,
            }

        history_ref = (
            self._device_ref()
            .child("ai")
            .child("prediction_history")
        )

        if not history_data:
            history_ref.delete()

            self._logger.debug(
                "Prediction history cleared.",
            )

            return

        history_ref.set(history_data)

        self._logger.debug(
            "Prediction history saved. count=%d",
            len(history),
        )

    def load_prediction_history(
        self,
    ) -> list[
        tuple[
            MoisturePrediction,
            float,
        ]
    ]:
        """
        Load prediction history from Firebase.

        Invalid or incomplete history items are skipped.

        Observation mode only.
        """

        history_ref = (
            self._device_ref()
            .child("ai")
            .child("prediction_history")
        )

        history_data = history_ref.get()

        if not isinstance(
            history_data,
            dict,
        ):
            self._logger.info(
                "No prediction history found.",
            )

            return []

        loaded_history: list[
            tuple[
                MoisturePrediction,
                float,
            ]
        ] = []

        for item_key in sorted(
            history_data.keys(),
        ):
            item = history_data[item_key]

            if not isinstance(
                item,
                dict,
            ):
                self._logger.warning(
                    "Invalid prediction history item skipped. "
                    "key=%s",
                    item_key,
                )

                continue

            try:
                prediction = MoisturePrediction(
                    prediction_status=str(
                        item["prediction_status"],
                    ),

                    prediction_method=str(
                        item["prediction_method"],
                    ),

                    current_moisture=float(
                        item["current_moisture"],
                    ),

                    moisture_limit=float(
                        item["moisture_limit"],
                    ),

                    drying_rate_per_minute=float(
                        item[
                            "drying_rate_per_minute"
                        ],
                    ),

                    predicted_moisture_1_hour=float(
                        item[
                            "predicted_moisture_1_hour"
                        ],
                    ),

                    predicted_moisture_3_hours=float(
                        item[
                            "predicted_moisture_3_hours"
                        ],
                    ),

                    predicted_moisture_6_hours=float(
                        item[
                            "predicted_moisture_6_hours"
                        ],
                    ),

                    estimated_minutes_until_limit=float(
                        item[
                            "estimated_minutes_until_limit"
                        ],
                    ),

                    estimated_limit_reached_at=str(
                        item[
                            "estimated_limit_reached_at"
                        ],
                    ),

                    confidence=float(
                        item["confidence"],
                    ),

                    confidence_level=str(
                        item["confidence_level"],
                    ),

                    generated_at=str(
                        item["generated_at"],
                    ),
                )

                actual_moisture = float(
                    item["actual_moisture"],
                )

            except (
                KeyError,
                TypeError,
                ValueError,
            ) as error:
                self._logger.warning(
                    "Prediction history item could not "
                    "be loaded. key=%s error=%s",
                    item_key,
                    error,
                )

                continue

            loaded_history.append(
                (
                    prediction,
                    actual_moisture,
                )
            )

        self._logger.info(
            "Prediction history loaded. count=%d",
            len(loaded_history),
        )

        return loaded_history
