"""
Prediction validation status model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class PredictionValidationStatus:
    """
    Describes the current state of the prediction
    validation queue.

    Observation mode only.
    """

    validation_status: str

    pending_count: int

    target_minutes: int

    next_validation_at: str

    remaining_seconds: int

    updated_at: str