"""Season boundary checks for the in-memory multi-zone irrigation AI."""

from tools.hardware_test_stubs import install_hardware_import_stubs

install_hardware_import_stubs()

from services.irrigation_service import IrrigationService


class _Engine:
    def __init__(self) -> None:
        self.reset_sensor_ids: list[str] = []

    def reset(self, sensor_id: str) -> bool:
        self.reset_sensor_ids.append(sensor_id)
        return True


class _Logger:
    def info(self, *_args, **_kwargs) -> None:
        return None


def main() -> None:
    assert IrrigationService._active_zone_season_id(
        {"season": {"status": "ACTIVE", "active_season_id": "season-new"}}
    ) == "season-new"
    assert IrrigationService._active_zone_season_id(
        {"season": {"status": "CLOSED", "active_season_id": "season-old"}}
    ) == ""
    assert IrrigationService._active_zone_season_id({}) == ""

    service = IrrigationService.__new__(IrrigationService)
    service._zone_ai_pipelines = {"zone-001": object(), "zone-002": object()}
    service._zone_prediction_validation_queues = {
        "zone-001": object(),
        "zone-002": object(),
    }
    service._zone_prediction_histories = {
        "zone-001": ["old-result"],
        "zone-002": ["keep-result"],
    }
    service._adaptive_recommendations_by_zone = {
        "zone-001": object(),
        "zone-002": object(),
    }
    service._watering_duration_plans_by_zone = {
        "zone-001": object(),
        "zone-002": object(),
    }
    service._adaptive_recommendation_cache = {
        "zone-001": object(),
        "zone-002": object(),
    }
    service._multi_zone_engine = _Engine()
    service._last_zone_ai_update = 42.0
    service._last_multi_zone_status_signature = ("old",)
    service._last_multi_zone_log_signature = ("old",)
    service._logger = _Logger()
    service._persist_zone_irrigation_safety_state = lambda **_kwargs: None

    service._reset_transient_zone_ai_for_season(
        zone_id="zone-001",
        sensor_id="soil-001",
        previous_season_id="season-old",
        active_season_id="season-new",
    )

    for mapping in (
        service._zone_ai_pipelines,
        service._zone_prediction_validation_queues,
        service._zone_prediction_histories,
        service._adaptive_recommendations_by_zone,
        service._watering_duration_plans_by_zone,
        service._adaptive_recommendation_cache,
    ):
        assert "zone-001" not in mapping
        assert "zone-002" in mapping
    assert service._multi_zone_engine.reset_sensor_ids == ["soil-001"]
    assert service._last_zone_ai_update == 0.0
    assert service._last_multi_zone_status_signature is None
    assert service._last_multi_zone_log_signature is None

    print("[PASS] Season boundary resets only transient zone AI state.")


if __name__ == "__main__":
    main()
