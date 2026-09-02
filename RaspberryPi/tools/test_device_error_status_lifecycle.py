"""Regression checks for stable Raspberry Pi device-error incidents."""

from __future__ import annotations

import logging
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from core.firebase_service import FirebaseService


class FakeReference:
    def __init__(self, values: dict, path: tuple[str, ...] = ()) -> None:
        self.values = values
        self.path = path

    def child(self, name: str) -> "FakeReference":
        return FakeReference(self.values, self.path + (name,))

    def _node(self) -> dict:
        node = self.values
        for name in self.path:
            node = node.setdefault(name, {})
        return node

    def get(self):
        return dict(self._node())

    def update(self, values: dict) -> None:
        self._node().update(values)


def main() -> None:
    values = {
        "status": {
            "last_error": "'zone-003'",
            "error_incident_id": "stable-incident",
        }
    }
    reference = FakeReference(values)
    service = FirebaseService.__new__(FirebaseService)
    service._logger = logging.getLogger("device-error-status-test")
    service._active_error_incident_id = ""
    service._device_ref = lambda: reference
    service._send_push_notification = lambda **_kwargs: None

    assert service.has_active_error() is True

    service.update_status()
    assert values["status"]["last_error"] == "'zone-003'"
    assert values["status"]["error_incident_id"] == "stable-incident"

    service.report_error("same continuing failure")
    assert values["status"]["error_incident_id"] == "stable-incident"

    service.clear_error()
    assert service.has_active_error() is False
    assert values["status"]["error_incident_id"] == ""

    print("[PASS] Device heartbeat preserves one stable error incident.")


if __name__ == "__main__":
    main()
