"""
AI decision summary model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class AIDecisionSummary:
    """
    A unified and explainable irrigation decision.

    Observation mode only.
    This model does not modify irrigation commands.
    """

    decision_code: str

    decision_title: str

    decision_message: str

    severity: str

    confidence: float

    confidence_level: str

    should_water: bool

    recommendation_type: str

    soil_classification: str

    trend_classification: str

    primary_reason: str

    secondary_reason: str

    generated_at: str