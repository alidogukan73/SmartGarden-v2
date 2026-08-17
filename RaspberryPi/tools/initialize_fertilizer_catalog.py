"""Initialize verified fertilizer products and crop-stage recommendations."""

from __future__ import annotations

import sys
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, db

PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from core.config import AppConfig, FirebaseConfig


GUBRETAS_20_URL = (
    "https://www.gubretas.com.tr/en/urun/2020.20plusme"
)
GUBRETAS_10_5_40_URL = (
    "https://www.gubretas.com.tr/tr/urun/105.40plusme"
)
GUBRETAS_CALSIMAGSI_URL = (
    "https://www.gubretas.com.tr/tr/urun/calsimagsi"
)
GUBRETAS_SEARIUS_URL = (
    "https://www.gubretas.com.tr/tr/urun/searius"
)
GUBRETAS_MAGSUL_URL = (
    "https://www.gubretas.com.tr/tr/urun/magsul"
)
GUBRETAS_18_URL = (
    "https://www.gubretas.com.tr/tr/urun/1818.18plusme"
)
GUBRETAS_COMBI_PLUS_URL = (
    "https://www.gubretas.com.tr/tr/urun/combi-plus"
)
AGRAGRON_BIOHUMAGRO_URL = (
    "https://www.agragron.com/agragron-biohumagro/"
)
SUPERSOL_FERTISOL_URL = (
    "https://supersol.com.tr/urunler/ss-fertisol/"
)
SUPERSOL_MICRO_TRACE_URL = (
    "https://supersol.com.tr/urunler/ss-micro-trace/"
)
SUPERSOL_ROOT_URL = (
    "https://supersol.com.tr/urunler/root/"
)
SUPERSOL_GREEN_URL = (
    "https://supersol.com.tr/en/urunler/ss-super-green/"
)
SUPERSOL_PAN_URL = (
    "https://supersol.com.tr/urunler/ss-super-pan-2/"
)

