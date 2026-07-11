"""
System monitor.
"""

import shutil
import socket
import subprocess
import time

import psutil

from models.health_status import HealthStatus


class SystemMonitor:
    """
    Reads Raspberry Pi system information.
    """
    def get_health_status(
        self,
    ) -> HealthStatus:
        """
        Read Raspberry Pi health information.
        """

        with open(
            "/sys/class/thermal/thermal_zone0/temp",
            "r",
        ) as file:

            cpu_temperature = (
                int(file.read())
                / 1000
            )

        cpu_usage = psutil.cpu_percent(
            interval=None,
        )

        memory_usage = (
            psutil.virtual_memory().percent
        )

        disk = shutil.disk_usage(
            "/",
        )

        disk_usage = (
            disk.used
            / disk.total
        ) * 100

        uptime_seconds = int(
            time.time()
            - psutil.boot_time()
        )

        return HealthStatus(
            cpu_temperature=round(
                cpu_temperature,
                1,
            ),

            cpu_usage=round(
                cpu_usage,
                1,
            ),

            memory_usage=round(
                memory_usage,
                1,
            ),

            disk_usage=round(
                disk_usage,
                1,
            ),

            uptime_seconds=uptime_seconds,

            ip_address=self._get_ip_address(),

            wifi_signal=self._get_wifi_signal(),

            is_throttled=self._is_throttled(),
        )
    
    def _get_ip_address(
        self,
    ) -> str:
        """
        Return active network IP address.
        """

        sock = socket.socket(
            socket.AF_INET,
            socket.SOCK_DGRAM,
        )

        try:

            sock.connect(
                (
                    "8.8.8.8",
                    80,
                ),
            )

            return sock.getsockname()[0]

        except OSError:

            return ""

        finally:

            sock.close()
            
    def _get_wifi_signal(
        self,
    ) -> int:
        """
        Return Wi-Fi signal strength (dBm).
        """

        try:

            output = subprocess.check_output(
                [
                    "iwconfig",
                    "wlan0",
                ],
                text=True,
            )

            for line in output.splitlines():

                if "Signal level=" in line:

                    value = (
                        line.split(
                            "Signal level=",
                        )[1]
                        .split(
                            " ",
                        )[0]
                    )

                    return int(
                        value,
                    )

        except Exception:

            pass

        return -100


    def _is_throttled(
        self,
    ) -> bool:
        """
        Return Raspberry Pi throttling state.
        """

        try:

            output = subprocess.check_output(
                [
                    "vcgencmd",
                    "get_throttled",
                ],
                text=True,
            )

            value = int(
                output.strip().split("=")[1],
                16,
            )

            return value != 0

        except Exception:

            return False

    def read(
        self,
    ) -> HealthStatus:
        """
        Backward compatibility.
        """

        return self.get_health_status()