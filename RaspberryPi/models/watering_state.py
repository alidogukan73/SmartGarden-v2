"""
Watering state model.
"""

from __future__ import annotations

from enum import Enum


class WateringState(str, Enum):
    """
    Irrigation system runtime state.
    """

    READY = "READY"

    WATERING = "WATERING"

    COOLDOWN = "COOLDOWN"

    WAITING_FOR_RESET = "WAITING_FOR_RESET"

    MANUAL = "MANUAL"

    DISABLED = "DISABLED"

    ERROR = "ERROR"