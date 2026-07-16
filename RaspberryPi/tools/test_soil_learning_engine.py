"""
Manual test for SoilLearningEngine.
"""

from controllers.soil_learning_engine import (
    SoilLearningEngine,
)
from models.moisture_trend import MoistureTrend
from models.watering_record import WateringRecord


def create_trend(
    *,
    classification: str,
    sample_count: int,
    change_per_minute: float,
    duration_seconds: float,
    average_moisture: float = 45.0,
    is_stable: bool = False,
) -> MoistureTrend:
    """
    Create one test moisture trend.
    """

    return MoistureTrend(
        classification=classification,
        sample_count=sample_count,
        first_moisture=50,
        latest_moisture=40,
        minimum_moisture=40,
        maximum_moisture=50,
        average_moisture=average_moisture,
        total_change=-10,
        change_per_minute=change_per_minute,
        duration_seconds=duration_seconds,
        is_stable=is_stable,
    )


def create_record(
    *,
    moisture_delta: int = 6,
    duration: int = 300,
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
        moisture_before=30,
        moisture_after=(
            30
            + moisture_delta
        ),
        moisture_delta=moisture_delta,
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


def create_valid_records(
    count: int = 5,
) -> list[WateringRecord]:
    """
    Create reliable automatic watering records.
    """

    return [
        create_record(
            moisture_delta=6 + index % 2,
            duration=300,
        )
        for index in range(
            count
        )
    ]


def print_profile(
    name: str,
    profile,
) -> None:
    """
    Print one soil-learning profile.
    """

    print()
    print("=" * 72)
    print(name)
    print("=" * 72)

    print(
        f"profile_status={profile.profile_status}"
    )

    print(
        "soil_classification="
        f"{profile.soil_classification}"
    )

    print(
        f"confidence={profile.confidence}"
    )

    print(
        "confidence_level="
        f"{profile.confidence_level}"
    )

    print(
        "sensor_history_count="
        f"{profile.sensor_history_count}"
    )

    print(
        "watering_count="
        f"{profile.watering_count_analyzed}"
    )

    print(
        "average_moisture="
        f"{profile.average_moisture}"
    )

    print(
        "average_drying_rate="
        f"{profile.average_drying_rate_per_minute}"
    )

    print(
        "average_gain="
        f"{profile.average_moisture_gain_per_watering}"
    )

    print(
        "average_duration="
        f"{profile.average_watering_duration_seconds}"
    )

    print(
        "retention_minutes_per_point="
        f"{profile.estimated_water_retention_minutes}"
    )

    print(
        "irrigation_efficiency="
        f"{profile.irrigation_efficiency}"
    )


def run_scenario(
    engine: SoilLearningEngine,
    *,
    name: str,
    trend: MoistureTrend,
    records: list[WateringRecord],
) -> None:
    """
    Run one soil-learning scenario.
    """

    profile = engine.analyze(
        moisture_trend=trend,
        watering_records=records,
    )

    print_profile(
        name,
        profile,
    )


def main() -> None:
    """
    Run all soil-learning scenarios.
    """

    engine = SoilLearningEngine()

    run_scenario(
        engine,
        name="Senaryo 1 - Yetersiz sulama verisi",
        trend=create_trend(
            classification="NORMAL_DRYING",
            sample_count=100,
            change_per_minute=-0.50,
            duration_seconds=3600,
        ),
        records=create_valid_records(
            count=3,
        ),
    )

    run_scenario(
        engine,
        name="Senaryo 2 - Yetersiz sensör geçmişi",
        trend=create_trend(
            classification="STABLE",
            sample_count=10,
            change_per_minute=-0.05,
            duration_seconds=3600,
            is_stable=True,
        ),
        records=create_valid_records(),
    )

    run_scenario(
        engine,
        name="Senaryo 3 - Yüksek su tutma",
        trend=create_trend(
            classification="STABLE",
            sample_count=100,
            change_per_minute=-0.05,
            duration_seconds=3600,
            is_stable=True,
        ),
        records=create_valid_records(
            count=15,
        ),
    )

    run_scenario(
        engine,
        name="Senaryo 4 - Yavaş kuruma",
        trend=create_trend(
            classification="SLOW_DRYING",
            sample_count=100,
            change_per_minute=-0.20,
            duration_seconds=3600,
        ),
        records=create_valid_records(
            count=15,
        ),
    )

    run_scenario(
        engine,
        name="Senaryo 5 - Dengeli kuruma",
        trend=create_trend(
            classification="NORMAL_DRYING",
            sample_count=100,
            change_per_minute=-0.50,
            duration_seconds=3600,
        ),
        records=create_valid_records(
            count=15,
        ),
    )

    run_scenario(
        engine,
        name="Senaryo 6 - Hızlı kuruma",
        trend=create_trend(
            classification="FAST_DRYING",
            sample_count=100,
            change_per_minute=-1.00,
            duration_seconds=3600,
        ),
        records=create_valid_records(
            count=15,
        ),
    )

    run_scenario(
        engine,
        name="Senaryo 7 - Çok hızlı kuruma",
        trend=create_trend(
            classification="VERY_FAST_DRYING",
            sample_count=100,
            change_per_minute=-1.60,
            duration_seconds=3600,
        ),
        records=create_valid_records(
            count=15,
        ),
    )

    run_scenario(
        engine,
        name="Senaryo 8 - Güvensiz kayıtları filtrele",
        trend=create_trend(
            classification="NORMAL_DRYING",
            sample_count=100,
            change_per_minute=-0.50,
            duration_seconds=3600,
        ),
        records=(
            create_valid_records(
                count=5,
            )
            + [
                create_record(
                    moisture_delta=8,
                    completed=False,
                ),
                create_record(
                    moisture_delta=8,
                    mode="MANUAL",
                ),
                create_record(
                    moisture_delta=-5,
                ),
                create_record(
                    moisture_delta=50,
                ),
            ]
        ),
    )


if __name__ == "__main__":
    main()