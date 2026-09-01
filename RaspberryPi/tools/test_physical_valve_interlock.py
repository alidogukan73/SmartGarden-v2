"""Verify only installed valves may start the shared pump."""

from __future__ import annotations

import sys
import time
import types
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

try:
    import RPi.GPIO  # noqa: F401
except ModuleNotFoundError:
    rpi = types.ModuleType("RPi")
    gpio = types.ModuleType("RPi.GPIO")
    rpi.GPIO = gpio
    sys.modules["RPi"] = rpi
    sys.modules["RPi.GPIO"] = gpio

from core.config import ValveConfig
from hardware.valve_controller import ValveController


def main() -> None:
    valves = ValveController()

    assert valves.is_physical_valve("valve-001") is True
    assert valves.is_simulated_valve("valve-001") is False
    valves._active_valve_id = "valve-001"
    valves._active_valve_opened_at = time.monotonic()
    assert valves.is_ready_for_pump("valve-001") is False
    valves._active_valve_opened_at -= (
        ValveConfig.OPENING_DELAY_SECONDS + 0.1
    )
    assert valves.is_ready_for_pump("valve-001") is True
    assert valves.is_ready_for_pump("valve-002") is False

    for valve_id in (
        "valve-002",
        "valve-003",
        "valve-004",
        "valve-005",
        "valve-006",
        "valve-007",
        "valve-008",
    ):
        assert valve_id in ValveConfig.GPIO_PINS
        assert valves.is_simulated_valve(valve_id) is True

    print("[PASS] Only installed valve-001 can enable the pump.")


if __name__ == "__main__":
    main()
