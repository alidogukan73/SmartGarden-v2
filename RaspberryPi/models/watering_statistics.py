"""
Watering statistics model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class WateringStatistics:
    """
    Overall watering statistics.
    """

    total_waterings: int

    completed_waterings: int

    interrupted_waterings: int

    total_watering_seconds: int

    last_watering_duration: int

    last_stop_reason: str

    success_rate: int

    waterings_today: int

    watering_seconds_today: int

    statistics_date: str

    average_duration: int

    before_moisture: int

    after_moisture: int

    moisture_delta: int