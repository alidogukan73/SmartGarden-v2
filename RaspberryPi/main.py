"""
SmartGarden application entry point.
"""

from __future__ import annotations

import time

from core.config import AppConfig
from core.logger import AppLogger
from services.irrigation_service import IrrigationService


def main() -> None:
    """
    Run SmartGarden.
    """

    logger = AppLogger().logger

    logger.info(
        "SmartGarden started.",
    )

    service = IrrigationService()

    try:

        service.initialize()

        while True:

            service.update()

            time.sleep(
                AppConfig.LOOP_DELAY_SECONDS,
            )

    except KeyboardInterrupt:

        logger.info(
            "Stopping SmartGarden...",
        )

    except Exception as exc:

        logger.exception(exc)

    finally:

        service.cleanup()

        logger.info(
            "SmartGarden stopped.",
        )


if __name__ == "__main__":
    main()