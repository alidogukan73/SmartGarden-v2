"""Donanım kullanmadan ağ komutu/sulama güvenliği entegrasyon testi."""

from __future__ import annotations

from pathlib import Path
from types import SimpleNamespace
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.hardware_test_stubs import install_hardware_import_stubs

install_hardware_import_stubs()

from core.firebase_service import FirebaseService  # noqa: E402
from core.network_configuration import NetworkConfigurationOutcome  # noqa: E402
from models.command_state import CommandState  # noqa: E402
from services.irrigation_service import IrrigationService  # noqa: E402


class FakeFirebase:
    def __init__(self) -> None:
        self.acknowledged: list[str] = []
        self.results: list[dict] = []
        self.network_status: list[dict] = []
        self.completions: list[dict] = []

    def acknowledge_network_configuration_request(self, identifier: str) -> None:
        self.acknowledged.append(identifier)

    def update_network_configuration_result(self, **value) -> None:
        self.results.append(value)

    def update_network_status(self, value: dict) -> None:
        self.network_status.append(value)

    def publish_network_configuration_completion(self, **value) -> None:
        self.completions.append(value)
        self.results.append(value)
        self.network_status.append(value["network_status"])


class FakeNetwork:
    def __init__(self) -> None:
        self.requests = []

    def apply(self, request, callback):
        self.requests.append(request)
        callback("VALIDATING", "ok")
        callback("APPLYING", "ok")
        callback("VERIFYING", "ok")
        return NetworkConfigurationOutcome(
            "SUCCESS",
            "verified",
            "192.168.1.50",
        )

    @staticmethod
    def read_status() -> dict:
        return {"supported": True, "ip_address": "192.168.1.50"}


class FakeLogger:
    def info(self, *_args, **_kwargs): pass
    def warning(self, *_args, **_kwargs): pass
    def debug(self, *_args, **_kwargs): pass
    def exception(self, *_args, **_kwargs): pass


def command(*, requested_at: int | None = None) -> CommandState:
    now = int(time.time() * 1000)
    return CommandState(
        network_configuration_requested=True,
        network_configuration_request_id=(
            "123e4567-e89b-12d3-a456-426614174020"
        ),
        network_configuration_interface="wlan0",
        network_configuration_mode="STATIC",
        network_configuration_ip_address="192.168.1.50",
        network_configuration_prefix_length=24,
        network_configuration_gateway="192.168.1.1",
        network_configuration_primary_dns="1.1.1.1",
        network_configuration_secondary_dns="8.8.8.8",
        network_configuration_requested_at_ms=(
            now if requested_at is None else requested_at
        ),
        network_configuration_expires_at_ms=now + 180_000,
        network_configuration_source="android",
    )


def service(*, relay_on: bool = False) -> IrrigationService:
    value = object.__new__(IrrigationService)
    value._firebase = FakeFirebase()
    value._network_configuration = FakeNetwork()
    value._relay = SimpleNamespace(is_on=relay_on)
    value._zone_executor = SimpleNamespace(active_zone_id=None)
    value._active_zone_test_request_id = ""
    value._last_network_configuration_request_id = ""
    value._logger = FakeLogger()
    return value


def test_successful_request_is_consumed_before_apply() -> None:
    value = service()
    handled = value._process_network_configuration_command(command())
    identifier = "123e4567-e89b-12d3-a456-426614174020"
    assert handled
    assert value._firebase.acknowledged == [identifier]
    assert len(value._network_configuration.requests) == 1
    assert [item["status"] for item in value._firebase.results] == [
        "VALIDATING", "APPLYING", "VERIFYING", "SUCCESS"
    ]
    assert len(value._firebase.completions) == 1
    assert value._firebase.completions[0]["applied_ip"] == "192.168.1.50"
    assert value._firebase.network_status[-1]["ip_address"] == "192.168.1.50"


def test_watering_blocks_network_change() -> None:
    value = service(relay_on=True)
    assert value._process_network_configuration_command(command())
    assert value._firebase.results[-1]["status"] == "WATERING_ACTIVE"
    assert value._network_configuration.requests == []


def test_stale_request_is_never_applied() -> None:
    value = service()
    old = int(time.time() * 1000) - 181_000
    assert value._process_network_configuration_command(command(requested_at=old))
    assert value._firebase.results[-1]["status"] == "STALE_COMMAND"
    assert value._network_configuration.requests == []


def test_firebase_command_parser_keeps_bounded_network_values() -> None:
    now = int(time.time() * 1000)
    commands = {
        "network_configuration": {
            "requested": True,
            "request_id": "123e4567-e89b-12d3-a456-426614174021",
            "interface": "wlan0",
            "mode": "STATIC",
            "ip_address": "192.168.1.60",
            "prefix_length": 24,
            "gateway": "192.168.1.1",
            "primary_dns": "1.1.1.1",
            "secondary_dns": "8.8.8.8",
            "requested_at": now,
            "expires_at": now + 180_000,
            "source": "android",
        }
    }

    class Reference:
        def child(self, _name): return self
        def get(self): return commands

    value = object.__new__(FirebaseService)
    value._logger = FakeLogger()
    value._device_ref = lambda: Reference()
    parsed = value.get_commands()

    assert parsed.network_configuration_requested
    assert parsed.network_configuration_interface == "wlan0"
    assert parsed.network_configuration_mode == "STATIC"
    assert parsed.network_configuration_ip_address == "192.168.1.60"
    assert parsed.network_configuration_prefix_length == 24
    assert parsed.network_configuration_source == "android"


if __name__ == "__main__":
    test_successful_request_is_consumed_before_apply()
    test_watering_blocks_network_change()
    test_stale_request_is_never_applied()
    test_firebase_command_parser_keeps_bounded_network_values()
    print("[PASS] Network command integration tests.")
