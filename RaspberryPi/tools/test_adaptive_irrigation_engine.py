"""
Manual test for AdaptiveIrrigationEngine.
"""

from controllers.adaptive_irrigation_engine import (
    AdaptiveIrrigationEngine,
)
from models.watering_record import WateringRecord


def create_record(
    *,
    duration: int,
    moisture_before: int,
    moisture_after: int,
    completed: bool = True,
    mode: str = "AUTO",
) -> WateringRecord:
    """
    Create one test watering record.
    """

    return WateringRecord(
        started_at="2026-07-16T10:00:00",
        finished_at="2026-07-16T10:05:00",
        duration=duration,
        moisture_before=moisture_before,
        moisture_after=moisture_after,
        moisture_delta=(
            moisture_after
            - moisture_before
        ),
        moisture_limit=40,
        restart_delta=10,
        cooldown_seconds=600,
        completed=completed,
        stop_reason=(
            "COMPLETED"
            if completed
            else "INTERRUPTED"
        ),
        mode=mode,
        firmware="3.0.0-test",
    )


def print_recommendation(
    name: str,
    recommendation,
) -> None:
    """
    Print one recommendation.
    """

    print()
    print("=" * 72)
    print(name)
    print("=" * 72)

    print(
        f"type={recommendation.recommendation_type}"
    )

    print(
        f"reason={recommendation.reason}"
    )

    print(
        f"should_apply={recommendation.should_apply}"
    )

    print(
        f"confidence={recommendation.confidence}"
    )
    print(
        "confidence_level="
        f"{recommendation.confidence_level}"
    )
    print(
        "pump="
        f"{recommendation.current_pump_duration_seconds}"
        " -> "
        f"{recommendation.recommended_pump_duration_seconds}"
    )

    print(
        "cooldown="
        f"{recommendation.current_cooldown_seconds}"
        " -> "
        f"{recommendation.recommended_cooldown_seconds}"
    )

    print(
        "watering_count="
        f"{recommendation.watering_count_analyzed}"
    )

    print(
        "average_moisture_delta="
        f"{recommendation.average_moisture_delta}"
    )

    print(
        "average_duration="
        f"{recommendation.average_watering_duration_seconds}"
    )


def run_insufficient_data_scenario(
    engine: AdaptiveIrrigationEngine,
) -> None:
    """
    Test insufficient data.
    """

    records = [
        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=35,
        ),
        create_record(
            duration=300,
            moisture_before=31,
            moisture_after=36,
        ),
        create_record(
            duration=300,
            moisture_before=29,
            moisture_after=34,
        ),
    ]

    recommendation = engine.analyze(
        records=records,
        current_pump_duration_seconds=300,
        current_cooldown_seconds=600,
    )

    print_recommendation(
        "Senaryo 1 - Yetersiz veri",
        recommendation,
    )


def run_increase_scenario(
    engine: AdaptiveIrrigationEngine,
) -> None:
    """
    Test pump-duration increase.
    """

    records = [
        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=31,
        ),
        create_record(
            duration=300,
            moisture_before=29,
            moisture_after=31,
        ),
        create_record(
            duration=300,
            moisture_before=31,
            moisture_after=33,
        ),
        create_record(
            duration=300,
            moisture_before=28,
            moisture_after=30,
        ),
        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=32,
        ),
    ]

    recommendation = engine.analyze(
        records=records,
        current_pump_duration_seconds=300,
        current_cooldown_seconds=600,
    )

    print_recommendation(
        "Senaryo 2 - Pompa süresini artır",
        recommendation,
    )


def run_decrease_scenario(
    engine: AdaptiveIrrigationEngine,
) -> None:
    """
    Test pump-duration decrease.
    """

    records = [
        create_record(
            duration=300,
            moisture_before=25,
            moisture_after=39,
        ),
        create_record(
            duration=300,
            moisture_before=26,
            moisture_after=40,
        ),
        create_record(
            duration=300,
            moisture_before=24,
            moisture_after=38,
        ),
        create_record(
            duration=300,
            moisture_before=27,
            moisture_after=41,
        ),
        create_record(
            duration=300,
            moisture_before=25,
            moisture_after=40,
        ),
    ]

    recommendation = engine.analyze(
        records=records,
        current_pump_duration_seconds=300,
        current_cooldown_seconds=600,
    )

    print_recommendation(
        "Senaryo 3 - Pompa süresini azalt",
        recommendation,
    )


def run_keep_scenario(
    engine: AdaptiveIrrigationEngine,
) -> None:
    """
    Test keeping current settings.
    """

    records = [
        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=36,
        ),
        create_record(
            duration=300,
            moisture_before=29,
            moisture_after=36,
        ),
        create_record(
            duration=300,
            moisture_before=31,
            moisture_after=38,
        ),
        create_record(
            duration=300,
            moisture_before=28,
            moisture_after=35,
        ),
        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=37,
        ),
    ]

    recommendation = engine.analyze(
        records=records,
        current_pump_duration_seconds=300,
        current_cooldown_seconds=600,
    )

    print_recommendation(
        "Senaryo 4 - Mevcut ayarları koru",
        recommendation,
    )


def run_filtering_scenario(
    engine: AdaptiveIrrigationEngine,
) -> None:
    """
    Verify unsafe records are excluded.
    """

    records = [
        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=36,
        ),
        create_record(
            duration=300,
            moisture_before=29,
            moisture_after=36,
        ),
        create_record(
            duration=300,
            moisture_before=31,
            moisture_after=38,
        ),
        create_record(
            duration=300,
            moisture_before=28,
            moisture_after=35,
        ),
        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=37,
        ),

        # Öğrenmeye katılmaması gerekenler:

        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=50,
            completed=False,
        ),

        create_record(
            duration=300,
            moisture_before=30,
            moisture_after=50,
            mode="MANUAL",
        ),

        create_record(
            duration=300,
            moisture_before=40,
            moisture_after=30,
        ),
    ]

    recommendation = engine.analyze(
        records=records,
        current_pump_duration_seconds=300,
        current_cooldown_seconds=600,
    )

    print_recommendation(
        "Senaryo 5 - Güvensiz kayıtları filtrele",
        recommendation,
    )


def main() -> None:
    """
    Run all adaptive irrigation scenarios.
    """

    engine = AdaptiveIrrigationEngine()

    run_insufficient_data_scenario(
        engine
    )

    run_increase_scenario(
        engine
    )

    run_decrease_scenario(
        engine
    )

    run_keep_scenario(
        engine
    )

    run_filtering_scenario(
        engine
    )


if __name__ == "__main__":
    main()