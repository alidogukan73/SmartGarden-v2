"""
Adaptive irrigation recommendation model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class AdaptiveIrrigationRecommendation:
    """
    A safe recommendation produced from irrigation history.

    The first version operates in observation mode and does not
    change pump settings automatically.
    """

    recommendation_type: str

    should_apply: bool

    reason: str

    confidence: float

    confidence_level: str

    current_pump_duration_seconds: int

    recommended_pump_duration_seconds: int

    current_cooldown_seconds: int

    recommended_cooldown_seconds: int

    watering_count_analyzed: int

    average_moisture_delta: float

    average_watering_duration_seconds: float