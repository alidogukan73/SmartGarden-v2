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

    def should_water(
        self,
        reading: SensorReading,
        commands: CommandState,
    ) -> bool:
        """
        Decide whether irrigation is required.
        """

        return (
            reading.moisture
            < commands.moisture_limit
        )

    def water(
        self,
        duration: int,
        get_commands: Callable[
            [],
            CommandState,
        ],
    ) -> bool:
        """
        Water for the specified duration.

        During irrigation, Firebase commands are checked
        periodically so watering can be interrupted.

        Returns
        -------
        bool
            True if watering completed normally,
            False if interrupted.
        """

        self._logger.info(
            "Starting irrigation (%d s).",
            duration,
        )

        self._relay.on()

        completed = True

        start_time = time.monotonic()

        while (
            time.monotonic() - start_time
            < duration
        ):

            commands = get_commands()

            # System disabled
            if not commands.enabled:

                self._logger.info(
                    "Irrigation interrupted (system disabled).",
                )

                completed = False
                break

            # Manual mode selected
            if not commands.auto_mode:

                self._logger.info(
                    "Irrigation interrupted (manual mode).",
                )

                completed = False
                break

            time.sleep(0.2)

        self._relay.off()

        self._logger.info(
            "Irrigation finished.",
        )

        return completed