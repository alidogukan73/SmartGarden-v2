"""Offline safety checks for AVORA's organic fertilizer AI advisor."""

from services.plant_vision_service import PlantVisionService


def main() -> None:
    prompt = PlantVisionService._organic_prompt({
        "plant_type": "tomato",
        "growth_stage": "FRUITING",
        "application_method": "DRIP_IRRIGATION",
        "organic_only": True,
        "deterministic_result": "NO_COMPATIBLE_ORGANIC_PRODUCT",
        "zone_name": "Private zone",
        "moisture": 44,
        "stock": {"secret": 12},
        "location": "Private location",
    })
    assert '"plant_type": "tomato"' in prompt
    assert '"growth_stage": "FRUITING"' in prompt
    for forbidden_context in ("Private zone", "moisture", "secret", "Private location"):
        assert forbidden_context not in prompt

    normalized = PlantVisionService._normalize_organic({
        "headline": "Organik alternatifler",
        "rationale": "Meyve döneminde yalnız doğrulanmış organik girdileri değerlendirin.",
        "recommendations": [
            {
                "product_type": "Organik tarıma uygun potasyum kaynağı",
                "purpose": "Meyve gelişimini destekleme",
                "selection_criteria": "Resmî etikette ürün ve dönem uygunluğunu doğrulayın",
                "application_method": "Damlama etiketine göre",
            },
            {
                "product_type": "10-5-40 NPK",
                "purpose": "Kimyasal destek",
                "selection_criteria": "Etiketi kontrol edin",
                "application_method": "Damlama",
            },
            {
                "product_type": "Organik sıvı ürün",
                "purpose": "Destek",
                "selection_criteria": "Etiketi kontrol edin",
                "application_method": "5 kg/dekar",
            },
        ],
        "cautions": ["Hasat aralığını etiketten doğrulayın"],
        "disclaimer": "Uygulama kararı kullanıcıya aittir.",
    })
    assert len(normalized["recommendations"]) == 1
    assert normalized["recommendations"][0]["product_type"].startswith("Organik")
    assert len(normalized["cautions"]) == 1
    print("[PASS] Fertilizer AI service safety scenarios.")


if __name__ == "__main__":
    main()
