"""
Watering history model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class WateringRecord:
    """
    Represents one watering operation.
    """

    started_at: str
    finished_at: str
    duration: int

    moisture_before: int
    moisture_after: int
    moisture_delta: int
    moisture_limit: int

    restart_delta: int
    cooldown_seconds: int

    completed: bool
    stop_reason: str

    mode: str
    firmware: str
    
    @property
    def firebase_key(
        self,
    ) -> str:
        """
        Firebase-safe key for watering history.
        """

        return (
            self.finished_at
            .replace(":", "-")
            .replace(".", "-")
        )