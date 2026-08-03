"""
Test calibrated soil moisture sensor.
"""

from __future__ import annotations

import sys
import time
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(
        0,
        str(PROJECT_ROOT),
    )


from hardware.sensor import SoilMoistureSensor


def main() -> None:
    sensor = SoilMoistureSensor()
    sensor.initialize()

    print("Calibrated soil sensor test started.")
    print("Press Ctrl+C to stop.\n")

    try:
        while True:
            reading = sensor.read()

            print(
                f"Raw={reading.raw} "
                f"Voltage={reading.voltage:.3f} V "
                f"Moisture={reading.moisture}%"
            )

            time.sleep(2)

    except KeyboardInterrupt:
        print("\nTest stopped.")


if __name__ == "__main__":
    main()