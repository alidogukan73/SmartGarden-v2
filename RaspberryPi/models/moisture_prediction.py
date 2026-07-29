"""
Moisture prediction model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class MoisturePrediction:
    """
    Predicted future soil-moisture behaviour.

    Observation mode only.
    This model never changes irrigation settings
    and never controls irrigation hardware.
    """

    prediction_status: str

    prediction_method: str

    current_moisture: float

    moisture_limit: float

    drying_rate_per_minute: float

    predicted_moisture_1_hour: float

    predicted_moisture_3_hours: float

    predicted_moisture_6_hours: float

    estimated_minutes_until_limit: float

    estimated_limit_reached_at: str

    confidence: float

    confidence_level: str

    generated_at: str