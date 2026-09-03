"""Checks for the root-owned AVORA NetworkManager helper."""

from __future__ import annotations

import importlib.machinery
import importlib.util
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[1]
HELPER = ROOT / "deploy" / "avora-network-config"


def load_helper():
    loader = importlib.machinery.SourceFileLoader(
        "avora_network_helper",
        str(HELPER),
    )
    spec = importlib.util.spec_from_loader(loader.name, loader)
    if spec is None or spec.loader is None:
        raise RuntimeError("Network helper could not be loaded")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def completed(arguments, returncode=0):
    return subprocess.CompletedProcess(arguments, returncode, stdout="", stderr="")


def test_active_tailscale_is_restarted() -> None:
    helper = load_helper()
    calls = []

    def subprocess_run(arguments, **kwargs):
        del kwargs
        calls.append(list(arguments))
        return completed(arguments)

    def helper_run(arguments, **kwargs):
        del kwargs
        calls.append(list(arguments))
        return ""

    helper.subprocess.run = subprocess_run
    helper.run = helper_run
    helper.refresh_network_dependents()
    assert ["systemctl", "restart", "tailscaled.service"] in calls


def test_inactive_tailscale_is_left_untouched() -> None:
    helper = load_helper()
    calls = []

    def subprocess_run(arguments, **kwargs):
        del kwargs
        calls.append(list(arguments))
        return completed(arguments, returncode=3)

    def helper_run(arguments, **kwargs):
        del kwargs
        calls.append(list(arguments))
        return ""

    helper.subprocess.run = subprocess_run
    helper.run = helper_run
    helper.refresh_network_dependents()
    assert ["systemctl", "restart", "tailscaled.service"] not in calls


if __name__ == "__main__":
    test_active_tailscale_is_restarted()
    test_inactive_tailscale_is_left_untouched()
    print("[PASS] Raspberry Pi network helper tests.")
