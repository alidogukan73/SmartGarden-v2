"""
Pending prediction validation model.
"""

from __future__ import annotations

from dataclasses import dataclass

from models.moisture_prediction import MoisturePrediction


@dataclass(frozen=True)
class PendingPredictionValidation:
    """
    Represents a moisture prediction waiting to be
    validated against a future sensor measurement.

    Observation mode only.
    """

    prediction: MoisturePrediction

    target_minutes: int

    validate_at: str