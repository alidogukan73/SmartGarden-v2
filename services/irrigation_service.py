"""
Irrigation service.

Contains the irrigation control logic.
"""

from __future__ import annotations

import time

from core.config import FirebaseConfig
from core.firebase_service import FirebaseService
from core.logger import AppLogger
from hardware.relay import RelayController
from hardware.sensor import SoilMoistureSensor


class IrrigationService:
    """
    Smart irrigation service.
    """

    def __init__(self) -> None:
        self._logger = AppLogger().logger

        self._sensor = SoilMoistureSensor()
        self._relay = RelayController()
        self._firebase = FirebaseService()

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

    def should_water(
        self,
        moisture: int,
        limit: int,
    ) -> bool:
        """
        Decide whether watering is required.
        """

        return moisture < limit

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

        # Firebase komutlarını oku.
        commands = self._firebase.command_state

        reading = self._sensor.read()

        # Firebase'e sensör verisini gönder.
        self._firebase.update_sensor(reading)

        # Cihaz durumunu belirli aralıklarla güncelle.
        self._update_status_if_needed()

        # Sistem devre dışı bırakıldıysa röleyi kapat.
        if not commands.enabled:

            self._relay.off()

            self._logger.info(
                "System disabled from Firebase.",
            )

            return

        if commands.auto_mode:

            if self.should_water(
                reading.moisture,
                commands.moisture_limit,
            ):
                self._water_for(
                    commands.pump_duration,
                )
            else:
                self._relay.off()

            mode = "AUTO"

        else:

            if commands.relay:
                self._relay.on()
            else:
                self._relay.off()

            mode = "MANUAL"

        self._logger.info(
            "Mode=%s Raw=%d Voltage=%.3f V Moisture=%d%% Limit=%d%% Relay=%s",
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

    def _water_for(
        self,
        duration: int,
    ) -> None:
        """
        Water for the specified duration.

        During watering, Firebase commands are checked
        periodically so irrigation can be interrupted.
        """

        self._logger.info(
            "Starting irrigation (%d s).",
            duration,
        )

        self._relay.on()

        start_time = time.monotonic()

        while time.monotonic() - start_time < duration:

            commands = self._firebase.command_state

            # Android'den sistem kapatıldıysa
            if not commands.enabled:

                self._logger.info(
                    "Irrigation interrupted (system disabled).",
                )

                break

            # Auto mod kapatıldıysa
            if not commands.auto_mode:

                self._logger.info(
                    "Irrigation interrupted (manual mode).",
                )

                break

            time.sleep(0.2)

        self._relay.off()

        self._logger.info(
            "Irrigation finished.",
        )
