"""Import-only stubs for service tests running away from Raspberry Pi hardware."""

from __future__ import annotations

import sys
import types


def install_hardware_import_stubs() -> None:
    try:
        import RPi.GPIO  # noqa: F401
    except ModuleNotFoundError:
        rpi = types.ModuleType("RPi")
        gpio = types.ModuleType("RPi.GPIO")
        gpio.BCM = 11
        gpio.OUT = 1
        gpio.HIGH = 1
        gpio.LOW = 0
        gpio.setmode = lambda _mode: None
        gpio.setwarnings = lambda _enabled: None
        gpio.setup = lambda *_args, **_kwargs: None
        gpio.output = lambda *_args, **_kwargs: None
        gpio.cleanup = lambda *_args, **_kwargs: None
        rpi.GPIO = gpio
        sys.modules["RPi"] = rpi
        sys.modules["RPi.GPIO"] = gpio

    try:
        import board  # noqa: F401
        import busio  # noqa: F401
        import adafruit_ads1x15  # noqa: F401
        return
    except ModuleNotFoundError:
        pass

    board = types.ModuleType("board")
    board.SCL = object()
    board.SDA = object()
    busio = types.ModuleType("busio")
    busio.I2C = object
    adafruit = types.ModuleType("adafruit_ads1x15")
    ads1x15 = types.ModuleType("adafruit_ads1x15.ads1x15")
    ads1115 = types.ModuleType("adafruit_ads1x15.ads1115")
    analog_in = types.ModuleType("adafruit_ads1x15.analog_in")
    ads1115.ADS1115 = object
    analog_in.AnalogIn = object
    adafruit.ads1x15 = ads1x15
    sys.modules["board"] = board
    sys.modules["busio"] = busio
    sys.modules["adafruit_ads1x15"] = adafruit
    sys.modules["adafruit_ads1x15.ads1x15"] = ads1x15
    sys.modules["adafruit_ads1x15.ads1115"] = ads1115
    sys.modules["adafruit_ads1x15.analog_in"] = analog_in
