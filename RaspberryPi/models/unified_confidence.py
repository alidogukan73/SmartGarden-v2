"""
Unified AI confidence model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class UnifiedConfidence:
    """
    Represents the overall confidence of the AI system.

    Observation mode only.
    """

    overall_confidence: float

    confidence_level: str

    soil_learning_confidence: float

    prediction_accuracy: float

    connection_confidence: float

    measurement_confidence: float

    decision_confidence: float

    sensor_confidence: float

    trend_confidence: float

    weighted_score: float

    status: str

    generated_at: str