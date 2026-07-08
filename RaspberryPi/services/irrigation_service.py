"""
Irrigation service.

Contains the irrigation control logic.
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


class IrrigationService:
    """
    Smart irrigation service.
    """

    def __init__(self) -> None:
        self._logger = AppLogger().logger

        self._sensor = SoilMoistureSensor()
        self._relay = RelayController()
        self._firebase = FirebaseService()

        self._controller = WateringController(
            self._relay,
        )

        self._last_status_update = 0.0

    def initialize(self) -> None:
        """
        Initialize all hardware.
        """

        self._sensor.initialize()
        self._relay.initialize()

        self._firebase.initialize()
        self._firebase.update_status()

        self._last_status_update = time.monotonic()

        self._logger.info(
            "Irrigation service initialized.",
        )

    def _update_status_if_needed(self) -> None:
        """
        Update device status periodically.
        """

        current_time = time.monotonic()

        if (
            current_time - self._last_status_update
            >= FirebaseConfig.STATUS_UPDATE_INTERVAL_SECONDS
        ):
            self._firebase.update_status()
            self._last_status_update = current_time

    def update(self) -> None:
        """
        Execute one irrigation cycle.
        """

        # Son komutları al.
        commands = self._firebase.command_state

        # Sensörü oku.
        reading = self._sensor.read()

        # Sensör verisini Firebase'e gönder.
        self._firebase.update_sensor(reading)

        # Online durumunu güncelle.
        self._update_status_if_needed()

        # Sistem kapalıysa hiçbir işlem yapma.
        if not commands.enabled:

            self._relay.off()

            self._logger.info(
                "System disabled from Firebase.",
            )

            return

        # -----------------------------
        # AUTO MODE
        # -----------------------------
        if commands.auto_mode:

            if self._controller.should_water(
                reading,
                commands,
            ):

                started_at = datetime.now()

                completed = self._controller.water(
                    duration=commands.pump_duration,
                    get_commands=lambda: self._firebase.command_state,
                )

                finished_at = datetime.now()

                record = WateringRecord(
                    started_at=started_at.isoformat(),
                    finished_at=finished_at.isoformat(),
                    duration=int(
                        (
                            finished_at - started_at
                        ).total_seconds(),
                    ),
                    moisture_before=reading.moisture,
                    moisture_limit=commands.moisture_limit,
                    completed=completed,
                    mode="AUTO",
                )

                self._firebase.add_watering_record(
                    record,
                )

            else:

                self._relay.off()

            mode = "AUTO"

        # -----------------------------
        # MANUAL MODE
        # -----------------------------
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

    def cleanup(self) -> None:
        """
        Release hardware resources.
        """

        self._firebase.stop_command_sync()

        self._relay.cleanup()

        self._logger.info(
            "Irrigation service stopped.",
        )