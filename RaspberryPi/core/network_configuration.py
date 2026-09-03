"""Read and safely apply Raspberry Pi IPv4 network configuration."""

from __future__ import annotations

from dataclasses import dataclass
import ipaddress
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import time
from typing import Callable, Sequence
from urllib.parse import urlparse

from core.config import FirebaseConfig


MODE_DHCP = "DHCP"
MODE_STATIC = "STATIC"
TERMINAL_RESULTS = frozenset(
    {"SUCCESS", "ROLLED_BACK", "FAILED", "INVALID_REQUEST", "UNSUPPORTED"}
)


@dataclass(frozen=True)
class NetworkConfigurationRequest:
    request_id: str
    interface: str
    mode: str
    ip_address: str = ""
    prefix_length: int = 24
    gateway: str = ""
    primary_dns: str = ""
    secondary_dns: str = ""


@dataclass(frozen=True)
class NetworkConfigurationOutcome:
    status: str
    message: str = ""
    applied_ip: str = ""


class NetworkConfigurationError(ValueError):
    """A bounded, user-originated network request is invalid."""


class NetworkConfigurationService:
    """Uses a root-owned helper; never executes user-provided shell text."""

    HELPER_DEFAULT = "/usr/local/sbin/avora-network-config"
    APPLY_TIMEOUT_SECONDS = 30
    VERIFY_TIMEOUT_SECONDS = 60
    PROBE_INTERVAL_SECONDS = 3

    def __init__(
        self,
        *,
        helper_path: str | None = None,
        runner: Callable[..., subprocess.CompletedProcess] | None = None,
        connectivity_probe: Callable[[], bool] | None = None,
        sleeper: Callable[[float], None] = time.sleep,
        monotonic: Callable[[], float] = time.monotonic,
    ) -> None:
        self._helper_path = helper_path or os.getenv(
            "AVORA_NETWORK_HELPER",
            self.HELPER_DEFAULT,
        )
        self._runner = runner or subprocess.run
        self._connectivity_probe = connectivity_probe or self._probe_firebase
        self._sleep = sleeper
        self._monotonic = monotonic

    def is_supported(self) -> bool:
        return bool(shutil.which("nmcli") and Path(self._helper_path).is_file())

    def read_status(self) -> dict:
        """Return the active default-route profile without changing the host."""
        result = {
            "supported": self.is_supported(),
            "interface": "",
            "connection_name": "",
            "mode": "",
            "ip_address": "",
            "prefix_length": 0,
            "subnet_mask": "",
            "gateway": "",
            "primary_dns": "",
            "secondary_dns": "",
            "tailscale_ip": self._tailscale_ip(),
        }
        try:
            route = self._run_read(["ip", "-4", "route", "show", "default"])
            interface = self._route_value(route, "dev")
            gateway = self._route_value(route, "via")
            if not interface:
                return result
            connection = self._run_read(
                ["nmcli", "-g", "GENERAL.CONNECTION", "device", "show", interface]
            ).strip()
            address_output = self._run_read(
                ["ip", "-4", "-o", "addr", "show", "dev", interface, "scope", "global"]
            )
            address, prefix = self._address_and_prefix(address_output)
            method = ""
            if connection and connection != "--":
                raw_method = self._run_read(
                    ["nmcli", "-g", "ipv4.method", "connection", "show", connection]
                ).strip().lower()
                method = MODE_STATIC if raw_method == "manual" else MODE_DHCP
            dns_values = [
                value.strip()
                for value in self._run_read(
                    ["nmcli", "-g", "IP4.DNS", "device", "show", interface]
                ).splitlines()
                if value.strip()
            ]
            result.update(
                {
                    "interface": interface,
                    "connection_name": "" if connection == "--" else connection,
                    "mode": method,
                    "ip_address": address,
                    "prefix_length": prefix,
                    "subnet_mask": self._subnet_mask(prefix),
                    "gateway": gateway,
                    "primary_dns": dns_values[0] if dns_values else "",
                    "secondary_dns": dns_values[1] if len(dns_values) > 1 else "",
                }
            )
        except (OSError, subprocess.SubprocessError, ValueError):
            result["supported"] = False
        return result

    def apply(
        self,
        request: NetworkConfigurationRequest,
        on_status: Callable[[str, str], None] | None = None,
    ) -> NetworkConfigurationOutcome:
        notify = on_status or (lambda status, message: None)
        try:
            normalized = self.validate(request)
        except NetworkConfigurationError as exc:
            return NetworkConfigurationOutcome("INVALID_REQUEST", str(exc))
        if not self.is_supported():
            return NetworkConfigurationOutcome(
                "UNSUPPORTED",
                "NetworkManager helper is not installed.",
            )

        notify("VALIDATING", "Network values validated.")
        arguments = [
            *self._helper_command(),
            "apply",
            "--request-id", normalized.request_id,
            "--interface", normalized.interface,
            "--mode", normalized.mode.lower(),
        ]
        if normalized.mode == MODE_STATIC:
            arguments.extend(
                [
                    "--address", normalized.ip_address,
                    "--prefix", str(normalized.prefix_length),
                    "--gateway", normalized.gateway,
                    "--primary-dns", normalized.primary_dns,
                ]
            )
            if normalized.secondary_dns:
                arguments.extend(["--secondary-dns", normalized.secondary_dns])

        try:
            notify("APPLYING", "Applying NetworkManager profile.")
            self._run(arguments, timeout=self.APPLY_TIMEOUT_SECONDS)
            status = self.read_status()
            if not self._matches_request(status, normalized):
                self._rollback(normalized.request_id)
                return NetworkConfigurationOutcome(
                    "ROLLED_BACK",
                    "Active network values did not match the requested configuration.",
                )
            if not self._wait_for_connectivity():
                self._rollback(normalized.request_id)
                return NetworkConfigurationOutcome(
                    "ROLLED_BACK",
                    "Firebase connection did not recover before the safety timeout.",
                )
            status = self.read_status()
            if not self._matches_request(status, normalized):
                self._rollback(normalized.request_id)
                return NetworkConfigurationOutcome(
                    "ROLLED_BACK",
                    "Requested network values were no longer active after verification.",
                )
            self._run(
                [*self._helper_command(), "confirm", "--request-id", normalized.request_id],
                timeout=10,
            )
            return NetworkConfigurationOutcome(
                "SUCCESS",
                "Network configuration applied and verified.",
                str(status.get("ip_address", "")),
            )
        except (OSError, subprocess.SubprocessError) as exc:
            try:
                self._rollback(normalized.request_id)
                return NetworkConfigurationOutcome("ROLLED_BACK", str(exc)[:300])
            except (OSError, subprocess.SubprocessError) as rollback_error:
                return NetworkConfigurationOutcome(
                    "FAILED",
                    f"apply={exc}; rollback={rollback_error}"[:300],
                )

    def validate(self, request: NetworkConfigurationRequest) -> NetworkConfigurationRequest:
        request_id = str(request.request_id or "").strip().lower()
        if not re.fullmatch(r"[0-9a-f]{8}-[0-9a-f-]{27,40}", request_id):
            raise NetworkConfigurationError("Invalid request identifier.")
        interface = str(request.interface or "").strip()
        if interface == "lo" or not re.fullmatch(r"[a-zA-Z0-9_.:-]{1,32}", interface):
            raise NetworkConfigurationError("Invalid network interface.")
        mode = str(request.mode or "").strip().upper()
        if mode not in {MODE_DHCP, MODE_STATIC}:
            raise NetworkConfigurationError("Invalid IPv4 mode.")
        if mode == MODE_DHCP:
            return NetworkConfigurationRequest(request_id, interface, MODE_DHCP)

        try:
            address = ipaddress.IPv4Address(str(request.ip_address).strip())
            gateway = ipaddress.IPv4Address(str(request.gateway).strip())
            prefix = int(request.prefix_length)
            network = ipaddress.IPv4Network(f"{address}/{prefix}", strict=False)
            primary_dns = ipaddress.IPv4Address(str(request.primary_dns).strip())
            secondary_dns = (
                ipaddress.IPv4Address(str(request.secondary_dns).strip())
                if str(request.secondary_dns or "").strip()
                else None
            )
        except (ipaddress.AddressValueError, ipaddress.NetmaskValueError, ValueError) as exc:
            raise NetworkConfigurationError("Invalid IPv4 configuration.") from exc
        if prefix < 1 or prefix > 30:
            raise NetworkConfigurationError("IPv4 prefix must be between 1 and 30.")
        unusable = {network.network_address, network.broadcast_address}
        if (address not in network or gateway not in network
                or address in unusable or gateway in unusable):
            raise NetworkConfigurationError("Address and gateway must be usable hosts.")
        if address == gateway:
            raise NetworkConfigurationError("Address and gateway cannot be identical.")
        for value in (address, gateway, primary_dns, secondary_dns):
            if value is not None and (
                value.is_loopback or value.is_link_local or value.is_multicast
                or value.is_unspecified
            ):
                raise NetworkConfigurationError("Reserved IPv4 addresses are not allowed.")
        return NetworkConfigurationRequest(
            request_id,
            interface,
            MODE_STATIC,
            str(address),
            prefix,
            str(gateway),
            str(primary_dns),
            "" if secondary_dns is None else str(secondary_dns),
        )

    @staticmethod
    def _matches_request(status: dict, request: NetworkConfigurationRequest) -> bool:
        if not isinstance(status, dict) or not bool(status.get("supported")):
            return False
        if str(status.get("interface", "")) != request.interface:
            return False
        if str(status.get("mode", "")).upper() != request.mode:
            return False
        if request.mode == MODE_DHCP:
            return True
        if (
            str(status.get("ip_address", "")) != request.ip_address
            or int(status.get("prefix_length", 0) or 0) != request.prefix_length
            or str(status.get("gateway", "")) != request.gateway
            or str(status.get("primary_dns", "")) != request.primary_dns
        ):
            return False
        requested_secondary = str(request.secondary_dns or "")
        return not requested_secondary or (
            str(status.get("secondary_dns", "")) == requested_secondary
        )

    def _wait_for_connectivity(self) -> bool:
        deadline = self._monotonic() + self.VERIFY_TIMEOUT_SECONDS
        while self._monotonic() < deadline:
            try:
                if self._connectivity_probe():
                    return True
            except Exception:
                pass
            self._sleep(self.PROBE_INTERVAL_SECONDS)
        return False

    def _probe_firebase(self) -> bool:
        host = urlparse(FirebaseConfig.DATABASE_URL).hostname
        if not host:
            return False
        with socket.create_connection((host, 443), timeout=4):
            return True

    def _rollback(self, request_id: str) -> None:
        self._run(
            [*self._helper_command(), "rollback", "--request-id", request_id],
            timeout=self.APPLY_TIMEOUT_SECONDS,
        )

    def _helper_command(self) -> list[str]:
        if getattr(os, "geteuid", lambda: 1000)() == 0:
            return [self._helper_path]
        return ["sudo", "-n", self._helper_path]

    def _run(self, arguments: Sequence[str], *, timeout: int) -> str:
        completed = self._runner(
            list(arguments),
            check=True,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        return str(completed.stdout or "")

    def _run_read(self, arguments: Sequence[str]) -> str:
        return self._run(arguments, timeout=8)

    def _tailscale_ip(self) -> str:
        if not shutil.which("tailscale"):
            return ""
        try:
            return self._run_read(["tailscale", "ip", "-4"]).splitlines()[0].strip()
        except (OSError, subprocess.SubprocessError, IndexError):
            return ""

    @staticmethod
    def _route_value(route: str, key: str) -> str:
        values = route.split()
        try:
            return values[values.index(key) + 1]
        except (ValueError, IndexError):
            return ""

    @staticmethod
    def _address_and_prefix(output: str) -> tuple[str, int]:
        values = output.split()
        try:
            address = values[values.index("inet") + 1]
            ip_text, prefix_text = address.split("/", 1)
            return ip_text, int(prefix_text)
        except (ValueError, IndexError):
            return "", 0

    @staticmethod
    def _subnet_mask(prefix: int) -> str:
        if prefix < 0 or prefix > 32:
            return ""
        return str(ipaddress.IPv4Network(f"0.0.0.0/{prefix}").netmask)
