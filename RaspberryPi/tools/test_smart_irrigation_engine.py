"""
Manual test for SmartIrrigationEngine.
"""

from controllers.smart_irrigation_engine import SmartIrrigationEngine
from models.command_state import CommandState
from models.sensor_reading import SensorReading


def create_commands() -> CommandState:
    """
    Return test command values.
    """

    return CommandState(
        auto_mode=True,
        relay=False,
        enabled=True,
        moisture_limit=40,
        pump_duration=120,
        restart_delta=10,
        cooldown_seconds=600,
    )


def create_reading(
    moisture: int,
) -> SensorReading:
    """
    Return a test sensor reading.
    """

    return SensorReading(
        raw=0,
        voltage=0.0,
        moisture=moisture,
    )


def run_scenario(
    name: str,
    samples: list[int],
    *,
    cooldown_active: bool = False,
) -> None:
    """
    Run one decision scenario.
    """

    print()
    print("=" * 60)
    print(name)
    print("=" * 60)

    engine = SmartIrrigationEngine()
    commands = create_commands()

    for index, moisture in enumerate(
        samples,
        start=1,
    ):

        decision = engine.evaluate(
            reading=create_reading(
                moisture,
            ),
            commands=commands,
            cooldown_active=cooldown_active,
        )

        print(
            f"{index}. ölçüm={moisture}% "
            f"karar={decision.should_water} "
            f"neden={decision.reason} "
            f"kararlı={decision.sensor_stable}"
        )


def main() -> None:
    """
    Run manual engine tests.
    """

    run_scenario(
        "Senaryo 1 - Nem yeterli ve sensör kararlı",
        [
            52,
            51,
            52,
            50,
            51,
        ],
    )

    run_scenario(
        "Senaryo 2 - Nem düşük ve sensör kararlı",
        [
            32,
            31,
            32,
            30,
            31,
        ],
    )

    run_scenario(
        "Senaryo 3 - Sensör kararsız",
        [
            32,
            55,
            21,
            48,
            29,
        ],
    )

    run_scenario(
        "Senaryo 4 - Nem düşük ama cooldown aktif",
        [
            29,
            30,
            29,
            28,
            30,
        ],
        cooldown_active=True,
    )


if __name__ == "__main__":
    main()