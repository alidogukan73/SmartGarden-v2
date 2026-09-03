"""Shared physical-zone season scope checks for Firebase irrigation reads."""

from tools.hardware_test_stubs import install_hardware_import_stubs

install_hardware_import_stubs()

from core.firebase_service import FirebaseService


def main() -> None:
    active_zone = {
        "season": {
            "status": "ACTIVE",
            "active_season_id": "season-pepper",
            "active_season_ids": {
                "season-tomato": True,
                "season-pepper": True,
                "season-closed": False,
            },
            "include_legacy_records": False,
        }
    }
    assert FirebaseService._active_season_ids_from_zone(active_zone) == (
        "season-pepper",
        "season-tomato",
    )
    assert FirebaseService._season_scope_from_zone(active_zone) == (
        True,
        "season-pepper",
        False,
    )

    list_zone = {
        "season": {
            "status": "ACTIVE",
            "active_season_id": "season-a",
            "active_season_ids": ["season-b", "season-a"],
        }
    }
    assert FirebaseService._active_season_ids_from_zone(list_zone) == (
        "season-a",
        "season-b",
    )

    closed_zone = {
        "season": {
            "status": "CLOSED",
            "active_season_id": "season-old",
        }
    }
    assert FirebaseService._season_scope_from_zone(closed_zone)[0] is False

    # Legacy installations keep their previous behavior until migrated.
    assert FirebaseService._season_scope_from_zone({}) == (True, "", True)

    print("[PASS] Shared physical-zone crop season scope scenarios.")


if __name__ == "__main__":
    main()
