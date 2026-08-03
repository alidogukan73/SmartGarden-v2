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
    relay_requested_at_ms: int = 0

    enabled: bool = True

    moisture_limit: int = 40

    pump_duration: int = 30

    # Hysteresis (%)
    restart_delta: int = 10

    # Minimum interval between irrigations (seconds)
    cooldown_seconds: int = 120

    # Device restart command
    restart_device: bool = False

    # One-shot zone valve test command from Android.
    zone_test_requested: bool = False
    zone_test_request_id: str = ""
    zone_test_zone_id: str = ""
    zone_test_valve_id: str = ""
    zone_test_duration: int = 10
    zone_test_cancel_requested: bool = False
    zone_test_requested_at_ms: int = 0
