"""
Capacitive soil moisture sensor hardware test.

Reads ADS1115 channel A0 and prints raw/voltage values.
"""

from __future__ import annotations

import time

import board

from adafruit_ads1x15 import ADS1115, AnalogIn, ads1x15


def main() -> None:
    print("Capacitive sensor test started.")
    print("Press Ctrl+C to stop.\n")

    i2c = board.I2C()

    ads = ADS1115(
        i2c
    )

    channel = AnalogIn(
        ads,
        ads1x15.Pin.A0,
    )

    try:
        while True:

            print(
                f"Raw={channel.value} "
                f"Voltage={channel.voltage:.3f} V"
            )

            time.sleep(2)

    except KeyboardInterrupt:
        print("\nTest stopped.")


if __name__ == "__main__":
    main()