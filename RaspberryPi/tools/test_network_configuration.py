"""Regression checks for safe Raspberry Pi network configuration."""

from __future__ import annotations

import os
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from core.network_configuration import (  # noqa: E402
    MODE_DHCP,
    MODE_STATIC,
    NetworkConfigurationRequest,
    NetworkConfigurationService,
)


class FakeRunner:
    def __init__(self) -> None:
        self.calls: list[list[str]] = []

    def __call__(self, arguments, **kwargs):
        del kwargs
        self.calls.append(list(arguments))
        if arguments[:5] == ["ip", "-4", "route", "show", "default"]:
            output = "default via 192.168.1.1 dev wlan0 proto dhcp\n"
        elif "GENERAL.CONNECTION" in arguments:
            output = "AVORA WiFi\n"
        elif arguments[:4] == ["ip", "-4", "-o", "addr"]:
            output = "2: wlan0 inet 192.168.1.20/24 brd 192.168.1.255 scope global wlan0\n"
        elif "ipv4.method" in arguments:
            output = "auto\n"
        elif "IP4.DNS" in arguments:
            output = "192.168.1.1\n1.1.1.1\n"
        else:
            output = ""
        return subprocess.CompletedProcess(arguments, 0, stdout=output, stderr="")


def helper_file() -> str:
    handle, path = tempfile.mkstemp(prefix="avora-network-helper-")
    os.close(handle)
    return path


def test_static_success() -> None:
    runner = FakeRunner()
    helper = helper_file()
    try:
        service = NetworkConfigurationService(
            helper_path=helper,
            runner=runner,
            connectivity_probe=lambda: True,
            sleeper=lambda seconds: None,
        )
        service.is_supported = lambda: True
        service.read_status = lambda: {
            "supported": True,
            "interface": "wlan0",
            "mode": MODE_STATIC,
            "ip_address": "192.168.1.50",
            "prefix_length": 24,
            "gateway": "192.168.1.1",
            "primary_dns": "1.1.1.1",
            "secondary_dns": "8.8.8.8",
        }
        stages = []
        request = NetworkConfigurationRequest(
            "123e4567-e89b-12d3-a456-426614174000",
            "wlan0",
            MODE_STATIC,
            "192.168.1.50",
            24,
            "192.168.1.1",
            "1.1.1.1",
            "8.8.8.8",
        )
        outcome = service.apply(request, lambda status, message: stages.append(status))
        assert outcome.status == "SUCCESS"
        assert stages == ["VALIDATING", "APPLYING"]
        apply_call = next(call for call in runner.calls if "apply" in call)
        assert "192.168.1.50" in apply_call
        assert not any("shell" in str(call).lower() for call in runner.calls)
        assert any("confirm" in call for call in runner.calls)
    finally:
        os.unlink(helper)


def test_failed_probe_rolls_back() -> None:
    runner = FakeRunner()
    helper = helper_file()
    clock = [0.0]

    def monotonic():
        return clock[0]

    def sleep(seconds):
        clock[0] += seconds

    try:
        service = NetworkConfigurationService(
            helper_path=helper,
            runner=runner,
            connectivity_probe=lambda: False,
            sleeper=sleep,
            monotonic=monotonic,
        )
        service.is_supported = lambda: True
        outcome = service.apply(
            NetworkConfigurationRequest(
                "123e4567-e89b-12d3-a456-426614174001",
                "wlan0",
                MODE_DHCP,
            )
        )
        assert outcome.status == "ROLLED_BACK"
        assert any("rollback" in call for call in runner.calls)
    finally:
        os.unlink(helper)


def test_invalid_interface_never_reaches_helper() -> None:
    runner = FakeRunner()
    service = NetworkConfigurationService(
        helper_path=helper_file(),
        runner=runner,
    )
    try:
        outcome = service.apply(
            NetworkConfigurationRequest(
                "123e4567-e89b-12d3-a456-426614174002",
                "wlan0;shutdown",
                MODE_DHCP,
            )
        )
        assert outcome.status == "INVALID_REQUEST"
        assert runner.calls == []
    finally:
        os.unlink(service._helper_path)


def test_safety_rollback_cannot_be_reported_as_success() -> None:
    runner = FakeRunner()
    helper = helper_file()
    requested = {
        "supported": True,
        "interface": "wlan0",
        "mode": MODE_STATIC,
        "ip_address": "192.168.1.50",
        "prefix_length": 24,
        "gateway": "192.168.1.1",
        "primary_dns": "1.1.1.1",
        "secondary_dns": "",
    }
    rolled_back = {
        **requested,
        "ip_address": "192.168.1.20",
    }
    statuses = iter((requested, rolled_back))
    try:
        service = NetworkConfigurationService(
            helper_path=helper,
            runner=runner,
            connectivity_probe=lambda: True,
            sleeper=lambda seconds: None,
        )
        service.is_supported = lambda: True
        service.read_status = lambda: next(statuses)
        outcome = service.apply(
            NetworkConfigurationRequest(
                "123e4567-e89b-12d3-a456-426614174003",
                "wlan0",
                MODE_STATIC,
                "192.168.1.50",
                24,
                "192.168.1.1",
                "1.1.1.1",
            )
        )
        assert outcome.status == "ROLLED_BACK"
        assert any("rollback" in call for call in runner.calls)
        assert not any("confirm" in call for call in runner.calls)
    finally:
        os.unlink(helper)


def test_status_is_read_only() -> None:
    runner = FakeRunner()
    service = NetworkConfigurationService(runner=runner)
    service.is_supported = lambda: True
    status = service.read_status()
    assert status["interface"] == "wlan0"
    assert status["mode"] == MODE_DHCP
    assert status["subnet_mask"] == "255.255.255.0"
    assert status["gateway"] == "192.168.1.1"
    assert not any("modify" in call or "apply" in call for call in runner.calls)


if __name__ == "__main__":
    test_static_success()
    test_failed_probe_rolls_back()
    test_invalid_interface_never_reaches_helper()
    test_safety_rollback_cannot_be_reported_as_success()
    test_status_is_read_only()
    print("[PASS] Raspberry Pi network configuration tests.")
