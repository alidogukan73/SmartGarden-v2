"""
Manual test for AIExplanationEngine.
"""

from controllers.ai_explanation_engine import (
    AIExplanationEngine,
)
from models.ai_decision_summary import (
    AIDecisionSummary,
)
from models.soil_learning_profile import (
    SoilLearningProfile,
)


def create_decision(
    *,
    decision_code: str,
    decision_title: str,
    decision_message: str,
    severity: str = "INFO",
) -> AIDecisionSummary:
    """
    Create one AI decision summary for testing.
    """

    return AIDecisionSummary(
        decision_code=decision_code,
        decision_title=decision_title,
        decision_message=decision_message,
        severity=severity,
        confidence=0.80,
        confidence_level="MEDIUM",
        should_water=(
            decision_code
            == "WATERING_RECOMMENDED"
        ),
        recommendation_type=(
            "KEEP_CURRENT_SETTINGS"
        ),
        soil_classification="BALANCED",
        trend_classification="STABLE",
        primary_reason="TEST_PRIMARY_REASON",
        secondary_reason="TEST_SECONDARY_REASON",
        generated_at="2026-07-16T20:30:00",
    )


def create_soil_profile(
    *,
    profile_status: str = "READY",
    learning_stage: int = 5,
    sensor_history_count: int = 100,
    watering_count_analyzed: int = 15,
    remaining_sensor_samples: int = 0,
    remaining_auto_waterings: int = 0,
    next_milestone_code: str = "PROFILE_READY",
    next_milestone_text: str = (
        "Toprak öğrenme profili hazır"
    ),
) -> SoilLearningProfile:
    """
    Create one soil profile for testing.
    """

    return SoilLearningProfile(
        profile_status=profile_status,
        soil_classification=(
            "BALANCED"
            if profile_status == "READY"
            else "UNKNOWN"
        ),
        confidence=(
            0.90
            if profile_status == "READY"
            else 0.0
        ),
        confidence_level=(
            "HIGH"
            if profile_status == "READY"
            else "LOW"
        ),
        learning_stage=learning_stage,
        next_milestone_code=(
            next_milestone_code
        ),
        next_milestone_text=(
            next_milestone_text
        ),
        remaining_sensor_samples=(
            remaining_sensor_samples
        ),
        remaining_auto_waterings=(
            remaining_auto_waterings
        ),
        sensor_history_count=(
            sensor_history_count
        ),
        watering_count_analyzed=(
            watering_count_analyzed
        ),
        average_moisture=45.0,
        average_drying_rate_per_minute=-0.50,
        average_moisture_gain_per_watering=6.5,
        average_watering_duration_seconds=300.0,
        estimated_water_retention_minutes=2.0,
        irrigation_efficiency=0.022,
        learned_at="2026-07-16T20:30:00",
    )


def print_explanation(
    name: str,
    explanation,
) -> None:
    """
    Print one AI explanation.
    """

    print()
    print("=" * 72)
    print(name)
    print("=" * 72)

    print(
        "explanation_code="
        f"{explanation.explanation_code}"
    )

    print(
        f"title={explanation.title}"
    )

    print(
        f"summary={explanation.summary}"
    )

    print(
        f"severity={explanation.severity}"
    )

    print(
        "progress_percent="
        f"{explanation.progress_percent}"
    )

    print("reason_lines:")

    for line in explanation.reason_lines:

        print(
            f"  - {line}"
        )

    print(
        f"next_step={explanation.next_step}"
    )


def run_scenario(
    engine: AIExplanationEngine,
    *,
    name: str,
    decision: AIDecisionSummary,
    soil_profile: SoilLearningProfile,
) -> None:
    """
    Run one explanation scenario.
    """

    explanation = engine.analyze(
        decision=decision,
        soil_profile=soil_profile,
    )

    print_explanation(
        name,
        explanation,
    )


def main() -> None:
    """
    Run all explanation scenarios.
    """

    engine = AIExplanationEngine()

    run_scenario(
        engine,
        name="Senaryo 1 - Öğrenme devam ediyor",
        decision=create_decision(
            decision_code="LEARNING",
            decision_title=(
                "Sistem öğrenmeye devam ediyor"
            ),
            decision_message=(
                "Sensör verisi toplanıyor"
            ),
        ),
        soil_profile=create_soil_profile(
            profile_status="INSUFFICIENT_DATA",
            learning_stage=1,
            sensor_history_count=13,
            watering_count_analyzed=24,
            remaining_sensor_samples=7,
            next_milestone_code=(
                "COLLECT_SENSOR_HISTORY"
            ),
            next_milestone_text=(
                "Sensör verisi toplanıyor"
            ),
        ),
    )

    run_scenario(
        engine,
        name="Senaryo 2 - Sensör kararsız",
        decision=create_decision(
            decision_code="SENSOR_UNSTABLE",
            decision_title=(
                "Sensör verisi kararsız"
            ),
            decision_message=(
                "Kararlı ölçümler bekleniyor."
            ),
            severity="WARNING",
        ),
        soil_profile=create_soil_profile(),
    )

    run_scenario(
        engine,
        name="Senaryo 3 - Sistem devre dışı",
        decision=create_decision(
            decision_code="SYSTEM_DISABLED",
            decision_title="Sistem devre dışı",
            decision_message=(
                "Sulama sistemi kapalı."
            ),
        ),
        soil_profile=create_soil_profile(),
    )

    run_scenario(
        engine,
        name="Senaryo 4 - Manuel mod",
        decision=create_decision(
            decision_code="MANUAL_MODE",
            decision_title="Manuel mod etkin",
            decision_message=(
                "Pompa kullanıcı tarafından yönetiliyor."
            ),
        ),
        soil_profile=create_soil_profile(),
    )

    run_scenario(
        engine,
        name="Senaryo 5 - Sulama gerekli",
        decision=create_decision(
            decision_code="WATERING_RECOMMENDED",
            decision_title="Sulama öneriliyor",
            decision_message=(
                "Toprak nemi sınırın altında."
            ),
            severity="WARNING",
        ),
        soil_profile=create_soil_profile(),
    )

    run_scenario(
        engine,
        name="Senaryo 6 - Pompa süresini artır",
        decision=create_decision(
            decision_code="INCREASE_PUMP_DURATION",
            decision_title=(
                "Pompa süresi artırılabilir"
            ),
            decision_message=(
                "Nem kazancı düşük."
            ),
            severity="WARNING",
        ),
        soil_profile=create_soil_profile(),
    )

    run_scenario(
        engine,
        name="Senaryo 7 - Pompa süresini azalt",
        decision=create_decision(
            decision_code="DECREASE_PUMP_DURATION",
            decision_title=(
                "Pompa süresi azaltılabilir"
            ),
            decision_message=(
                "Nem kazancı yüksek."
            ),
        ),
        soil_profile=create_soil_profile(),
    )

    run_scenario(
        engine,
        name="Senaryo 8 - Sistem sağlıklı",
        decision=create_decision(
            decision_code="NO_ACTION_REQUIRED",
            decision_title=(
                "Mevcut ayarlar uygun"
            ),
            decision_message=(
                "Toprak ve sulama verileri "
                "mevcut ayarları destekliyor."
            ),
            severity="SUCCESS",
        ),
        soil_profile=create_soil_profile(),
    )


if __name__ == "__main__":
    main()