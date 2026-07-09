"""
Application logging.
"""

from __future__ import annotations

import logging
from pathlib import Path

from core.config import LogConfig


class AppLogger:
    """
    Application logger.
    """

    _instance: logging.Logger | None = None

    def __init__(self) -> None:

        if AppLogger._instance is None:

            logger = logging.getLogger(
                "SmartGarden",
            )

            logger.setLevel(
                getattr(logging, LogConfig.LEVEL),
            )

            formatter = logging.Formatter(
                fmt=LogConfig.FORMAT,
                datefmt=LogConfig.DATE_FORMAT,
            )

            log_path = Path(
                LogConfig.LOG_FILE,
            )

            log_path.parent.mkdir(
                parents=True,
                exist_ok=True,
            )

            file_handler = logging.FileHandler(
                log_path,
                encoding="utf-8",
            )

            file_handler.setFormatter(
                formatter,
            )

            console_handler = logging.StreamHandler()

            console_handler.setFormatter(
                formatter,
            )

            logger.addHandler(
                file_handler,
            )

            logger.addHandler(
                console_handler,
            )

            AppLogger._instance = logger

        self._logger = AppLogger._instance

    @property
    def logger(self) -> logging.Logger:
        """
        Return logger.
        """

        return self._logger