"""Tests for the non-destructive fertilization initializer."""

from __future__ import annotations

from tools.initialize_fertilization_calendar import build_updates


def main() -> None:
    device = {
        "zones": {
            "zone-001": {"name": "Domates"},
            "zone-002": {
                "name": "Biber",
                "fertilization": {
                    "enabled": True,
                    "growth_stage": "FLOWERING",
                },
            },
        },
    }

    updates = build_updates(device)

    assert updates["fertilization/config/mode"] == "ADVISORY_ONLY"
    assert updates[
        "fertilization/config/automatic_dosing_enabled"
    ] is False
    assert updates[
        "zones/zone-001/fertilization/growth_stage"
    ] == "NOT_SET"
    assert "zones/zone-002/fertilization/enabled" not in updates
    assert (
        "zones/zone-002/fertilization/growth_stage"
        not in updates
    )
    assert updates[
        "zones/zone-002/fertilization/reminder_enabled"
    ] is True

    complete_device = {
        "fertilization": {
            "config": {
                "schema_version": 1,
                "mode": "ADVISORY_ONLY",
                "automatic_dosing_enabled": False,
                "require_label_dosage": True,
            },
        },
        "zones": {
            "zone-001": {
                "fertilization": {
                    "enabled": False,
                    "planting_date": "",
                    "growth_stage": "NOT_SET",
                    "active_plan_id": "",
                    "active_product_id": "",
                    "next_application_at_epoch": 0,
                    "reminder_enabled": True,
                    "updated_at_epoch": 0,
                },
            },
        },
    }
    assert build_updates(complete_device) == {}

    print("[PASS] Fertilization initializer is non-destructive.")


if __name__ == "__main__":
    main()
