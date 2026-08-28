"""
Offline actuator command freshness policy tests.
"""

from __future__ import annotations

import time

from tools.hardware_test_stubs import install_hardware_import_stubs

install_hardware_import_stubs()

from services.irrigation_service import IrrigationService


def main() -> None:
    now_ms = int(time.time() * 1000)

    assert IrrigationService._is_recent_command(now_ms)
    assert IrrigationService._is_recent_command(
        now_ms - 29_000,
    )
    assert not IrrigationService._is_recent_command(
        now_ms - 31_000,
    )
    assert not IrrigationService._is_recent_command(0)
    assert not IrrigationService._is_recent_command(
        now_ms + 301_000,
    )

    print(
        "[PASS] Offline actuator command freshness policy.",
    )


if __name__ == "__main__":
    main()
