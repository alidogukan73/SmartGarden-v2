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
from core.logger import AppLogger
from models.command_state import CommandState
from models.sensor_reading import SensorReading
from models.watering_record import WateringRecord
from models.watering_result import WateringResult
from models.watering_statistics import WateringStatistics
from models.health_status import HealthStatus

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
            enabled=True,
            moisture_limit=IrrigationConfig.DEFAULT_MOISTURE_LIMIT,
            pump_duration=IrrigationConfig.DEFAULT_PUMP_DURATION_SECONDS,
            restart_delta=IrrigationConfig.DEFAULT_RESTART_DELTA,
            cooldown_seconds=IrrigationConfig.DEFAULT_COOLDOWN_SECONDS,
        )

        self._command_lock = threading.Lock()

        self._sync_thread: threading.Thread | None = None
        self._running = False

        self._retry_delay = 0.5
        self._max_retry_delay = 30.0

    def initialize(self) -> None:
        """
        Initialize Firebase.
        """

        if self._initialized:
            return

        try:

            credential = credentials.Certificate(
                Path(
                    FirebaseConfig.CREDENTIALS_FILE,
                ),
            )

            firebase_admin.initialize_app(
                credential,
                {
                    "databaseURL": FirebaseConfig.DATABASE_URL,
                },
            )

            self._initialized = True

            self.initialize_commands()

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
        last_watering: str,
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
                "last_watering": last_watering,
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
                "cpu_temperature": health.cpu_temperature,

                "cpu_usage": health.cpu_usage,

                "memory_usage": health.memory_usage,

                "disk_usage": health.disk_usage,

                "uptime_seconds": health.uptime_seconds,

                "ip_address": health.ip_address,

                "wifi_signal": health.wifi_signal,

                "is_throttled": health.is_throttled,

                "updated_at": datetime.now().isoformat(),
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

    # -------------------------------------------------
    # Sensor
    # -------------------------------------------------

    def update_sensor(
        self,
        reading: SensorReading,
    ) -> None:
        """
        Upload sensor values.
        """

        self._device_ref().child(
            "sensor",
        ).update(
            {
                "raw": reading.raw,
                "voltage": round(
                    reading.voltage,
                    3,
                ),
                "moisture": reading.moisture,
                "updated_at": datetime.now().isoformat(),
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
            

        
    # -------------------------------------------------
    # Commands
    # -------------------------------------------------

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

        return CommandState(
            auto_mode=bool(
                commands.get(
                    "auto_mode",
                    True,
                ),
            ),
            relay=bool(
                commands.get(
                    "relay",
                    False,
                ),
            ),
            enabled=bool(
                commands.get(
                    "enabled",
                    True,
                ),
            ),
            moisture_limit=int(
                commands.get(
                    "moisture_limit",
                    IrrigationConfig.DEFAULT_MOISTURE_LIMIT,
                ),
            ),
            pump_duration=int(
                commands.get(
                    "pump_duration",
                    IrrigationConfig.DEFAULT_PUMP_DURATION_SECONDS,
                ),
            ),
            restart_delta=int(
                commands.get(
                    "restart_delta",
                    IrrigationConfig.DEFAULT_RESTART_DELTA,
                ),
            ),
            cooldown_seconds=int(
                commands.get(
                    "cooldown_seconds",
                    IrrigationConfig.DEFAULT_COOLDOWN_SECONDS,
                ),
            ),
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

    def _sync_commands(self) -> None:
        """
        Background command synchronization.
        """

        self._logger.info(
            "Command synchronization started.",
        )

        while self._running:

            try:

                new_state = self.get_commands()

                with self._command_lock:
                    self._command_state = new_state

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

                time.sleep(
                    self._retry_delay,
                )

                self._retry_delay = min(
                    self._retry_delay * 2,
                    self._max_retry_delay,
                )

                continue

            time.sleep(
                FirebaseConfig.COMMAND_SYNC_INTERVAL_SECONDS,
            )

        self._logger.info(
            "Command synchronization stopped.",
        )