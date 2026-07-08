"""
Sensor reading model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class SensorReading:
    """
    Represents one copmlete sensor measurement.
    """

    raw: int
    voltage: float
    moisture: int
