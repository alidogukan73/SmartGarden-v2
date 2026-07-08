"""
Watering record model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class WateringRecord:
    """
    Represents one irrigation history record.
    """

    started_at: str
    finished_at: str

    duration: int

    moisture_before: int
    moisture_limit: int

    completed: bool
    mode: str