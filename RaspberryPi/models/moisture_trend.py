"""
Moisture trend analysis model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class MoistureTrend:
    """
    Result produced by moisture trend analysis.
    """

    classification: str

    sample_count: int

    first_moisture: int

    latest_moisture: int

    minimum_moisture: int

    maximum_moisture: int

    average_moisture: float

    total_change: int

    change_per_minute: float

    duration_seconds: float

    is_stable: bool