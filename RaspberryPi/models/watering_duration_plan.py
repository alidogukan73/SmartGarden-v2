"""Explainable runtime watering-duration plan."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class WateringDurationPlan:
    """One automatic cycle's bounded duration without changing saved settings."""

    configured_duration_seconds: int
    learned_duration_seconds: int
    effective_duration_seconds: int
    source: str
    reason: str
    adaptive_applied: bool
    adaptive_confidence: float
    adaptive_watering_count: int
    adaptive_recommendation_type: str
