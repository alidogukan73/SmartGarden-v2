"""
Device health status.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class HealthStatus:
    """
    Raspberry Pi health information.
    """

    cpu_temperature: float

    cpu_usage: float

    memory_usage: float

    disk_usage: float

    uptime_seconds: int

    ip_address: str

    wifi_signal: int

    is_throttled: bool