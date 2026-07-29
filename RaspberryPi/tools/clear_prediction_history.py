"""
Clear legacy prediction-history records from Firebase.

Run from the RaspberryPi project root:

    python tools/clear_prediction_history.py
"""

from __future__ import annotations

import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(
        0,
        str(PROJECT_ROOT),
    )


from core.firebase_service import FirebaseService


def main() -> None:
    """
    Clear the persisted prediction history.
    """

    firebase = FirebaseService()

    try:
        firebase.initialize()

        firebase.save_prediction_history(
            [],
        )

        print(
            "[PASS] Prediction history cleared successfully."
        )

    finally:
        firebase.stop_command_sync()


if __name__ == "__main__":
    main()