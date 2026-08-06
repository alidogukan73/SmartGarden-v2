"""Gemini-backed, safety constrained visual screening for plant photographs."""

from __future__ import annotations

import base64
import json
from pathlib import Path
from typing import Any

import requests


class PlantVisionService:
    """Calls Gemini using a key which never leaves the Raspberry Pi."""

    API_URL = (
        "https://generativelanguage.googleapis.com/v1beta/"
        "models/gemini-3.1-flash-lite:generateContent"
    )
    MAX_IMAGE_BYTES = 5 * 1024 * 1024

    def __init__(self, key_path: str = "vision_api_key.txt") -> None:
        self._key_path = Path(key_path)

    def configured(self) -> bool:
        return self._key_path.is_file() and bool(self._read_key())

    def analyze(self, image_base64: str, mime_type: str, context: dict[str, Any]) -> dict[str, Any]:
        key = self._read_key()
        if not key:
            raise RuntimeError("VISION_NOT_CONFIGURED")
        if mime_type not in {"image/jpeg", "image/png", "image/webp"}:
            raise ValueError("UNSUPPORTED_IMAGE_TYPE")
        try:
            image = base64.b64decode(image_base64, validate=True)
        except Exception as error:
            raise ValueError("INVALID_IMAGE") from error
        if not image or len(image) > self.MAX_IMAGE_BYTES:
            raise ValueError("IMAGE_TOO_LARGE")

        payload = {
            "generationConfig": {"temperature": 0.2, "responseMimeType": "application/json"},
            "contents": [{"role": "user", "parts": [
                {"text": self._prompt(context)},
                {"inlineData": {"mimeType": mime_type, "data": image_base64}},
            ]}],
        }
        response = requests.post(
            self.API_URL,
            params={"key": key},
            json=payload,
            timeout=45,
        )
        if not response.ok:
            raise RuntimeError(f"VISION_PROVIDER_ERROR:{response.status_code}")
        try:
            text = response.json()["candidates"][0]["content"]["parts"][0]["text"]
            result = json.loads(text)
        except Exception as error:
            raise RuntimeError("VISION_INVALID_RESPONSE") from error
        return self._normalize(result)

    def _read_key(self) -> str:
        try:
            return self._key_path.read_text(encoding="utf-8").strip()
        except OSError:
            return ""

    @staticmethod
    def _prompt(context: dict[str, Any]) -> str:
        safe_context = json.dumps(context, ensure_ascii=False)
        return (
            "You are AVORA Visual Plant Doctor. Analyze the supplied plant photo "
            "together with the garden context. This is agricultural decision support, "
            "not a diagnosis. Never recommend pesticides, dosage, or automatic irrigation. "
            "If photo quality is insufficient, say so clearly. Return only JSON with these keys: "
            "is_plant_photo (boolean), title (Turkish short string), confidence (integer 0-100), "
            "urgency (Düşük|Orta|Yüksek), visual_findings (Turkish string), "
            "possible_causes (array of at most 3 Turkish strings), next_steps (array of at most 4 Turkish strings), "
            "red_flags (array of Turkish strings), disclaimer (Turkish string). "
            "Do not claim a disease with certainty. Garden context: " + safe_context
        )

    @staticmethod
    def _normalize(value: dict[str, Any]) -> dict[str, Any]:
        confidence = value.get("confidence", 0)
        try:
            confidence = max(0, min(100, int(confidence)))
        except (TypeError, ValueError):
            confidence = 0
        urgency = value.get("urgency", "Düşük")
        if urgency not in {"Düşük", "Orta", "Yüksek"}:
            urgency = "Düşük"
        return {
            "is_plant_photo": bool(value.get("is_plant_photo", False)),
            "title": str(value.get("title", "Görsel ön değerlendirme"))[:120],
            "confidence": confidence,
            "urgency": urgency,
            "visual_findings": str(value.get("visual_findings", ""))[:1200],
            "possible_causes": [str(x)[:220] for x in value.get("possible_causes", [])[:3]],
            "next_steps": [str(x)[:220] for x in value.get("next_steps", [])[:4]],
            "red_flags": [str(x)[:220] for x in value.get("red_flags", [])[:3]],
            "disclaimer": str(value.get("disclaimer", "Bu sonuç kesin teşhis değildir."))[:300],
        }