PRODUCTS = {
    "product-gubretas-20-20-20-me": {
        "product_id": "product-gubretas-20-20-20-me",
        "name": "GÜBRETAŞ 20.20.20+ME",
        "form": "POWDER",
        "npk": "20-20-20+ME",
        "application_type": "NUTRITION",
        "functional_tags": ["TRACE_ELEMENTS", "PHOSPHATE"],
        "label_dosage": 3.5,
        "label_dosage_min": 3.0,
        "label_dosage_max": 4.0,
        "dosage_unit": "kg/dekar · 1 ton su ile",
        "minimum_interval_days": 7,
        "notes": (
            "Üretici damlama aralığı 3-4 kg/dekar. "
            "Uygulama dozu toprak/yaprak analizine göre ayarlanmalıdır."
        ),
        "enabled": True,
        "verified": True,
        "source_url": GUBRETAS_20_URL,
        "recommended_stages": [
            "ROOTING",
            "VEGETATIVE",
            "FLOWERING",
        ],
    },
    "product-gubretas-10-5-40-me": {
        "product_id": "product-gubretas-10-5-40-me",
        "name": "GÜBRETAŞ 10.5.40+ME",
        "form": "POWDER",
        "npk": "10-5-40+ME",
        "application_type": "NUTRITION",
        "functional_tags": ["TRACE_ELEMENTS", "PHOSPHATE"],
        "label_dosage": 5.5,
        "label_dosage_min": 5.0,
        "label_dosage_max": 6.0,
        "dosage_unit": "kg/dekar · 1 ton su ile",
        "minimum_interval_days": 7,
        "notes": (
            "Üretici damlama aralığı 5-6 kg/dekar. "
            "Meyve irileşmesinden hasada 15-20 gün kalana kadar."
        ),
        "enabled": True,
        "verified": True,
        "source_url": GUBRETAS_10_5_40_URL,
        "recommended_stages": ["FRUITING"],
    },
    "product-gubretas-calsimagsi": {
        "product_id": "product-gubretas-calsimagsi",
        "name": "GÜBRETAŞ CALSİMAGSI",
        "form": "POWDER",
        "npk": "13-0-0 + 16CaO + 6MgO",
        "application_type": "NUTRITION",
        "functional_tags": ["CALCIUM_MAGNESIUM", "CALCIUM"],
        "label_dosage": 3.5,
        "label_dosage_min": 3.0,
        "label_dosage_max": 4.0,
        "dosage_unit": "kg/dekar · 1 ton su ile",
        "minimum_interval_days": 7,
        "notes": (
            "Domates, biber, patlıcan ve hıyarda üretici damlama "
            "aralığı 3-4 kg/dekar; fasulyede 2-3 kg/dekar."
        ),
        "enabled": True,
        "verified": True,
        "source_url": GUBRETAS_CALSIMAGSI_URL,
        "recommended_stages": ["FRUITING"],
    },
    "product-agragron-biohumagro": {
        "product_id": "product-agragron-biohumagro",
        "name": "AgraGron Biohumagro Hümik Fülvik",
        "form": "LIQUID",
        "npk": "Leonardit menşeli humik + fulvik asit",
        "application_type": "ORGANIC",
        "organic_farming_eligible": True,
        "functional_tags": ["ORGANIC_MATTER", "HUMIC_FULVIC"],
        "label_dosage": 100.0,
        "label_dosage_min": 100.0,
        "label_dosage_max": 100.0,
        "dosage_unit": "ml/100 L su · yapraktan",
        "minimum_interval_days": 15,
        "notes": (
            "Toprak düzenleyici ve kök desteğidir; ana NPK gübresi "
            "değildir. Üretici sayfasında damlama dozu belirtilmediği "
            "için damlama uygulamasında ambalaj etiketi esas alınmalıdır."
        ),
        "enabled": True,
        "verified": True,
        "source_url": AGRAGRON_BIOHUMAGRO_URL,
        "recommended_stages": ["ROOTING", "VEGETATIVE"],
    },
    "product-supersol-fertisol": {
        "product_id": "product-supersol-fertisol",
        "name": "Supersol Fertisol",
        "form": "POWDER",
        "npk": "15-0-5 + 5MgO + ME",
        "application_type": "NUTRITION",
        "functional_tags": ["TRACE_ELEMENTS"],
        "label_dosage": 0.75,
        "label_dosage_min": 0.5,
        "label_dosage_max": 1.0,
        "dosage_unit": "kg/dekar · topraktan",
        "minimum_interval_days": 15,
        "notes": (
            "Azot, potasyum, magnezyum ve iz element içerir. "
            "Toprak/yaprak analizine göre düzeltici ürün olarak kullanılmalıdır."
        ),
        "enabled": True,
        "verified": True,
        "source_url": SUPERSOL_FERTISOL_URL,
        "recommended_stages": ["VEGETATIVE"],
    },
    "product-supersol-micro-trace": {
        "product_id": "product-supersol-micro-trace",
        "name": "Supersol SS Micro Trace",
        "form": "POWDER",
        "npk": "İz element karışımı",
        "application_type": "NUTRITION",
        "functional_tags": ["TRACE_ELEMENTS"],
        "label_dosage": 0.75,
        "label_dosage_min": 0.5,
        "label_dosage_max": 1.0,
        "dosage_unit": "kg/dekar · topraktan",
        "minimum_interval_days": 15,
        "notes": (
            "Mikro elementler yalnız analiz veya doğrulanmış noksanlık "
            "durumunda kullanılmalıdır."
        ),
        "enabled": True,
        "verified": True,
        "source_url": SUPERSOL_MICRO_TRACE_URL,
        "recommended_stages": ["VEGETATIVE", "FLOWERING"],
    },
    "product-supersol-super-root": {
        "product_id": "product-supersol-super-root",
        "name": "Supersol SS-Super Root",
        "form": "LIQUID",
        "npk": "Mikrobiyal · 1×10⁷ kob/ml",
        "application_type": "ORGANIC",
        "organic_farming_eligible": True,
        "functional_tags": ["MICROBIAL"],
        "label_dosage": 1.0,
        "label_dosage_min": 1.0,
        "label_dosage_max": 1.0,
        "dosage_unit": "L/dekar · topraktan",
        "minimum_interval_days": 15,
        "notes": (
            "Köklenme desteği, azot bağlama ve fosfat çözündürme amaçlıdır. "
            "Bakterisit ve bakırlı preparatlarla birlikte kullanılmaz."
        ),
        "enabled": True,
        "verified": True,
        "source_url": SUPERSOL_ROOT_URL,
        "recommended_stages": ["ROOTING"],
    },
    "product-supersol-super-green": {
        "product_id": "product-supersol-super-green",
        "name": "Supersol SS-Super Green",
        "form": "LIQUID",
        "npk": "Mikrobiyal · 1×10⁷ kob/ml",
        "application_type": "ORGANIC",
        "organic_farming_eligible": True,
        "functional_tags": ["MICROBIAL"],
        "label_dosage": 1.0,
        "label_dosage_min": 1.0,
        "label_dosage_max": 1.0,
        "dosage_unit": "L/dekar · topraktan",
        "minimum_interval_days": 15,
        "notes": (
            "Vejetatif gelişim ve toprak biyolojisi desteğidir. "
            "Bakterisit ve bakırlı preparatlarla birlikte kullanılmaz."
        ),
        "enabled": True,
        "verified": True,
        "source_url": SUPERSOL_GREEN_URL,
        "recommended_stages": ["VEGETATIVE"],
    },
    "product-supersol-super-pan": {
        "product_id": "product-supersol-super-pan",
        "name": "Supersol SS-Super Pan",
        "form": "LIQUID",
        "npk": "Mikrobiyal · 1×10⁷ kob/ml",
        "application_type": "ORGANIC",
        "organic_farming_eligible": True,
        "functional_tags": ["MICROBIAL"],
        "label_dosage": 1.0,
        "label_dosage_min": 1.0,
        "label_dosage_max": 1.0,
        "dosage_unit": "L/dekar · topraktan",
        "minimum_interval_days": 15,
        "notes": (
            "Üretici tarafından özellikle yumrulu bitkiler için "
            "konumlandırılmıştır. Bakterisit ve bakırlı preparatlarla kullanılmaz."
        ),
        "enabled": True,
        "verified": True,
        "source_url": SUPERSOL_PAN_URL,
        "recommended_stages": ["TUBER_CROPS"],
    },
    "product-gubretas-searius": {
        "product_id": "product-gubretas-searius",
        "name": "GÜBRETAŞ SEARİUS",
        "form": "LIQUID",
        "npk": "Deniz yosunu · %1 K₂O",
        "application_type": "BIOSTIMULANT",
        "functional_tags": ["SEAWEED"],
        "label_dosage": 1.0,
        "label_dosage_min": 1.0,
        "label_dosage_max": 1.0,
        "dosage_unit": "L/dekar · 1 ton su ile",
        "minimum_interval_days": 15,
        "notes": (
            "Fide dikimi sonrası, çiçeklenme ve meyve tutumundan "
            "hasada kadar kullanılan biyostimulanttır; ana NPK değildir."
        ),
        "enabled": True,
        "verified": True,
        "source_url": GUBRETAS_SEARIUS_URL,
        "recommended_stages": ["ROOTING", "FLOWERING", "FRUITING", "HARVEST"],
    },
    "product-gubretas-magsul": {
        "product_id": "product-gubretas-magsul",
        "name": "GÜBRETAŞ MAGSUL",
        "form": "POWDER",
        "npk": "16MgO + 32SO₃",
        "application_type": "NUTRITION",
        "functional_tags": ["CALCIUM_MAGNESIUM", "SULFATE"],
        "label_dosage": 4.0,
        "label_dosage_min": 3.0,
        "label_dosage_max": 5.0,
        "dosage_unit": "kg/dekar · 1 ton su ile",
        "minimum_interval_days": 7,
        "notes": (
            "Domates, biber, patlıcan ve hıyarda meyve bağlama döneminde; "
            "fasulyede nodül oluşumuna kadar önerilir. Kalsiyumla aynı tankta kullanılmaz."
        ),
        "enabled": True,
        "verified": True,
        "source_url": GUBRETAS_MAGSUL_URL,
        "recommended_stages": ["ROOTING", "FRUITING"],
    },
    "product-gubretas-18-18-18-me": {
        "product_id": "product-gubretas-18-18-18-me",
        "name": "GÜBRETAŞ 18.18.18+ME",
        "form": "POWDER",
        "npk": "18-18-18+ME",
        "application_type": "NUTRITION",
        "functional_tags": ["TRACE_ELEMENTS", "PHOSPHATE"],
        "label_dosage": 3.5,
        "label_dosage_min": 3.0,
        "label_dosage_max": 4.0,
        "dosage_unit": "kg/dekar · 1 ton su ile",
        "minimum_interval_days": 7,
        "notes": (
            "Fide dikiminden itibaren dengeli gelişim için kullanılabilir. "
            "Mevcut 20.20.20+ME ile aynı amaç grubundadır; birlikte uygulanmaz."
        ),
        "enabled": True,
        "verified": True,
        "source_url": GUBRETAS_18_URL,
        "recommended_stages": ["ROOTING", "VEGETATIVE", "FLOWERING"],
    },
    "product-gubretas-combi-plus": {
        "product_id": "product-gubretas-combi-plus",
        "name": "GÜBRETAŞ COMBİ PLUS",
        "form": "POWDER",
        "npk": "Cu + Fe + Mn + Zn + B + Mo",
        "application_type": "NUTRITION",
        "functional_tags": ["TRACE_ELEMENTS"],
        "label_dosage": 0.325,
        "label_dosage_min": 0.2,
        "label_dosage_max": 0.45,
        "dosage_unit": "kg/dekar · 1 ton su ile",
        "minimum_interval_days": 7,
        "notes": (
            "Çiçeklenme öncesinden itibaren kullanılabilen iz element "
            "karışımıdır. Yalnız analiz veya doğrulanmış noksanlıkta seçilmelidir."
        ),
        "enabled": True,
        "verified": True,
        "source_url": GUBRETAS_COMBI_PLUS_URL,
        "recommended_stages": ["FLOWERING"],
    },
}

