"""
Create the safe fertilization calendar foundation in Firebase.

Existing values are never overwritten. Product, plan and history nodes
are intentionally created later, when the user saves real data.
"""

from __future__ import annotations

import sys
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, db


PROJECT_ROOT = Path(__file__).resolve().parent.parent

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from core.config import AppConfig, FirebaseConfig
from models.fertilization_profile import FertilizationProfile


FERTILIZATION_CONFIG = {
    "schema_version": 1,
    "mode": "ADVISORY_ONLY",
    "automatic_dosing_enabled": False,
    "require_label_dosage": True,
}


def build_updates(
    device_data: dict[str, object],
) -> dict[str, object]:
    updates: dict[str, object] = {}

    fertilization = device_data.get("fertilization")
    if fertilization is None:
        fertilization = {}
    if not isinstance(fertilization, dict):
        raise RuntimeError(
            "Firebase fertilization node must be a JSON object.",
        )

    config = fertilization.get("config")
    if config is None:
        config = {}
    if not isinstance(config, dict):
        raise RuntimeError(
            "Firebase fertilization/config node must be a JSON object.",
        )

    for field, value in FERTILIZATION_CONFIG.items():
        if field not in config:
            updates[f"fertilization/config/{field}"] = value

    zones = device_data.get("zones")
    if not isinstance(zones, dict) or not zones:
        raise RuntimeError(
            "Garden zones must be initialized first.",
        )

    profile_defaults = FertilizationProfile().to_dict()

    for zone_id, zone in zones.items():
        if not isinstance(zone, dict):
            raise RuntimeError(
                f"Firebase zone must be a JSON object: {zone_id}",
            )

        profile = zone.get("fertilization")
        if profile is None:
            profile = {}
        if not isinstance(profile, dict):
            raise RuntimeError(
                "Zone fertilization profile must be a JSON object: "
                f"{zone_id}",
            )

        for field, value in profile_defaults.items():
            if field not in profile:
                updates[
                    f"zones/{zone_id}/fertilization/{field}"
                ] = value

    return updates


def initialize_firebase() -> None:
    if firebase_admin._apps:
        return

    credential_path = PROJECT_ROOT / FirebaseConfig.CREDENTIALS_FILE

    firebase_admin.initialize_app(
        credentials.Certificate(credential_path),
        {"databaseURL": FirebaseConfig.DATABASE_URL},
    )


def main() -> None:
    initialize_firebase()

    device_ref = db.reference(
        f"devices/{AppConfig.DEVICE_ID}",
    )
    device_data = device_ref.get() or {}

    if not isinstance(device_data, dict):
        raise RuntimeError(
            "Firebase device node must be a JSON object.",
        )

    updates = build_updates(device_data)
    if not updates:
        print(
            "Fertilization calendar already initialized; "
            "no changes were made.",
        )
        return

    device_ref.update(updates)
    print(
        "Fertilization calendar initialized successfully. "
        f"updated_fields={len(updates)}",
    )


if __name__ == "__main__":
    main()
