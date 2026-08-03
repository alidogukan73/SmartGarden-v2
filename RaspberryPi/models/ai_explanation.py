"""
AI explanation model.
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class AIDecisionFlow:
    """
    Individual stages used while generating an AI decision.

    Observation mode only.
    These values explain the decision but do not control irrigation.
    """

    sensor: str = ""
    sensor_status: str = "waiting"

    moisture: str = ""
    moisture_status: str = "waiting"

    soil: str = ""
    soil_status: str = "waiting"

    history: str = ""
    history_status: str = "waiting"

    result: str = ""
    result_status: str = "waiting"

    @classmethod
    def learning(
        cls,
        *,
        sensor: str,
        moisture: str,
        soil: str,
        history: str,
        result: str,
    ) -> AIDecisionFlow:
        """
        Flow used while the AI is still learning.
        """

        return cls(
            sensor=sensor,
            sensor_status="completed",

            moisture=moisture,
            moisture_status="analyzing",

            soil=soil,
            soil_status="learning",

            history=history,
            history_status="learning",

            result=result,
            result_status="analyzing",
        )

    @classmethod
    def sensor_unstable(
        cls,
        *,
        sensor: str,
        moisture: str,
        soil: str,
        history: str,
        result: str,
    ) -> AIDecisionFlow:
        """
        Flow used when sensor data is not reliable.
        """

        return cls(
            sensor=sensor,
            sensor_status="analyzing",

            moisture=moisture,
            moisture_status="waiting",

            soil=soil,
            soil_status="waiting",

            history=history,
            history_status="completed",

            result=result,
            result_status="waiting",
        )

    @classmethod
    def observation(
        cls,
        *,
        sensor: str,
        moisture: str,
        soil: str,
        history: str,
        result: str,
    ) -> AIDecisionFlow:
        """
        Flow used for disabled or manual observation modes.
        """

        return cls(
            sensor=sensor,
            sensor_status="completed",

            moisture=moisture,
            moisture_status="analyzing",

            soil=soil,
            soil_status="learning",

            history=history,
            history_status="completed",

            result=result,
            result_status="result",
        )

    @classmethod
    def completed(
        cls,
        *,
        sensor: str,
        moisture: str,
        soil: str,
        history: str,
        result: str,
    ) -> AIDecisionFlow:
        """
        Flow used when all analysis stages have completed.
        """

        return cls(
            sensor=sensor,
            sensor_status="completed",

            moisture=moisture,
            moisture_status="completed",

            soil=soil,
            soil_status="completed",

            history=history,
            history_status="completed",

            result=result,
            result_status="result",
        )


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

    decision_flow: AIDecisionFlow = field(
        default_factory=AIDecisionFlow
    )
    # ---------- AI Confidence ----------

    ai_confidence: float = 0.0

    ai_confidence_level: str = "LOW"

    # ---------- Prediction ----------

    prediction_status: str = ""

    estimated_minutes_until_limit: float = 0.0

    predicted_moisture_1_hour: float = 0.0

    predicted_moisture_3_hours: float = 0.0

    predicted_moisture_6_hours: float = 0.0

    # ---------- Prediction Accuracy ----------

    prediction_accuracy_percent: float = 0.0

    prediction_count: int = 0