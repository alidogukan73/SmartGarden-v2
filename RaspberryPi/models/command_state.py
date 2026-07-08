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

    auto_mode: bool
    relay: bool
    enabled: bool

    moisture_limit: int
    pump_duration: int