PLANTS = ("tomato", "pepper", "cucumber", "eggplant", "bean")

SOLANACEAE = ("tomato", "pepper", "eggplant")


def recommendation(
    product_id: str,
    minimum: float,
    maximum: float,
    source_url: str,
) -> dict[str, object]:
    return {
        "product_id": product_id,
        "method": "DRIP",
        "dose_min": minimum,
        "dose_max": maximum,
        "dose_unit": "kg/dekar · 1 ton su ile",
        "interval_days": 7,
        "advisory_only": True,
        "requires_soil_or_leaf_analysis": True,
        "source_url": source_url,
    }


def build_recommendations() -> dict[str, object]:
    result: dict[str, object] = {}
    for plant in PLANTS:
        for stage in ("ROOTING", "VEGETATIVE", "FLOWERING"):
            result[f"{plant}/{stage}/balanced_20_20_20"] = (
                recommendation(
                    "product-gubretas-20-20-20-me",
                    3.0,
                    4.0,
                    GUBRETAS_20_URL,
                )
            )
        for stage in ("FRUITING",):
            result[f"{plant}/{stage}/high_potassium_10_5_40"] = (
                recommendation(
                    "product-gubretas-10-5-40-me",
                    5.0,
                    6.0,
                    GUBRETAS_10_5_40_URL,
                )
            )
            result[f"{plant}/{stage}/calcium_magnesium"] = (
                recommendation(
                    "product-gubretas-calsimagsi",
                    2.0 if plant == "bean" else 3.0,
                    3.0 if plant == "bean" else 4.0,
                    GUBRETAS_CALSIMAGSI_URL,
                )
            )
    return result


