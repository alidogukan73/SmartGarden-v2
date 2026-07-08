"""
Watering controller.

Contains irrigation decision logic.
"""

from __future__ import annotations

import time

from core.logger import AppLogger
from hardware.relay import RelayController
from models.command_state import CommandState
from models.sensor_reading import SensorReading
from typing import Callable


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
        Decide whether watering is required.
        """

        return reading.moisture < commands.moisture_limit
    def water(
        self,
        duration: int,
        get_commands: Callable[[], CommandState],
    ) -> bool:
        """
        Water for the specified duration.

        Commands are checked periodically so
        irrigation can be interrupted.
        """

        self._logger.info(
            "Starting irrigation (%d s).",
            duration,
        )

        self._relay.on()

        start_time = time.monotonic()

        completed = True

        while time.monotonic() - start_time < duration:

            commands = get_commands()

            if not commands.enabled:

                self._logger.info(
                    "Irrigation interrupted (system disabled).",
                )

                completed = False
                break

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