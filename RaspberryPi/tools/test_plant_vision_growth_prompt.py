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
