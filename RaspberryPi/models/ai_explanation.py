"""
AI explanation model.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class AIExplanation:
    """
    User-friendly explanation generated from AI engine outputs.

    Observation mode only.
    This model does not modify irrigation commands.
    """

    explanation_code: str

    title: str

    summary: str

    reason_lines: tuple[str, ...]

    next_step: str

    progress_percent: int

    severity: str

    generated_at: str