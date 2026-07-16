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

        throttling_status = (
            self._read_throttling_status()
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

            is_throttled=bool(
                throttling_status[
                    "is_throttled"
                ],
            ),

            throttled_raw=int(
                throttling_status[
                    "throttled_raw"
                ],
            ),

            under_voltage_now=bool(
                throttling_status[
                    "under_voltage_now"
                ],
            ),

            frequency_capped_now=bool(
                throttling_status[
                    "frequency_capped_now"
                ],
            ),

            throttled_now=bool(
                throttling_status[
                    "throttled_now"
                ],
            ),

            soft_temperature_limit_now=bool(
                throttling_status[
                    "soft_temperature_limit_now"
                ],
            ),

            under_voltage_history=bool(
                throttling_status[
                    "under_voltage_history"
                ],
            ),

            frequency_capped_history=bool(
                throttling_status[
                    "frequency_capped_history"
                ],
            ),

            throttled_history=bool(
                throttling_status[
                    "throttled_history"
                ],
            ),

            soft_temperature_limit_history=bool(
                throttling_status[
                    "soft_temperature_limit_history"
                ],
            ),            
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

    def _read_throttling_status(
        self,
    ) -> dict[str, int | bool]:
        """
        Read and decode Raspberry Pi throttling flags.

        Current-state flags:
            Bit 0  -> Under-voltage detected
            Bit 1  -> ARM frequency capped
            Bit 2  -> Currently throttled
            Bit 3  -> Soft temperature limit active

        Historical flags:
            Bit 16 -> Under-voltage has occurred
            Bit 17 -> ARM frequency capping has occurred
            Bit 18 -> Throttling has occurred
            Bit 19 -> Soft temperature limit has occurred
        """

        try:

            output = subprocess.check_output(
                [
                    "vcgencmd",
                    "get_throttled",
                ],
                text=True,
                timeout=3,
            )

            value_text = (
                output
                .strip()
                .split(
                    "=",
                    maxsplit=1,
                )[1]
            )

            value = int(
                value_text,
                16,
            )

        except (
            IndexError,
            OSError,
            subprocess.SubprocessError,
            ValueError,
        ):

            value = 0

        under_voltage_now = bool(
            value & (1 << 0)
        )

        frequency_capped_now = bool(
            value & (1 << 1)
        )

        throttled_now = bool(
            value & (1 << 2)
        )

        soft_temperature_limit_now = bool(
            value & (1 << 3)
        )

        under_voltage_history = bool(
            value & (1 << 16)
        )

        frequency_capped_history = bool(
            value & (1 << 17)
        )

        throttled_history = bool(
            value & (1 << 18)
        )

        soft_temperature_limit_history = bool(
            value & (1 << 19)
        )

        is_throttled = any(
            (
                under_voltage_now,
                frequency_capped_now,
                throttled_now,
                soft_temperature_limit_now,
            ),
        )

        return {
            "is_throttled":
                is_throttled,

            "throttled_raw":
                value,

            "under_voltage_now":
                under_voltage_now,

            "frequency_capped_now":
                frequency_capped_now,

            "throttled_now":
                throttled_now,

            "soft_temperature_limit_now":
                soft_temperature_limit_now,

            "under_voltage_history":
                under_voltage_history,

            "frequency_capped_history":
                frequency_capped_history,

            "throttled_history":
                throttled_history,

            "soft_temperature_limit_history":
                soft_temperature_limit_history,
        }    

    def read(
        self,
    ) -> HealthStatus:
        """
        Backward compatibility.
        """

        return self.get_health_status()