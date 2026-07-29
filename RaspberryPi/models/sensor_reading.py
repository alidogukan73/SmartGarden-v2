"""
Sensor reading model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class SensorReading:

    raw: int
    voltage: float
    moisture: int

    sensor_id: str = ""
    firmware: str = ""
    rssi: int = 0
    uptime_seconds: int = 0