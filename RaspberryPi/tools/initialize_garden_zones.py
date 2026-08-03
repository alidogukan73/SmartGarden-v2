"""
Create the initial multi-zone garden structure in Firebase.

Existing zones and existing fields are never overwritten.
"""

from __future__ import annotations

import sys
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, db


PROJECT_ROOT = Path(__file__).resolve().parent.parent

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(
        0,
        str(PROJECT_ROOT),
    )

from core.config import AppConfig, FirebaseConfig


DEFAULT_ZONES = {
    "zone-001": {
        "name": "Domates",
        "plant_type": "tomato",
        "emoji": "🍅",
        "sensor_id": "soil-001",
        "valve_id": "valve-001",
        "valve_type": "TWO_WIRE_POWER_OPEN",
        "valve_mode": "SIMULATION",
        "order": 1,
        "enabled": True,
        "irrigation_enabled": False,
        "moisture_limit": 40,
        "pump_duration": 10,
        "cooldown_seconds": 600,
        "restart_delta": 10,
    },
    "zone-002": {
        "name": "Biber",
        "plant_type": "pepper",
        "emoji": "🌶️",
        "sensor_id": "soil-002",
        "valve_id": "valve-002",
        "valve_type": "TWO_WIRE_POWER_OPEN",
        "valve_mode": "SIMULATION",
        "order": 2,
        "enabled": True,
        "irrigation_enabled": False,
        "moisture_limit": 40,
        "pump_duration": 10,
        "cooldown_seconds": 600,
        "restart_delta": 10,
    },
    "zone-003": {
        "name": "Salatalık",
        "plant_type": "cucumber",
        "emoji": "🥒",
        "sensor_id": "soil-003",
        "valve_id": "valve-003",
        "valve_type": "TWO_WIRE_POWER_OPEN",
        "valve_mode": "SIMULATION",
        "order": 3,
        "enabled": True,
        "irrigation_enabled": False,
        "moisture_limit": 40,
        "pump_duration": 10,
        "cooldown_seconds": 600,
        "restart_delta": 10,
    },
    "zone-004": {
        "name": "Patlıcan",
        "plant_type": "eggplant",
        "emoji": "🍆",
        "sensor_id": "soil-004",
        "valve_id": "valve-004",
        "valve_type": "TWO_WIRE_POWER_OPEN",
        "valve_mode": "SIMULATION",
        "order": 4,
        "enabled": True,
        "irrigation_enabled": False,
        "moisture_limit": 40,
        "pump_duration": 10,
        "cooldown_seconds": 600,
        "restart_delta": 10,
    },
    "zone-005": {
        "name": "Fasulye",
        "plant_type": "bean",
        "emoji": "🫘",
        "sensor_id": "soil-005",
        "valve_id": "valve-005",
        "valve_type": "TWO_WIRE_POWER_OPEN",
        "valve_mode": "SIMULATION",
        "order": 5,
        "enabled": True,
        "irrigation_enabled": False,
        "moisture_limit": 40,
        "pump_duration": 10,
        "cooldown_seconds": 600,
        "restart_delta": 10,
    },
}


def initialize_firebase() -> None:
    if firebase_admin._apps:
        return

    credential_path = (
        PROJECT_ROOT
        / FirebaseConfig.CREDENTIALS_FILE
    )

    firebase_admin.initialize_app(
        credentials.Certificate(
            credential_path,
        ),
        {
            "databaseURL": FirebaseConfig.DATABASE_URL,
        },
    )


def main() -> None:
    initialize_firebase()

    zones_ref = db.reference(
        f"devices/{AppConfig.DEVICE_ID}/zones",
    )

    existing = zones_ref.get()

    if existing is not None and not isinstance(
        existing,
        dict,
    ):
        raise RuntimeError(
            "Firebase zones node must be a JSON object.",
        )

    existing = existing or {}
    updates: dict[str, object] = {}

    for zone_id, defaults in DEFAULT_ZONES.items():
        current = existing.get(zone_id)

        if current is None:
            updates[zone_id] = {
                "zone_id": zone_id,
                **defaults,
            }
            continue

        if not isinstance(current, dict):
            raise RuntimeError(
                f"Firebase zone must be a JSON object: {zone_id}",
            )

        for field, value in defaults.items():
            if field not in current:
                updates[f"{zone_id}/{field}"] = value

        if "zone_id" not in current:
            updates[f"{zone_id}/zone_id"] = zone_id

    if not updates:
        print(
            "Garden zones already initialized; "
            "no changes were made.",
        )
        return

    zones_ref.update(updates)

    print(
        "Garden zones initialized successfully. "
        f"updated_fields={len(updates)}",
    )


if __name__ == "__main__":
    main()
