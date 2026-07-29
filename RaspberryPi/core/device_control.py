"""
Device-level system operations.
"""

from __future__ import annotations

import subprocess
import time

from core.logger import AppLogger


class DeviceControl:
    """
    Performs controlled Raspberry Pi system operations.
    """

    def __init__(self) -> None:

        self._logger = AppLogger().logger

    def restart_device(self) -> None:
        """
        Restart the Raspberry Pi safely.
        """

        self._logger.warning(
            "Raspberry Pi will restart in 3 seconds.",
        )

        time.sleep(3)

        try:

            subprocess.run(
                [
                    "sudo",
                    "-n",
                    "/usr/bin/systemctl",
                    "reboot",
                ],
                check=True,
                timeout=10,
            )

        except subprocess.CalledProcessError as exc:

            self._logger.exception(
                "Raspberry Pi restart command failed: %s",
                exc,
            )

            raise

        except FileNotFoundError as exc:

            self._logger.exception(
                "systemctl or sudo command was not found: %s",
                exc,
            )

            raise

        except subprocess.TimeoutExpired as exc:

            self._logger.exception(
                "Raspberry Pi restart command timed out: %s",
                exc,
            )

            raise
