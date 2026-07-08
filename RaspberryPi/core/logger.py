"""
SmartGarden v2

Logging module.
"""

from __future__ import annotations

import logging
from pathlib import Path

from core.config import LogConfig


class AppLogger:
    """Application logger."""

    def __init__(self) -> None:
        self._logger = logging.getLogger("SmartGarden")
        self._logger.setLevel(LogConfig.LOG_LEVEL)

        if not self._logger.handlers:
            Path(LogConfig.LOG_FOLDER).mkdir(
                parents=True,
                exist_ok=True,
            )

            formatter = logging.Formatter(
                fmt="%(asctime)s | %(levelname)-8s | %(message)s",
                datefmt="%Y-%m-%d %H:%M:%S",
            )

            file_handler = logging.FileHandler(
                LogConfig.LOG_FILE,
                encoding="utf-8",
            )
            file_handler.setFormatter(formatter)

            console_handler = logging.StreamHandler()
            console_handler.setFormatter(formatter)

            self._logger.addHandler(file_handler)
            self._logger.addHandler(console_handler)

    @property
    def logger(self) -> logging.Logger:
        """Return configured logger."""

        return self._logger
