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

        self._last_status_update = 0.0
        self._last_health_update = 0.0
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

            if not commands.enabled:

                self._relay.off()

                self._logger.info(
                    "System disabled from Firebase.",
                )

                return

            # ---------------- AUTO MODE ----------------

            if commands.auto_mode:

                should_water = self._controller.should_water(
                    reading,
                    commands,
                )

                if should_water:

                    # Röle açılıyor

                    started_at = datetime.now()

                    result = self._controller.water(
                        duration=commands.pump_duration,
                        get_commands=lambda: self._firebase.command_state,
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

                mode = "MANUAL"

            self._logger.info(
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

            except Exception:

                pass

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