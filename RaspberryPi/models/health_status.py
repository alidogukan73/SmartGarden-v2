"""
Device health status.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class HealthStatus:
    """
    Raspberry Pi health information.
    """

    cpu_temperature: float

    cpu_usage: float

    memory_usage: float

    disk_usage: float

    uptime_seconds: int

    ip_address: str

    wifi_signal: int

    # Genel aktif sorun durumu
    is_throttled: bool

    # Ham get_throttled değeri
    throttled_raw: int

    # Şu an aktif durumlar
    under_voltage_now: bool

    frequency_capped_now: bool

    throttled_now: bool

    soft_temperature_limit_now: bool

    # Geçmişte en az bir kez oluşmuş durumlar
    under_voltage_history: bool

    frequency_capped_history: bool

    throttled_history: bool

    soft_temperature_limit_history: bool