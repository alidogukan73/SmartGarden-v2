"""Unit checks for AVORA Firebase device-owner claim configuration."""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.configure_device_owner import (  # noqa: E402
    CLAIM_NAME,
    merge_device_claim,
    normalize_device_id,
    normalize_uid,
    remove_device_claim,
)


def assert_rejected(action) -> None:
    try:
        action()
    except ValueError:
        return
    raise AssertionError("Invalid ownership value was accepted.")


def main() -> None:
    assert normalize_uid("  abcDEF_123-xyz  ") == "abcDEF_123-xyz"
    assert normalize_device_id(" smartgarden-001 ") == "smartgarden-001"
    assert_rejected(lambda: normalize_uid(""))
    assert_rejected(lambda: normalize_uid("uid with spaces"))
    assert_rejected(lambda: normalize_device_id("../smartgarden-001"))

    claims = merge_device_claim(
        {"support_role": "viewer"},
        "smartgarden-001",
    )
    assert claims == {
        "support_role": "viewer",
        CLAIM_NAME: "smartgarden-001",
    }

    unchanged = merge_device_claim(claims, "smartgarden-001")
    assert unchanged == claims
    assert_rejected(
        lambda: merge_device_claim(claims, "smartgarden-002"),
    )
    replaced = merge_device_claim(
        claims,
        "smartgarden-002",
        replace_existing=True,
    )
    assert replaced[CLAIM_NAME] == "smartgarden-002"
    assert replaced["support_role"] == "viewer"

    removed = remove_device_claim(claims, "smartgarden-001")
    assert CLAIM_NAME not in removed
    assert removed["support_role"] == "viewer"
    assert_rejected(
        lambda: remove_device_claim(claims, "smartgarden-002"),
    )

    print("[PASS] Firebase device owner claim tests.")


if __name__ == "__main__":
    main()
