"""
Prediction accuracy model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class PredictionAccuracy:
    """
    Describes how accurate the prediction engine has been.

    Observation mode only.
    """

    prediction_count: int

    successful_predictions: int

    average_error: float

    maximum_error: float

    minimum_error: float

    accuracy_percent: float

    confidence_multiplier: float

    status: str

    generated_at: str