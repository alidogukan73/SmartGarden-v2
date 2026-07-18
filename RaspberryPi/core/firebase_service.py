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
from models.irrigation_decision import IrrigationDecision
from models.sensor_history_entry import SensorHistoryEntry
from models.adaptive_irrigation_recommendation import AdaptiveIrrigationRecommendation
from models.soil_learning_profile import SoilLearningProfile
from models.ai_decision_summary import AIDecisionSummary
from models.ai_explanation import AIExplanation

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

        """
        self._logger.info(
            "Relay status sent to Firebase: %s",
            relay,
        )
        """
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

                "updated_at":
                    datetime.now().isoformat(),
            },
        )  