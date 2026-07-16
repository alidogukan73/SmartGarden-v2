"""
Irrigation service.

Coordinates sensor, controller and Firebase.
"""

from __future__ import annotations

import time
from datetime import datetime

from controllers.watering_controller import WateringController
from core.config import FirebaseConfig
from core.firebase_service import FirebaseService
from core.logger import AppLogger
from hardware.relay import RelayController
from hardware.sensor import SoilMoistureSensor
from models.watering_record import WateringRecord
from core.config import AppConfig
from core.system_monitor import SystemMonitor
from controllers.smart_irrigation_engine import SmartIrrigationEngine
from models.sensor_history_entry import SensorHistoryEntry
from controllers.adaptive_irrigation_engine import AdaptiveIrrigationEngine
from controllers.soil_learning_engine import SoilLearningEngine

class IrrigationService:
    """
    Smart irrigation service.
    """

    def __init__(self) -> None:

        self._logger = AppLogger().logger
        self._sensor = SoilMoistureSensor()
        self._system_monitor = SystemMonitor()
        self._relay = RelayController()
        self._firebase = FirebaseService()

        self._controller = WateringController(
            self._relay,
        )

        self._smart_engine = SmartIrrigationEngine()

        self._adaptive_engine = (
            AdaptiveIrrigationEngine()
        )

        self._soil_learning_engine = (
            SoilLearningEngine()
        )

        self._last_soil_learning_analysis = 0.0

        self._soil_learning_interval_seconds = (
            1800  #1800 sn olacak
        )

        self._last_adaptive_analysis = 0.0

        self._adaptive_analysis_interval_seconds = (
            1800
        )        

        self._last_status_update = 0.0
        self._last_health_update = 0.0

        self._last_sensor_history_update = 0.0
        self._sensor_history_interval_seconds = 300

        self._started_at = 0.0
        self._last_watering_iso = ""       

    def initialize(self) -> None:
        """
        Initialize all services.
        """

        self._sensor.initialize()

        self._relay.initialize()

        self._firebase.initialize()

        self._firebase.update_status()

        health = self._system_monitor.read()

        self._firebase.update_health_status(
            health,
        )        

        self._last_status_update = time.monotonic()
        self._last_health_update = time.monotonic()

        self._last_sensor_history_update = time.monotonic()

        self._last_adaptive_analysis = time.monotonic()

        self._last_soil_learning_analysis = time.monotonic()

        self._started_at = time.monotonic()

        self._logger.info(
            "Irrigation service initialized.",
        )

    def _update_status_if_needed(self) -> None:
        """
        Update online status periodically.
        """

        current_time = time.monotonic()

        if (
            current_time - self._last_status_update
            >= FirebaseConfig.STATUS_UPDATE_INTERVAL_SECONDS
        ):

            self._firebase.update_status()

            self._last_status_update = current_time

    def _update_health_if_needed(
        self,
    ) -> None:
        """
        Update Raspberry Pi health information periodically.
        """

        current_time = time.monotonic()

        if (
            current_time - self._last_health_update
            >= FirebaseConfig.STATUS_UPDATE_INTERVAL_SECONDS
        ):

            health = self._system_monitor.read()

            self._firebase.update_health_status(
                health,
            )

            self._last_health_update = current_time

    def _save_sensor_history_if_needed(
        self,
        *,
        reading,
        decision,
    ) -> None:
        """
        Save sensor history periodically.
        """

        current_time = time.monotonic()

        if (
            current_time
            - self._last_sensor_history_update
            < self._sensor_history_interval_seconds
        ):
            return

        entry = SensorHistoryEntry(
            moisture=reading.moisture,
            voltage=reading.voltage,
            raw=reading.raw,
            trend_classification=(
                decision.trend_classification
            ),
            moisture_change_per_minute=(
                decision.moisture_change_per_minute
            ),
            trend_sample_count=(
                decision.trend_sample_count
            ),
            trend_duration_seconds=(
                decision.trend_duration_seconds
            ),

            average_moisture=(
                decision.average_moisture
            ),

            recorded_at=datetime.now().isoformat(),
        )

        self._firebase.save_sensor_history(
            entry,
        )

        self._last_sensor_history_update = current_time

    def _update_adaptive_recommendation_if_needed(
        self,
        *,
        commands,
    ) -> None:
        """
        Analyze watering history periodically and upload
        an observation-mode adaptive recommendation.
        """

        current_time = time.monotonic()

        if (
            current_time
            - self._last_adaptive_analysis
            < self._adaptive_analysis_interval_seconds
        ):
            return

        records = (
            self._firebase
            .get_recent_watering_records(
                limit=30,
            )
        )

        recommendation = (
            self._adaptive_engine.analyze(
                records=records,
                current_pump_duration_seconds=(
                    commands.pump_duration
                ),
                current_cooldown_seconds=(
                    commands.cooldown_seconds
                ),
            )
        )

        self._firebase.update_adaptive_recommendation(
            recommendation,
        )

        self._last_adaptive_analysis = current_time

        self._logger.info(
            "Adaptive recommendation updated. "
            "type=%s confidence=%s level=%s "
            "records=%d apply=%s",
            recommendation.recommendation_type,
            recommendation.confidence,
            recommendation.confidence_level,
            recommendation.watering_count_analyzed,
            recommendation.should_apply,
        )

    def _update_soil_learning_profile_if_needed(
        self,
    ) -> None:
        """
        Analyze soil behaviour periodically and upload
        the latest observation-mode learning profile.
        """

        current_time = time.monotonic()

        if (
            current_time
            - self._last_soil_learning_analysis
            < self._soil_learning_interval_seconds
        ):
            return

        moisture_trend = (
            self._smart_engine.get_current_trend()
        )

        watering_records = (
            self._firebase
            .get_recent_watering_records(
                limit=30,
            )
        )

        profile = (
            self._soil_learning_engine.analyze(
                moisture_trend=moisture_trend,
                watering_records=watering_records,
            )
        )

        self._firebase.update_soil_learning_profile(
            profile,
        )

        self._last_soil_learning_analysis = (
            current_time
        )

        self._logger.info(
            "Soil learning profile updated. "
            "status=%s classification=%s "
            "confidence=%s level=%s "
            "sensor_records=%d waterings=%d",
            profile.profile_status,
            profile.soil_classification,
            profile.confidence,
            profile.confidence_level,
            profile.sensor_history_count,
            profile.watering_count_analyzed,
        )

    def update(self) -> None:
        """
        Execute one irrigation cycle.
        """

        try:

            commands = self._firebase.command_state

            reading = self._sensor.read()

            self._firebase.update_sensor(reading)

            self._update_status_if_needed()

            self._update_health_if_needed()

            # -------------------------------------------------
            # Smart irrigation decision
            # -------------------------------------------------

            decision = self._smart_engine.evaluate(
                reading=reading,
                commands=commands,
                cooldown_active=(
                    self._controller.cooldown_remaining > 0
                ),
            )

            self._firebase.update_irrigation_decision(
                decision,
            )
        
            self._save_sensor_history_if_needed(
                reading=reading,
                decision=decision,
            )

            self._update_adaptive_recommendation_if_needed(
                commands=commands,
            )

            self._update_soil_learning_profile_if_needed()

            self._logger.debug(
                "Smart irrigation decision: "
                "should_water=%s reason=%s "
                "moisture=%d%% limit=%d%% "
                "sensor_stable=%s cooldown_active=%s "
                "trend=%s trend_samples=%d "
                "change_per_minute=%.3f",
                decision.should_water,
                decision.reason,
                decision.moisture,
                decision.moisture_limit,
                decision.sensor_stable,
                decision.cooldown_active,
                decision.trend_classification,
                decision.trend_sample_count,
                decision.moisture_change_per_minute,
            )

            if not commands.enabled:

                self._relay.off()

                self._logger.info(
                    "System disabled from Firebase.",
                )

                return

            # ---------------- AUTO MODE ----------------

            if commands.auto_mode:

                if decision.should_water:

                    started_at = datetime.now()

                    result = self._controller.water(
                        duration=commands.pump_duration,
                        get_commands=lambda: self._firebase.command_state,
                        on_relay_changed=(
                            lambda relay_on:
                            self._firebase.update_relay_status(
                                relay_on,
                            )
                        ),
                    )

                if decision.should_water:

                    # Röle açılıyor

                    started_at = datetime.now()

                    result = self._controller.water(
                        duration=commands.pump_duration,
                        get_commands=lambda: self._firebase.command_state,
                        on_relay_changed=(
                            lambda relay_on:
                            self._firebase.update_relay_status(
                                relay_on,
                            )
                        ),
                    )

                    finished_at = datetime.now()
                    finished_reading = self._sensor.read()

                    # Röle kapandı

                    record = WateringRecord(
                        started_at=started_at.isoformat(),
                        finished_at=finished_at.isoformat(),
                        duration=result.duration,

                        moisture_before=reading.moisture,
                        moisture_after=finished_reading.moisture,
                        moisture_delta=(
                            finished_reading.moisture
                            - reading.moisture
                        ),
                        moisture_limit=commands.moisture_limit,

                        restart_delta=commands.restart_delta,
                        cooldown_seconds=commands.cooldown_seconds,

                        completed=result.completed,

                        stop_reason=result.stop_reason,

                        mode="AUTO",

                        firmware=AppConfig.VERSION,
                    )

                    self._firebase.save_watering(
                        result=result,
                        record=record,
                    )
                else:

                    self._relay.off()

                mode = "AUTO"

            # ---------------- MANUAL MODE ----------------

            else:

                if commands.relay:
                    self._relay.on()
                else:
                    self._relay.off()
                    
                self._firebase.update_relay_status(
                    self._relay.is_on,
                )

                mode = "MANUAL"
            """

            Burada ki .info olunca terminalde görünüyor. .debug olunca gerekirse görünüyor 
            
            """
            self._logger.debug(
                "Mode=%s Raw=%d Voltage=%.3f V Moisture=%d%% "
                "Limit=%d%% Relay=%s",
                mode,
                reading.raw,
                reading.voltage,
                reading.moisture,
                commands.moisture_limit,
                "ON" if self._relay.is_on else "OFF",
            )

            self._logger.debug(
                "Commands: %s",
                commands,
            )

        except Exception as exc:

            self._relay.off()

            self._logger.exception(exc)

            try:

                self._firebase.report_error(
                    str(exc),
                )

            except Exception:

                pass

            self._logger.error(
                "Update cycle failed. Relay=%s",
                "ON" if self._relay.is_on else "OFF",
            )

        finally:

            uptime = int(
                time.monotonic()
                - self._started_at
            )

            try:

                self._firebase.update_runtime_status(
                    relay=self._relay.is_on,
                    uptime=uptime,
                    sensor_time=datetime.now().isoformat(),
                    watering_state=self._controller.state.value,
                    cooldown_remaining=self._controller.cooldown_remaining,
                )

            except Exception as exc:

                self._logger.exception(
                    "Runtime status update failed: %s",
                    exc,
                )


    def cleanup(self) -> None:
        """
        Release hardware resources.
        """

        self._firebase.set_online(False)

        self._firebase.stop_command_sync()

        self._relay.cleanup()

        self._logger.info(
            "Irrigation service stopped.",
        )