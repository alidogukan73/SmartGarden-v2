"""
Manual test for MoistureTrendAnalyzer.
"""

from controllers.moisture_trend_analyzer import (
    MoistureTrendAnalyzer,
)
from models.moisture_history import (
    MoistureHistory,
)


def run_scenario(
    name: str,
    samples: list[tuple[int, float]],
) -> None:
    """
    Run one moisture trend scenario.
    """

    print()
    print("=" * 72)
    print(name)
    print("=" * 72)

    history = MoistureHistory(
        max_samples=20,
    )

    analyzer = MoistureTrendAnalyzer(
        history=history,
    )

    trend = analyzer.analyze()

    for index, (
        moisture,
        timestamp,
    ) in enumerate(
        samples,
        start=1,
    ):

        history.add(
            moisture,
            timestamp=timestamp,
        )

        trend = analyzer.analyze()

        print(
            f"{index}. ölçüm={moisture}% "
            f"sınıf={trend.classification} "
            f"değişim={trend.total_change:+d} "
            f"hız={trend.change_per_minute:+.3f}/dk "
            f"kararlı={trend.is_stable}"
        )

    print()
    print("Sonuç:")

    print(
        f"  classification   = "
        f"{trend.classification}"
    )

    print(
        f"  sample_count     = "
        f"{trend.sample_count}"
    )

    print(
        f"  first_moisture   = "
        f"{trend.first_moisture}%"
    )

    print(
        f"  latest_moisture  = "
        f"{trend.latest_moisture}%"
    )

    print(
        f"  minimum_moisture = "
        f"{trend.minimum_moisture}%"
    )

    print(
        f"  maximum_moisture = "
        f"{trend.maximum_moisture}%"
    )

    print(
        f"  average_moisture = "
        f"{trend.average_moisture}%"
    )

    print(
        f"  total_change     = "
        f"{trend.total_change:+d}"
    )

    print(
        f"  change_per_min   = "
        f"{trend.change_per_minute:+.3f}"
    )

    print(
        f"  duration_seconds = "
        f"{trend.duration_seconds}"
    )

    print(
        f"  is_stable        = "
        f"{trend.is_stable}"
    )


def main() -> None:
    """
    Run all trend scenarios.
    """

    run_scenario(
        "Senaryo 1 - Kararlı nem",
        [
            (40, 0),
            (41, 90),
            (40, 180),
            (39, 270),
            (40, 360),
        ]
    )

    run_scenario(
        "Senaryo 2 - Nem yükseliyor",
        [
            (30, 0),
            (31, 90),
            (32, 180),
            (33, 270),
            (34, 360),
        ],
    )

    run_scenario(
        "Senaryo 3 - Yavaş kuruma",
        [
            (40, 0),
            (40, 120),
            (39, 240),
            (39, 360),
            (39, 480),
        ],
    )

    run_scenario(
        "Senaryo 4 - Normal kuruma",
        [
            (40, 0),
            (39, 90),
            (38, 180),
            (38, 270),
            (37, 360),
        ],
    )

    run_scenario(
        "Senaryo 5 - Hızlı kuruma",
        [
            (40, 0),
            (39, 75),
            (38, 150),
            (37, 225),
            (36, 300),
        ],
    )

    run_scenario(
        "Senaryo 6 - Çok hızlı kuruma",
        [
            (40, 0),
            (38, 75),
            (36, 150),
            (34, 225),
            (32, 300),
        ],
    )


if __name__ == "__main__":
    main()