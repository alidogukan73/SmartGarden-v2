"""
Remove the retired top-level Firebase sensor node.

The multi-zone sensor data under devices/<id>/zones is not changed.
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


def main() -> None:
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

    legacy_ref = db.reference(
        f"devices/{AppConfig.DEVICE_ID}/sensor",
    )

    if legacy_ref.get() is None:
        print(
            "Legacy sensor node is already absent; "
            "no changes were made.",
        )
        return

    legacy_ref.delete()

    print(
        "Legacy sensor node removed successfully. "
        "Zone data was not changed.",
    )


if __name__ == "__main__":
    main()
