from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from services import plant_vision_server


def _valid_verifier(_: str) -> dict[str, str]:
    return {"app_id": plant_vision_server.FIREBASE_APP_ID}


def test_valid_app_check_is_accepted() -> None:
    method = plant_vision_server._authorization_method(
        {
            "X-Firebase-AppCheck": "valid-app-check-token",
            "X-SmartGarden-Token": "legacy-token",
        },
        _valid_verifier,
    )
    assert method == "APP_CHECK"


def test_wrong_firebase_app_is_rejected() -> None:
    method = plant_vision_server._authorization_method(
        {"X-Firebase-AppCheck": "other-app-token"},
        lambda _: {"app_id": "another-firebase-app"},
    )
    assert method == ""


def test_verification_failure_is_rejected() -> None:
    def failing_verifier(_: str) -> dict[str, str]:
        raise ValueError("invalid token")

    method = plant_vision_server._authorization_method(
        {"X-Firebase-AppCheck": "invalid-token"},
        failing_verifier,
    )
    assert method == ""


def test_legacy_token_is_rejected() -> None:
    method = plant_vision_server._authorization_method(
        {"X-SmartGarden-Token": "legacy-token"},
        lambda _: {},
    )
    assert method == ""


def test_missing_credentials_are_rejected() -> None:
    assert plant_vision_server._authorization_method({}, lambda _: {}) == ""


if __name__ == "__main__":
    test_valid_app_check_is_accepted()
    test_wrong_firebase_app_is_rejected()
    test_verification_failure_is_rejected()
    test_legacy_token_is_rejected()
    test_missing_credentials_are_rejected()
    print("[PASS] Plant Vision App Check authorization scenarios.")
