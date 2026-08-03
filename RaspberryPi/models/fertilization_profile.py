"""
Zone fertilization profile.

The first release is advisory only. It never controls a dosing pump.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass


@dataclass(frozen=True)
class FertilizationProfile:
    enabled: bool = False
    planting_date: str = ""
    growth_stage: str = "NOT_SET"
    active_plan_id: str = ""
    active_product_id: str = ""
    next_application_at_epoch: int = 0
    reminder_enabled: bool = True
    updated_at_epoch: int = 0

    def to_dict(self) -> dict[str, object]:
        return asdict(self)
