"""
Irrigation decision model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class IrrigationDecision:
    """
    Result produced by the smart irrigation decision engine.
    """

    should_water: bool

    reason: str

    moisture: int

    moisture_limit: int

    sensor_stable: bool

    cooldown_active: bool

    trend_classification: str

    trend_sample_count: int

    moisture_change_per_minute: float
    
    trend_duration_seconds: float

    average_moisture: float