def stage_guide(
    focus: str,
    support: str,
    caution: str,
) -> dict[str, object]:
    return {
        "primary_focus": focus,
        "support_options": support,
        "caution": caution,
        "advisory_only": True,
        "requires_soil_or_leaf_analysis": True,
    }


def build_stage_guides() -> dict[str, object]:
    guides: dict[str, object] = {}
    for plant in SOLANACEAE:
        guides[f"{plant}/ROOTING"] = stage_guide(
            "Köklenme ve dengeli başlangıç beslemesi",
            "Humik/fulvik asit; dengeli 20.20.20+ME",
            "Humik ürün ana NPK gübresinin yerine geçmez.",
        )
        guides[f"{plant}/VEGETATIVE"] = stage_guide(
            "Dengeli sürgün, yaprak ve kök gelişimi",
            "20.20.20+ME; eksiklik doğrulanırsa şelatlı mikro element",
            "Fazla azot çiçeklenmeyi ve meyve tutumunu geciktirebilir.",
        )
        guides[f"{plant}/FLOWERING"] = stage_guide(
            "Çiçeklenme ve sağlıklı meyve tutumu",
            "Dengeli NPK; yalnız analizle doğrulanırsa bor ve çinko",
            "Borun güvenli aralığı dardır; rutin veya tahmini doz kullanmayın.",
        )
        guides[f"{plant}/FRUITING"] = stage_guide(
            "Potasyum ağırlıklı meyve gelişimi ve kalite",
            "10.5.40+ME; ayrı uygulamada CALSİMAGSI",
            "Kalsiyumu fosfatlı veya sülfatlı ürünlerle aynı tankta karıştırmayın.",
        )
        guides[f"{plant}/HARVEST"] = stage_guide(
            "Hasat sürerken kaliteyi ve bitki dengesini koruma",
            "Yalnız etiketi hasada kadar kullanımı destekleyen ürün; belirtili veya analizli düzeltici destek",
            "Hasat öncesi kısıt, tekrar aralığı ve karışım kurallarına uyun; sıfır kalıntı garantisi verilmez.",
        )
        guides[f"{plant}/SEASON_END"] = stage_guide(
            "Sezonu kapatma ve gelecek ekime hazırlık",
            "Toprak analizi, organik madde ve taban gübresi planı",
            "Aktif bitki besleme planını kapatın; uygulamayı yeni sezon hazırlığına göre kaydedin.",
        )

    for plant in ("cucumber",):
        guides[f"{plant}/ROOTING"] = stage_guide(
            "Hızlı köklenme ve dengeli fide gelişimi",
            "Humik/fulvik asit; dengeli 20.20.20+ME",
            "Granül veya tamamen çözünmeyen organik ürünü damlama tankına koymayın.",
        )
        guides[f"{plant}/VEGETATIVE"] = stage_guide(
            "Dengeli fakat kesintisiz yaprak ve sürgün gelişimi",
            "20.20.20+ME; analizle doğrulanırsa magnezyum ve mikro element",
            "Aşırı azot yumuşak gelişim ve dengesiz meyve yükü oluşturabilir.",
        )
        guides[f"{plant}/FLOWERING"] = stage_guide(
            "Çiçeklenme ve meyve tutumunu koruma",
            "Dengeli NPK; yalnız eksiklikte bor veya çinko",
            "Çiçeklenmede tahmini yaprak gübresi uygulamayın.",
        )
        guides[f"{plant}/FRUITING"] = stage_guide(
            "Potasyum, kalsiyum ve magnezyumla sürekli meyve gelişimi",
            "10.5.40+ME; ayrı uygulamada CALSİMAGSI",
            "Kalsiyumu fosfatlı veya sülfatlı ürünlerle aynı tankta karıştırmayın.",
        )
        guides[f"{plant}/HARVEST"] = stage_guide(
            "Hasat sürerken kaliteyi ve bitki dengesini koruma",
            "Yalnız etiketi hasada kadar kullanımı destekleyen ürün; belirtili veya analizli düzeltici destek",
            "Hasat öncesi kısıt, tekrar aralığı ve karışım kurallarına uyun; sıfır kalıntı garantisi verilmez.",
        )
        guides[f"{plant}/SEASON_END"] = stage_guide(
            "Sezonu kapatma ve gelecek ekime hazırlık",
            "Toprak analizi, organik madde ve taban gübresi planı",
            "Aktif bitki besleme planını kapatın; uygulamayı yeni sezon hazırlığına göre kaydedin.",
        )

    for stage, guide in {
        "ROOTING": stage_guide(
            "Köklenme, nodül oluşumu ve düşük azotlu başlangıç",
            "Humik/fulvik asit; toprak analizine göre fosfor ve potasyum",
            "Fasulyede yüksek azot kök nodüllerini ve sonraki bakla oluşumunu baskılayabilir.",
        ),
        "VEGETATIVE": stage_guide(
            "Dengeli gelişim ve biyolojik azot bağlama",
            "Analizle doğrulanırsa demir, çinko, mangan veya molibden",
            "Rutin azot takviyesi yerine toprak ve yaprak analizini esas alın.",
        ),
        "FLOWERING": stage_guide(
            "Çiçeklenme ve bakla tutumu",
            "Toprak analizine göre fosfor-potasyum; yalnız eksiklikte mikro element",
            "Fazla azot yapraklanmayı artırıp çiçek ve baklayı geciktirebilir.",
        ),
        "FRUITING": stage_guide(
            "Bakla gelişimi ve potasyum desteği",
            "Potasyum ağırlıklı besleme; CALSİMAGSI 2-3 kg/dekar",
            "Kalsiyumu fosfatlı veya sülfatlı ürünlerle aynı tankta karıştırmayın.",
        ),
        "HARVEST": stage_guide(
            "Hasat sürerken bakla kalitesini ve bitki dengesini koruma",
            "Yalnız etiketi hasada kadar kullanımı destekleyen ürün; belirtili veya analizli düzeltici destek",
            "Fazla azottan kaçının; hasat öncesi kısıt ve tekrar aralığına uyun.",
        ),
        "SEASON_END": stage_guide(
            "Sezonu kapatma ve gelecek ekime hazırlık",
            "Toprak analizi, organik madde ve taban gübresi planı",
            "Aktif bitki besleme planını kapatın; uygulamayı yeni sezon hazırlığına göre kaydedin.",
        ),
    }.items():
        guides[f"bean/{stage}"] = guide
    return guides


