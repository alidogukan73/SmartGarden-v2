"""
Verify the safe zone-valve simulation sequence.
"""

from __future__ import annotations

import sys
import types
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

try:
    import RPi.GPIO  # noqa: F401
except ModuleNotFoundError:
    rpi_module = types.ModuleType("RPi")
    gpio_module = types.ModuleType("RPi.GPIO")
    rpi_module.GPIO = gpio_module
    sys.modules["RPi"] = rpi_module
    sys.modules["RPi.GPIO"] = gpio_module

from controllers.watering_controller import WateringController
from hardware.valve_controller import ValveController
from models.command_state import CommandState


class FakePumpRelay:
    def __init__(self) -> None:
        self.is_on = False
        self.on_count = 0

    def on(self) -> None:
        self.is_on = True
        self.on_count += 1

    def off(self) -> None:
        self.is_on = False


def main() -> None:
    pump = FakePumpRelay()
    valves = ValveController()
    valves.initialize()

    controller = WateringController(
        pump,
        valves,
    )

    valve_events: list[tuple[str | None, bool]] = []

    result = controller.water_zone(
        valve_id="valve-001",
        duration=10,
        get_commands=lambda: CommandState(),
        on_valve_changed=lambda valve_id, is_open: (
            valve_events.append((valve_id, is_open))
        ),
    )

    assert result.completed is False
    assert result.stop_reason == "VALVE_SIMULATION"
    assert pump.on_count == 0
    assert pump.is_on is False
    assert valves.active_valve_id is None
    assert valve_events == [
        ("valve-001", True),
        (None, False),
    ]

    valves.cleanup()

    print(
        "[PASS] Simulated valve sequence kept the real pump OFF.",
    )


if __name__ == "__main__":
    main()
