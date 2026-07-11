"""
Watering result model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class WateringResult:
    """
    Result returned after a watering cycle.
    """

    completed: bool
    stop_reason: str
    duration: int