"""
Safe control for two-wire motorized garden-zone valves.
"""

from __future__ import annotations

import time

import RPi.GPIO as GPIO

from core.config import ValveConfig
from core.logger import AppLogger


class ValveController:
    """
    Controls normally-closed, two-wire motorized valves.

    Applying power opens a valve. Removing power closes it.
    Only one valve may be selected at a time.
    """

    def __init__(self) -> None:
        self._logger = AppLogger().logger
        self._initialized = False
        self._active_valve_id: str | None = None

    def initialize(self) -> None:
        if self._initialized:
            return

        if ValveConfig.SIMULATION_MODE:
            self._initialized = True
            self._logger.info(
                "Zone valves initialized in simulation mode. "
                "The real pump is blocked for zone watering.",
            )
            return

        GPIO.setmode(GPIO.BCM)
        GPIO.setwarnings(False)

        for pin in ValveConfig.GPIO_PINS.values():
            GPIO.setup(pin, GPIO.OUT)
            GPIO.output(
                pin,
                GPIO.HIGH
                if ValveConfig.ACTIVE_LOW
                else GPIO.LOW,
            )

        self._initialized = True
        self._logger.info(
            "Zone valves initialized. count=%d",
            len(ValveConfig.GPIO_PINS),
        )

    def open(self, valve_id: str) -> None:
        self._require_initialized()

        if valve_id not in ValveConfig.GPIO_PINS:
            raise ValueError(
                f"Unknown garden valve: {valve_id}",
            )

        if self._active_valve_id == valve_id:
            return

        self.close_all()

        if not ValveConfig.SIMULATION_MODE:
            GPIO.output(
                ValveConfig.GPIO_PINS[valve_id],
                GPIO.LOW
                if ValveConfig.ACTIVE_LOW
                else GPIO.HIGH,
            )

        self._active_valve_id = valve_id
        self._logger.info(
            "Zone valve %s. valve_id=%s",
            "simulated OPEN"
            if ValveConfig.SIMULATION_MODE
            else "OPEN",
            valve_id,
        )

        if not ValveConfig.SIMULATION_MODE:
            time.sleep(
                ValveConfig.OPENING_DELAY_SECONDS,
            )

    def close_all(self) -> None:
        self._require_initialized()

        previous = self._active_valve_id

        if not ValveConfig.SIMULATION_MODE:
            for pin in ValveConfig.GPIO_PINS.values():
                GPIO.output(
                    pin,
                    GPIO.HIGH
                    if ValveConfig.ACTIVE_LOW
                    else GPIO.LOW,
                )

        self._active_valve_id = None

        if previous is not None:
            self._logger.info(
                "Zone valve %s. valve_id=%s",
                "simulated CLOSED"
                if ValveConfig.SIMULATION_MODE
                else "CLOSED",
                previous,
            )

            if not ValveConfig.SIMULATION_MODE:
                time.sleep(
                    ValveConfig.CLOSING_DELAY_SECONDS,
                )

    @property
    def simulation_mode(self) -> bool:
        return ValveConfig.SIMULATION_MODE

    @property
    def active_valve_id(self) -> str | None:
        return self._active_valve_id

    def cleanup(self) -> None:
        if not self._initialized:
            return

        try:
            self.close_all()
            if not ValveConfig.SIMULATION_MODE:
                for pin in ValveConfig.GPIO_PINS.values():
                    GPIO.cleanup(pin)
        finally:
            self._initialized = False
            self._active_valve_id = None

    def _require_initialized(self) -> None:
        if not self._initialized:
            raise RuntimeError(
                "Valve controller is not initialized.",
            )
