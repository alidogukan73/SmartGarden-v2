"""Regression checks for Plant Vision growth-assessment prompting."""

import sys
import types

sys.modules.setdefault("requests", types.SimpleNamespace(post=None))

from services.plant_vision_service import PlantVisionService


def main() -> None:
    growth_prompt = PlantVisionService._prompt({
        "analysis_goal": "growth_status",
        "plant": "Domates",
        "symptoms": [],
    })
    assert "primary goal is plant growth assessment" in growth_prompt
    assert "Do not infer exact plant age or growth rate" in growth_prompt
    assert '\"analysis_goal\": \"growth_status\"' in growth_prompt
    assert "growth_score" in growth_prompt
    assert "visible-vigor indicator" in growth_prompt

    normalized = PlantVisionService._normalize({
        "is_plant_photo": True,
        "confidence": 87,
        "urgency": "Düşük",
        "growth_score": 76,
        "growth_stage": "Vejetatif gelişim",
        "growth_signals": [
            "Yeni yaprak oluşumu",
            "Dengeli boğum aralığı",
            "Canlı yaprak rengi",
            "Dik gövde",
            "Bu beşinci değer kırpılmalı",
        ],
    })
    assert normalized["growth_score"] == 76
    assert normalized["growth_stage"] == "Vejetatif gelişim"
    assert len(normalized["growth_signals"]) == 4

    invalid = PlantVisionService._normalize({
        "is_plant_photo": False,
        "growth_score": 92,
        "growth_signals": "array değil",
    })
    assert invalid["growth_score"] == -1
    assert invalid["growth_signals"] == []

    health_prompt = PlantVisionService._prompt({
        "analysis_goal": "health_screening",
        "plant": "Domates",
        "symptoms": ["Yaprakta leke / yanıklık"],
    })
    assert "primary goal is a cautious visual health screening" in health_prompt
    assert "primary goal is plant growth assessment" not in health_prompt
    print("[PASS] Plant Vision growth prompt scenarios.")


if __name__ == "__main__":
    main()
