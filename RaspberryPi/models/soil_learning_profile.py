"""
Soil learning profile model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class SoilLearningProfile:
    """
    Learned characteristics of the soil and irrigation system.

    Observation mode only.
    This profile does not change irrigation settings automatically.
    """

    profile_status: str

    soil_classification: str

    confidence: float

    confidence_level: str

    learning_stage: int

    next_milestone_code: str

    next_milestone_text: str

    remaining_sensor_samples: int

    remaining_auto_waterings: int

    sensor_history_count: int

    watering_count_analyzed: int

    average_moisture: float

    average_drying_rate_per_minute: float

    average_moisture_gain_per_watering: float

    average_watering_duration_seconds: float

    estimated_water_retention_minutes: float

    irrigation_efficiency: float

    learned_at: str