SAFETY_RULES = {
    "calcium_separate_tank": {
        "title": "Kalsiyum karışım koruması",
        "message": (
            "CALSİMAGSI veya başka bir kalsiyum kaynağını fosfatlı "
            "ya da sülfatlı gübrelerle aynı tankta karıştırmayın. "
            "Uygulamalar arasında hattı temiz suyla yıkayın."
        ),
        "severity": "DANGER",
        "enabled": True,
    },
    "micronutrients_only_when_needed": {
        "title": "Mikro element koruması",
        "message": (
            "Bor, çinko, demir, mangan ve molibden yalnız eksiklik "
            "belirtisi veya analiz sonucu varsa önerilir."
        ),
        "severity": "WARNING",
        "enabled": True,
    },
    "granular_not_for_drip": {
        "title": "Damlama hattı koruması",
        "message": (
            "Granül veya tamamen suda çözünmeyen organik ürünleri "
            "damlama tankına eklemeyin."
        ),
        "severity": "DANGER",
        "enabled": True,
    },
    "microbial_no_copper_or_bactericide": {
        "title": "Mikrobiyal ürün koruması",
        "message": (
            "SS-Super Root, Green ve Pan gibi canlı mikroorganizma "
            "içeren ürünleri bakırlı preparatlar veya bakterisitlerle "
            "birlikte kullanmayın."
        ),
        "severity": "DANGER",
        "enabled": True,
    },
    "balanced_npk_choose_one": {
        "title": "Dengeli NPK tekrar koruması",
        "message": (
            "18.18.18+ME ile 20.20.20+ME aynı amaç grubundadır. "
            "Aynı uygulamada ikisini birlikte kullanmayın."
        ),
        "severity": "WARNING",
        "enabled": True,
    },
}


