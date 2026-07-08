"""
SmartGarden v2 entry point.
"""

from __future__ import annotations

import time

from core.config import SensorConfig
from core.logger import AppLogger
from RaspberryPi.services.irrigation_service import IrrigationService


def main() -> None:
    """
    Application entry point.
    """

    logger = AppLogger().logger

    logger.info("SmartGarden started.")

    service = IrrigationService()

    service.initialize()

    try:
        while True:
            service.update()

            time.sleep(
                SensorConfig.READ_INTERVAL_SECONDS,
            )

    except KeyboardInterrupt:
        logger.info("Shutdown requested by user.")

    finally:
        service.cleanup()

        logger.info("SmartGarden stopped.")


if __name__ == "__main__":
    main()
