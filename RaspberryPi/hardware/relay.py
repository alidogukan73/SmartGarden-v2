"""
Relay control module.
"""

from __future__ import annotations

import RPi.GPIO as GPIO

from core.config import RelayConfig
from core.logger import AppLogger


class RelayController:
    """
    Controls the irrigation relay.
    """

    def __init__(self) -> None:
        self._logger = AppLogger().logger

        self._pin = RelayConfig.GPIO_PIN

        self._initialized = False
        self._state = False

    def initialize(self) -> None:
        """
        Initialize relay GPIO.
        """

        try:
            GPIO.setmode(GPIO.BCM)
            GPIO.setwarnings(False)

            GPIO.setup(
                self._pin,
                GPIO.OUT,
            )

            # Keep relay OFF at startup.
            level = (
                GPIO.HIGH
                if RelayConfig.ACTIVE_LOW
                else GPIO.LOW
            )

            GPIO.output(
                self._pin,
                level,
            )

            self._state = False
            self._initialized = True

            self._logger.info(
                "Relay initialized on GPIO %d.",
                self._pin,
            )

        except Exception as exc:
            self._logger.exception(exc)
            raise

    def on(self) -> None:
        """
        Turn relay on.
        """

        if not self._initialized:
            raise RuntimeError(
                "Relay is not initialized.",
            )

        if self._state:
            return

        level = (
            GPIO.LOW
            if RelayConfig.ACTIVE_LOW
            else GPIO.HIGH
        )

        GPIO.output(
            self._pin,
            level,
        )

        self._state = True

        self._logger.info(
            "Relay ON.",
        )

    def off(self) -> None:
        """
        Turn relay off.
        """

        if not self._initialized:
            raise RuntimeError(
                "Relay is not initialized.",
            )

        if not self._state:
            return

        level = (
            GPIO.HIGH
            if RelayConfig.ACTIVE_LOW
            else GPIO.LOW
        )

        GPIO.output(
            self._pin,
            level,
        )

        self._state = False

        self._logger.info(
            "Relay OFF.",
        )

    def toggle(self) -> None:
        """
        Toggle relay state.
        """

        if self._state:
            self.off()
        else:
            self.on()

    @property
    def is_on(self) -> bool:
        """
        Return relay state.
        """

        return self._state

    def cleanup(self) -> None:
        """
        Leave the relay output at its inactive electrical level.

        Do not call ``GPIO.cleanup`` here. Cleanup changes the pin back to a
        high-impedance input and some relay boards then pull an active-high
        input HIGH. Holding the configured output at the inactive level keeps
        the pump safely off while the service is stopped or restarted.
        """

        if not self._initialized:
            return

        try:
            inactive_level = (
                GPIO.HIGH
                if RelayConfig.ACTIVE_LOW
                else GPIO.LOW
            )
            GPIO.output(self._pin, inactive_level)

            self._logger.info(
                "Relay GPIO held at safe OFF level.",
            )

        finally:

            self._initialized = False
            self._state = False