def initialize_firebase() -> None:
    if firebase_admin._apps:
        return
    firebase_admin.initialize_app(
        credentials.Certificate(
            PROJECT_ROOT / FirebaseConfig.CREDENTIALS_FILE
        ),
        {"databaseURL": FirebaseConfig.DATABASE_URL},
    )


def main() -> None:
    initialize_firebase()
    device_ref = db.reference(f"devices/{AppConfig.DEVICE_ID}")
    current = device_ref.get() or {}
    updates: dict[str, object] = {}

    existing_products = current.get("fertilizer_products") or {}
    for product_id, product in PRODUCTS.items():
        existing = existing_products.get(product_id) or {}
        for field, value in product.items():
            if field not in existing:
                updates[
                    f"fertilizer_products/{product_id}/{field}"
                ] = value
        configured_stages = list(existing.get("recommended_stages") or [])
        catalog_stages = list(product.get("recommended_stages") or [])
        merged_stages = list(dict.fromkeys(configured_stages + catalog_stages))
        if merged_stages != configured_stages:
            updates[
                f"fertilizer_products/{product_id}/recommended_stages"
            ] = merged_stages

    existing_recommendations = (
        (current.get("fertilization") or {})
        .get("recommendations")
        or {}
    )
    for path, value in build_recommendations().items():
        plant, stage, key = path.split("/")
        existing = (
            (existing_recommendations.get(plant) or {})
            .get(stage, {})
            .get(key)
        )
        if existing is None:
            updates[
                f"fertilization/recommendations/{path}"
            ] = value

    existing_guides = (
        (current.get("fertilization") or {})
        .get("stage_guides")
        or {}
    )
    for path, value in build_stage_guides().items():
        plant, stage = path.split("/")
        existing = (
            (existing_guides.get(plant) or {})
            .get(stage)
        )
        if existing != value:
            updates[
                f"fertilization/stage_guides/{path}"
            ] = value

    existing_safety_rules = (
        (current.get("fertilization") or {})
        .get("safety_rules")
        or {}
    )
    for rule_id, value in SAFETY_RULES.items():
        if existing_safety_rules.get(rule_id) is None:
            updates[
                f"fertilization/safety_rules/{rule_id}"
            ] = value

    if updates:
        device_ref.update(updates)
    print(
        "Fertilizer catalog initialized successfully. "
        f"updated_fields={len(updates)}"
    )


if __name__ == "__main__":
    main()
