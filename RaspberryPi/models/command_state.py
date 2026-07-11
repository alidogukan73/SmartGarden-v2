"""
Command state model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class CommandState:
    """
    Represents commands received from Firebase.
    """

    auto_mode: bool = True

    relay: bool = False

    enabled: bool = True

    moisture_limit: int = 40

    pump_duration: int = 30

    # Hysteresis (%)
    restart_delta: int = 10

    # Minimum interval between irrigations (seconds)
    cooldown_seconds: int = 120