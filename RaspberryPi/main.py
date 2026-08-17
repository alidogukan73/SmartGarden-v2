"""
AVORA application entry point.
"""

from __future__ import annotations

import signal
import time

from core.config import AppConfig
from core.logger import AppLogger
from services.irrigation_service import IrrigationService


class ShutdownRequested(BaseException):
    """
    Raised when the operating system requests a clean stop.

    BaseException prevents service-level ``except Exception``
    handlers from mistaking shutdown for a recoverable cycle
    error.
    """


def _request_shutdown(
    signal_number: int,
    frame,
) -> None:
    """
    Convert SIGTERM into the normal application cleanup path.
    """

    del signal_number
    del frame

    raise ShutdownRequested


def main() -> None:
    """
    Run AVORA.
    """

    logger = AppLogger().logger

    logger.info(
        "AVORA started.",
    )

    service = IrrigationService()

    signal.signal(
        signal.SIGTERM,
        _request_shutdown,
    )

    try:

        service.initialize()

        while True:

            service.update()

            time.sleep(
                AppConfig.LOOP_DELAY_SECONDS,
            )

    except (
        KeyboardInterrupt,
        ShutdownRequested,
    ):

        logger.info(
            "Stopping AVORA...",
        )

    finally:

        service.cleanup()

        logger.info(
            "AVORA stopped.",
        )


if __name__ == "__main__":
    main()
