"""
Watering controller.

Contains irrigation decision logic.
"""

from __future__ import annotations

import time
from collections.abc import Callable

from core.logger import AppLogger
from hardware.relay import RelayController
from models.command_state import CommandState
from models.sensor_reading import SensorReading
from models.watering_state import WateringState
from models.watering_result import WateringResult

class WateringController:
    """
    Controls irrigation logic.
    """

    def __init__(
        self,
        relay: RelayController,
    ) -> None:

        self._relay = relay
        self._logger = AppLogger().logger

        # Histerezis
        self._waiting_for_reset = False

        # Son sulama zamanı (monotonic)
        self._last_watering_time = 0.0
        self._last_command_cooldown = 120

        self._state = WateringState.READY

    def should_water(
        self,
        reading: SensorReading,
        commands: CommandState,
    ) -> bool:
        """
        Decide whether irrigation is required.
        """

        now = time.monotonic()

        self._last_command_cooldown = commands.cooldown_seconds       

        # -----------------------------
        # 1) Histerezis kontrolü
        # -----------------------------
        if self._waiting_for_reset:

            self._logger.info(
                "Waiting for soil recovery..."
            )

            if (
                reading.moisture
                >= commands.moisture_limit
                + commands.restart_delta
            ):

                self._waiting_for_reset = False

                self._logger.info(
                    "Recovery completed."
                )                

                self._state = WateringState.READY

                self._logger.info(
                    "Soil moisture recovered."
                )

            else:

                self._state = WateringState.WAITING_FOR_RESET

                return False

        # -----------------------------
        # 2) Cooldown kontrolü
        # -----------------------------
        if (
            self._last_watering_time > 0
            and (
                now - self._last_watering_time
                < commands.cooldown_seconds
            )
        ):

            self._state = WateringState.COOLDOWN

            remaining = int(
                commands.cooldown_seconds
                - (now - self._last_watering_time)
            )

            self._logger.debug(
                "Cooldown active (%d s remaining).",
                remaining,
            )

            return False

        # -----------------------------
        # 3) Sulama gerekli mi?
        # -----------------------------
        if reading.moisture < commands.moisture_limit:

            self._state = WateringState.WATERING

            self._logger.info(
                "Watering required."
            )            

            return True

        self._state = WateringState.READY

        return False

    def water(
        self,
        duration: int,
        get_commands: Callable[[], CommandState],
        on_relay_changed: Callable[[bool], None] | None = None,
    ) -> WateringResult:
        """
        Water for the specified duration.

        Returns
        -------
        bool
            True if watering completed,
            False if interrupted.
        """
        started = time.monotonic()

        try:
            self._logger.info(
                "Starting irrigation (%d s).",
                duration,
            )

            self._relay.on()

            self._state = WateringState.WATERING

            if on_relay_changed is not None:
                on_relay_changed(True)

            while (
                time.monotonic()
                - started
                < duration
            ):

                commands = get_commands()

                # Sistem kapatıldı
                if not commands.enabled:

                    self._relay.off()

                    if on_relay_changed is not None:
                        on_relay_changed(False)

                    self._state = WateringState.DISABLED

                    elapsed = int(
                        time.monotonic() - started
                    )

                    return WateringResult(
                        completed=False,
                        stop_reason="SYSTEM_DISABLED",
                        duration=elapsed,
                    )

                # Manuel moda geçildi
                if not commands.auto_mode:

                    self._relay.off()

                    if on_relay_changed is not None:
                        on_relay_changed(False)

                    self._state = WateringState.MANUAL

                    elapsed = int(
                        time.monotonic() - started
                    )

                    return WateringResult(
                        completed=False,
                        stop_reason="MANUAL_MODE",
                        duration=elapsed,
                    )

                time.sleep(0.2)

            self._relay.off()

            if on_relay_changed is not None:
                on_relay_changed(False)

            # Başarıyla sulandıysa

            self._waiting_for_reset = True
            self._last_watering_time = time.monotonic()
            self._state = WateringState.COOLDOWN

            self._logger.info(
                "Irrigation completed.",
            )

            elapsed = int(
                time.monotonic() - started
            )

            return WateringResult(
                completed=True,
                stop_reason="COMPLETED",
                duration=elapsed,
            )

        except Exception as exc:

            self._relay.off()

            if on_relay_changed is not None:
                on_relay_changed(False)

            self._state = WateringState.ERROR

            self._logger.exception(exc)

            elapsed = int(
                time.monotonic() - started
            )

            return WateringResult(
                completed=False,
                stop_reason="ERROR",
                duration=elapsed,
            )       
    
    @property
    def state(self) -> WateringState:
        """
        Current irrigation state.
        """

        return self._state
    
    @property
    def cooldown_remaining(self) -> int:
        """
        Remaining cooldown time in seconds.
        """

        if self._last_watering_time <= 0:
            return 0

        remaining = int(
            self._last_watering_time
            + self._last_command_cooldown
            - time.monotonic()
        )

        return max(0, remaining)