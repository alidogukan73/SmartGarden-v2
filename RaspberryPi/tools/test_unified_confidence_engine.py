"""Scenario tests for unified AI confidence."""

from controllers.ai_pipeline import AIPipeline
from controllers.moisture_prediction_engine import MoisturePredictionEngine
from controllers.unified_confidence_engine import (
    UnifiedConfidenceEngine,
)
from models.moisture_trend import MoistureTrend
from models.prediction_accuracy import PredictionAccuracy
from models.sensor_reading import SensorReading
from models.soil_learning_profile import SoilLearningProfile


def _soil_profile(status: str) -> SoilLearningProfile:
    return SoilLearningProfile(
        profile_status=status,
        soil_classification="TEST",
        confidence=0.8,
        confidence_level="HIGH",
        learning_stage=5,
        next_milestone_code="",
        next_milestone_text="",
        remaining_sensor_samples=0,
        remaining_auto_waterings=0,
        sensor_history_count=30,
        watering_count_analyzed=10,
        average_moisture=50.0,
        average_drying_rate_per_minute=0.1,
        average_moisture_gain_per_watering=10.0,
        average_watering_duration_seconds=5.0,
        estimated_water_retention_minutes=60.0,
        irrigation_efficiency=1.0,
        learned_at="2026-07-29T00:00:00",
    )


def _accuracy(status: str) -> PredictionAccuracy:
    return PredictionAccuracy(
        prediction_count=10 if status == "READY" else 0,
        successful_predictions=8 if status == "READY" else 0,
        average_error=1.0,
        maximum_error=2.0,
        minimum_error=0.0,
        accuracy_percent=80.0 if status == "READY" else 0.0,
        confidence_multiplier=1.0,
        status=status,
        generated_at="2026-07-29T00:00:00",
    )


def _trend(samples: int, duration: float) -> MoistureTrend:
    return MoistureTrend(
        classification="NORMAL_DRYING",
        sample_count=samples,
        first_moisture=60,
        latest_moisture=55,
        minimum_moisture=55,
        maximum_moisture=60,
        average_moisture=57.5,
        total_change=-5,
        change_per_minute=-0.5,
        duration_seconds=duration,
        is_stable=False,
    )


def test_input_confidence() -> None:
    good_signal = SensorReading(
        raw=2000,
        voltage=0.25,
        moisture=80,
        rssi=-60,
    )
    assert AIPipeline._sensor_confidence(good_signal) == 0.8
    assert AIPipeline._trend_confidence(_trend(20, 300)) == 1.0
    assert AIPipeline._trend_confidence(_trend(10, 150)) == 0.25


def test_learning_status() -> None:
    engine = UnifiedConfidenceEngine()
    result = engine.analyze(
        soil_profile=_soil_profile("READY"),
        prediction_accuracy=_accuracy("INSUFFICIENT_DATA"),
        sensor_confidence=2.0,
        trend_confidence=-1.0,
    )
    assert result.status == "LEARNING"
    assert result.sensor_confidence == 1.0
    assert result.trend_confidence == 0.0

    ready = engine.analyze(
        soil_profile=_soil_profile("READY"),
        prediction_accuracy=_accuracy("READY"),
        sensor_confidence=0.8,
        trend_confidence=1.0,
    )
    assert ready.status == "READY"

    separated = engine.analyze(
        soil_profile=_soil_profile("READY"),
        prediction_accuracy=_accuracy("READY"),
        connection_confidence=0.2,
        measurement_confidence=1.0,
        decision_confidence=0.8,
        trend_confidence=0.75,
    )
    assert separated.connection_confidence == 0.2
    assert separated.measurement_confidence == 1.0
    assert separated.decision_confidence == 0.8
    assert separated.sensor_confidence == 1.0


def test_post_watering_prediction_guard() -> None:
    engine = MoisturePredictionEngine()
    prediction = engine.analyze(
        trend=_trend(20, 600),
        moisture=55,
        moisture_limit=40,
        transition_blocked=True,
    )
    assert prediction.prediction_status == "POST_WATERING_TRANSITION"
    assert prediction.drying_rate_per_minute == 0.0
    assert prediction.confidence == 0.0


if __name__ == "__main__":
    test_input_confidence()
    test_learning_status()
    test_post_watering_prediction_guard()
    print("[PASS] Unified confidence scenarios.")
