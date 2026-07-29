"""
Irrigation service.

Coordinates sensor, controller and Firebase.
"""

from __future__ import annotations

import time
from datetime import datetime


from core.config import (
    AppConfig,
    FirebaseConfig,
    IrrigationConfig,
    SensorConfig,
)
from core.firebase_service import FirebaseService
from core.logger import AppLogger
from core.system_monitor import SystemMonitor

from controllers.smart_irrigation_engine import SmartIrrigationEngine
from controllers.ai_pipeline import AIPipeline
from controllers.prediction_validation_queue import PredictionValidationQueue
from controllers.watering_controller import WateringController

from hardware.relay import RelayController
from hardware.sensor_provider import SoilMoistureSensorProvider

from models.sensor_history_entry import SensorHistoryEntry
from models.watering_record import WateringRecord
from models.moisture_prediction import MoisturePrediction


class IrrigationService:
    """
    Smart irrigation service.
    """

    def __init__(self) -> None:

        self._logger = AppLogger().logger

        self._sensor = SoilMoistureSensorProvider(
            mode=SensorConfig.SENSOR_MODE,
            mqtt_broker=SensorConfig.MQTT_BROKER,
            mqtt_port=SensorConfig.MQTT_PORT,
            mqtt_topic=SensorConfig.MQTT_TOPIC,
            mqtt_sensor_id=SensorConfig.MQTT_SENSOR_ID,
            mqtt_stale_after_seconds=(
                SensorConfig.MQTT_STALE_AFTER_SECONDS
            ),
            mqtt_startup_timeout_seconds=(
                SensorConfig.MQTT_STARTUP_TIMEOUT_SECONDS
            ),
        )

        self._system_monitor = SystemMonitor()
        self._relay = RelayController()
        self._firebase = FirebaseService()

        self._controller = WateringController(
            self._relay,
        )

        self._smart_engine = SmartIrrigationEngine()

        self._ai_pipeline = AIPipeline()
        self._prediction_validation_queue = PredictionValidationQueue()

        self._prediction_history = []
        self._prediction_history_limit = 100

        self._last_moisture_prediction = None
        self._last_prediction_accuracy = None
        self._last_unified_confidence = None
        self._last_ai_decision = None
        self._last_ai_explanation = None
        self._last_soil_learning_profile = None
        self._last_adaptive_recommendation = None

        self._last_ai_decision_update = 0.0
        self._ai_decision_interval_seconds = 30             #30 sn olacak   

        self._last_prediction_validation_status_update = 0.0
        self._prediction_validation_status_interval_seconds = 30

        self._last_irrigation_decision = None

        self._last_status_update = 0.0
        self._last_health_update = 0.0

        self._last_sensor_history_update = 0.0
        self._sensor_history_interval_seconds = 300

        self._started_at = 0.0
        self._last_watering_iso = ""

        self._update_error_active = False
        self._last_update_error_log = 0.0
        self._update_error_log_interval_seconds = 30.0

        self._manual_relay_started_at = 0.0
        self._manual_relay_timeout_latched = False

    def initialize(self) -> None:
        """
        Initialize all services.
        """

        self._sensor.initialize()

        self._relay.initialize()

        self._firebase.initialize()

        self._restore_prediction_history()

        self._firebase.update_status()

        self._update_prediction_validation_status(
            force=True,
        )

        health = self._system_monitor.read()

        self._firebase.update_health_status(
            health,
        )        

        self._last_status_update = time.monotonic()
        self._last_health_update = time.monotonic()

        self._last_sensor_history_update = time.monotonic()

        self._last_ai_decision_update = 0.0

        self._started_at = time.monotonic()

        self._logger.info(
            "Irrigation service initialized.",
        )

    def _restore_prediction_history(
        self,
    ) -> None:
        """
        Restore prediction history from Firebase.

        A restore failure must not prevent the irrigation
        service from starting.
        """

        try:
            loaded_history = (
                self._firebase.load_prediction_history()
            )

            if not isinstance(
                loaded_history,
                list,
            ):
                self._logger.warning(
                    "Prediction history restore returned "
                    "an invalid value."
                )

                self._prediction_history = []

                return

            self._prediction_history = loaded_history[
                -self._prediction_history_limit:
            ]

            self._logger.info(
                "Prediction history restored. count=%d",
                len(self._prediction_history),
            )

        except Exception as exc:
            self._prediction_history = []

            self._logger.exception(
                "Prediction history could not be restored: %s",
                exc,
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





    def _update_ai_pipeline_if_needed(
        self,
        *,
        reading,
        commands,
    ) -> None:
        """
        Execute the complete observation-mode AI pipeline
        periodically and upload its outputs to Firebase.
        """

        current_time = time.monotonic()

        if (
            current_time
            - self._last_ai_decision_update
            < self._ai_decision_interval_seconds
        ):
            return

        if self._last_irrigation_decision is None:
            return

        trend = self._smart_engine.get_current_trend()

        watering_records = (
            self._firebase.get_recent_watering_records(
                limit=30,
            )
        )

        (
            soil_profile,
            adaptive,
            prediction,
            prediction_accuracy,
            unified_confidence,
            ai_decision,
            explanation,
        ) = self._ai_pipeline.analyze(
            irrigation_decision=(
                self._last_irrigation_decision
            ),
            trend=trend,
            reading=reading,
            watering_records=watering_records,
            prediction_history=self._prediction_history,
            current_pump_duration_seconds=(
                commands.pump_duration
            ),
            current_cooldown_seconds=(
                commands.cooldown_seconds
            ),
        )

        self._firebase.update_moisture_prediction(
            prediction,
        )

        self._firebase.update_prediction_accuracy(
            prediction_accuracy,
        )

        self._firebase.update_unified_confidence(
            unified_confidence,
        )

        # -------------------------------------------------
        # Keep latest AI outputs in memory
        # -------------------------------------------------

        self._last_soil_learning_profile = (
            soil_profile
        )

        self._last_adaptive_recommendation = (
            adaptive
        )

        self._last_moisture_prediction = (
            prediction
        )

        self._last_prediction_accuracy = (
            prediction_accuracy
        )

        self._last_unified_confidence = (
            unified_confidence
        )

        self._last_ai_decision = (
            ai_decision
        )

        self._last_ai_explanation = (
            explanation
        )

        if prediction.prediction_status == "READY":

            prediction_queued = (
                self._prediction_validation_queue.enqueue(
                    prediction=prediction,
                )
            )

            if prediction_queued:
                self._logger.info(
                    "Moisture prediction queued for "
                    "one-hour validation. pending=%d",
                    self._prediction_validation_queue.count,
                )
                self._update_prediction_validation_status(
                    force=True,
                )

            else:
                self._logger.debug(
                    "Moisture prediction was not queued "
                    "because a validation is already pending.",
                )

        # -------------------------------------------------
        # Upload currently supported outputs
        # -------------------------------------------------

        self._firebase.update_soil_learning_profile(
            soil_profile,
        )

        self._firebase.update_adaptive_recommendation(
            adaptive,
        )

        self._firebase.update_ai_decision(
            ai_decision,
        )

        self._firebase.update_ai_explanation(
            explanation,
        )

        self._last_ai_decision_update = current_time

        # -------------------------------------------------
        # Logs
        # -------------------------------------------------

        self._logger.info(
            "AI pipeline updated. "
            "decision=%s severity=%s "
            "should_water=%s confidence=%s "
            "prediction_status=%s",
            ai_decision.decision_code,
            ai_decision.severity,
            ai_decision.should_water,
            unified_confidence.overall_confidence,
            prediction.prediction_status,
        )

        self._logger.info(
            "AI explanation updated. "
            "code=%s progress=%d severity=%s "
            "prediction_accuracy=%.1f count=%d",
            explanation.explanation_code,
            explanation.progress_percent,
            explanation.severity,
            prediction_accuracy.accuracy_percent,
            prediction_accuracy.prediction_count,
        )

    def _store_prediction_result(
        self,
        *,
        prediction: MoisturePrediction,
        actual_moisture: float,
    ) -> None:
        """
        Store a time-validated prediction together with
        the measured future moisture value.

        The updated history is persisted to Firebase.
        """

        self._prediction_history.append(
            (
                prediction,
                actual_moisture,
            )
        )

        if (
            len(self._prediction_history)
            > self._prediction_history_limit
        ):
            self._prediction_history = (
                self._prediction_history[
                    -self._prediction_history_limit:
                ]
            )

        self._logger.info(
            "Validated prediction stored. "
            "actual_moisture=%.2f count=%d",
            actual_moisture,
            len(self._prediction_history),
        )

        try:
            self._firebase.save_prediction_history(
                self._prediction_history,
            )

            self._logger.debug(
                "Prediction history persisted to Firebase. "
                "count=%d",
                len(self._prediction_history),
            )

        except Exception as exc:
            self._logger.exception(
                "Prediction history could not be saved: %s",
                exc,
            )

    def _validate_due_predictions(
        self,
        *,
        actual_moisture: float,
    ) -> None:
        """
        Validate pending predictions whose target time
        has arrived.

        The current sensor reading is used as the actual
        future moisture value.
        """

        validated_results = (
            self._prediction_validation_queue.validate_due(
                actual_moisture=actual_moisture,
            )
        )

        if not validated_results:
            return

        for prediction, measured_moisture in (
            validated_results
        ):
            self._store_prediction_result(
                prediction=prediction,
                actual_moisture=measured_moisture,
            )

        self._logger.info(
            "Due prediction validations completed. "
            "validated=%d pending=%d",
            len(validated_results),
            self._prediction_validation_queue.count,
        )
        self._update_prediction_validation_status(
            force=True,
        )

    def _cancel_pending_prediction_validations(
        self,
        *,
        reason: str,
    ) -> None:
        """
        Cancel pending predictions when irrigation changes
        the natural soil-moisture behaviour.
        """

        cancelled_count = (
            self._prediction_validation_queue.cancel_all()
        )

        if cancelled_count == 0:
            return

        self._logger.info(
            "Pending prediction validations cancelled. "
            "count=%d reason=%s",
            cancelled_count,
            reason,
        )
        self._update_prediction_validation_status(
            force=True,
        )

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



    def update(self) -> None:
        """
        Execute one irrigation cycle.
        """

        try:

            commands = self._firebase.command_state

            reading = self._sensor.read()

            self._firebase.update_sensor(reading)

            self._validate_due_predictions(
                actual_moisture=reading.moisture,
            )

            self._update_prediction_validation_status()

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

            self._last_irrigation_decision = decision

            self._firebase.update_irrigation_decision(
                decision,
            )
        
            self._save_sensor_history_if_needed(
                reading=reading,
                decision=decision,
            )

            self._update_ai_pipeline_if_needed(
                reading=reading,
                commands=commands,
            )

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
                self._reset_manual_relay_safety()

                self._logger.info(
                    "System disabled from Firebase.",
                )

                self._mark_update_cycle_recovered()

                return

            # ---------------- AUTO MODE ----------------

            if commands.auto_mode:

                self._reset_manual_relay_safety()

                if decision.should_water:

                    self._cancel_pending_prediction_validations(
                        reason="AUTO_IRRIGATION_STARTED",
                    )

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

                    if result.completed:
                        self._smart_engine.mark_watering_completed()

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

                manual_timed_out = (
                    self._apply_manual_relay_command(
                        commands.relay,
                    )
                )

                if commands.relay:
                    self._cancel_pending_prediction_validations(
                        reason="MANUAL_IRRIGATION_STARTED",
                    )

                if manual_timed_out:
                    try:
                        self._firebase.set_relay_command(
                            False,
                        )
                    except Exception as exc:
                        self._logger.exception(
                            "Manual relay command could not "
                            "be reset after timeout: %s",
                            exc,
                        )
                    
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

            self._mark_update_cycle_recovered()

        except Exception as exc:

            self._relay.off()

            current_time = time.monotonic()

            should_report_error = (
                not self._update_error_active
                or (
                    current_time
                    - self._last_update_error_log
                    >= self._update_error_log_interval_seconds
                )
            )

            self._update_error_active = True

            if not should_report_error:
                return

            self._last_update_error_log = current_time

            self._logger.exception(
                "Update cycle failed. Relay=%s Error=%s",
                "ON" if self._relay.is_on else "OFF",
                exc,
            )

            try:

                self._firebase.report_error(
                    str(exc),
                )

            except Exception as report_exc:
                self._logger.debug(
                    "Firebase error report failed: %s",
                    report_exc,
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

    def _apply_manual_relay_command(
        self,
        commanded_on: bool,
    ) -> bool:
        """
        Apply manual relay control with a hard safety timeout.

        Returns True when the timeout has been reached.
        """

        if not commanded_on:
            self._relay.off()
            self._reset_manual_relay_safety()
            return False

        if self._manual_relay_timeout_latched:
            self._relay.off()
            return True

        current_time = time.monotonic()

        if self._manual_relay_started_at <= 0:
            self._manual_relay_started_at = current_time

        elapsed = (
            current_time
            - self._manual_relay_started_at
        )

        if (
            elapsed
            >= IrrigationConfig.MAX_MANUAL_PUMP_DURATION_SECONDS
        ):
            self._relay.off()
            self._manual_relay_timeout_latched = True

            self._logger.warning(
                "Manual irrigation safety timeout reached. "
                "maximum=%d seconds",
                IrrigationConfig.MAX_MANUAL_PUMP_DURATION_SECONDS,
            )

            return True

        self._relay.on()

        return False

    def _reset_manual_relay_safety(self) -> None:
        """
        Reset manual relay timeout state after an OFF command
        or a mode change.
        """

        self._manual_relay_started_at = 0.0
        self._manual_relay_timeout_latched = False

    def _mark_update_cycle_recovered(self) -> None:
        """
        Log one recovery event after a failed update period.
        """

        if not self._update_error_active:
            return

        self._update_error_active = False
        self._last_update_error_log = 0.0

        self._logger.info(
            "Update cycle recovered.",
        )


    def cleanup(self) -> None:
        """
        Release resources without allowing one cleanup failure
        to prevent the remaining safety steps.
        """

        try:
            self._relay.cleanup()
        except Exception as exc:
            self._logger.exception(
                "Relay cleanup failed: %s",
                exc,
            )

        try:
            self._sensor.stop()
        except Exception as exc:
            self._logger.exception(
                "Sensor provider cleanup failed: %s",
                exc,
            )

        try:
            self._firebase.stop_command_sync()
        except Exception as exc:
            self._logger.exception(
                "Firebase command synchronization cleanup failed: %s",
                exc,
            )

        try:
            self._firebase.set_online(False)
        except Exception as exc:
            self._logger.exception(
                "Device could not be marked offline: %s",
                exc,
            )

        self._logger.info(
            "Irrigation service stopped.",
        )

    def _update_prediction_validation_status(
        self,
        *,
        force: bool = False,
    ) -> None:
        """
        Upload the current prediction-validation queue status.

        Status is uploaded periodically or immediately when
        the queue state changes.
        """

        current_time = time.monotonic()

        if (
            not force
            and (
                current_time
                - self._last_prediction_validation_status_update
                < self._prediction_validation_status_interval_seconds
            )
        ):
            return

        try:
            status = (
                self._prediction_validation_queue.get_status()
            )

            self._firebase.update_prediction_validation_status(
                status,
            )

            self._last_prediction_validation_status_update = (
                current_time
            )

            self._logger.debug(
                "Prediction validation status uploaded. "
                "status=%s pending=%d remaining=%d",
                status.validation_status,
                status.pending_count,
                status.remaining_seconds,
            )

        except Exception as exc:
            self._logger.exception(
                "Prediction validation status update failed: %s",
                exc,
            )
