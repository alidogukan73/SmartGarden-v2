"""Small LAN-only HTTP gateway for the Android Plant Doctor screen."""

from __future__ import annotations

import json
import hmac
import logging
import os
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Callable, Mapping

import firebase_admin
from firebase_admin import app_check, credentials

from core.config import FirebaseConfig
from services.plant_vision_service import PlantVisionService

HOST = os.getenv("SMARTGARDEN_VISION_HOST", "0.0.0.0")
PORT = int(os.getenv("SMARTGARDEN_VISION_PORT", "8787"))
MAX_BODY_BYTES = 7 * 1024 * 1024
TOKEN_PATH = Path(os.getenv("SMARTGARDEN_VISION_TOKEN_FILE", "vision_client_token.txt"))
FIREBASE_APP_ID = os.getenv(
    "SMARTGARDEN_VISION_FIREBASE_APP_ID",
    "1:891662021997:android:cb8c582fdcbd87e6829664",
).strip()
SERVICE = PlantVisionService()
LOGGER = logging.getLogger("smartgarden.plant_vision")
APP_CHECK_READY = False


def _token() -> str:
    try:
        return TOKEN_PATH.read_text(encoding="utf-8").strip()
    except OSError:
        return ""


def _initialize_app_check() -> None:
    global APP_CHECK_READY
    try:
        firebase_admin.get_app()
    except ValueError:
        credentials_path = Path(FirebaseConfig.CREDENTIALS_FILE)
        if not credentials_path.is_absolute():
            credentials_path = Path(__file__).resolve().parents[1] / credentials_path
        firebase_admin.initialize_app(credentials.Certificate(credentials_path))
    APP_CHECK_READY = True
    LOGGER.info("Plant vision Firebase App Check initialized.")


def _verify_app_check_token(
    token: str,
    verifier: Callable[[str], Mapping[str, object]] | None = None,
) -> bool:
    if not token or not FIREBASE_APP_ID:
        return False
    verify = verifier or app_check.verify_token
    try:
        claims = verify(token)
    except Exception as error:
        LOGGER.warning(
            "Plant vision App Check verification failed: %s",
            type(error).__name__,
        )
        return False
    received_app_id = str(claims.get("app_id", "")).strip()
    return hmac.compare_digest(received_app_id, FIREBASE_APP_ID)


def _authorization_method(
    headers: Mapping[str, str],
    legacy_token: str,
    verifier: Callable[[str], Mapping[str, object]] | None = None,
) -> str:
    app_check_token = headers.get("X-Firebase-AppCheck", "").strip()
    if _verify_app_check_token(app_check_token, verifier):
        return "APP_CHECK"

    supplied_legacy_token = headers.get("X-SmartGarden-Token", "").strip()
    if legacy_token and hmac.compare_digest(supplied_legacy_token, legacy_token):
        return "LEGACY"
    return ""


class Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        if self.path != "/health":
            self._json(HTTPStatus.NOT_FOUND, {"error": "NOT_FOUND"})
            return
        authorization_ready = APP_CHECK_READY or bool(_token())
        self._json(
            HTTPStatus.OK,
            {"configured": SERVICE.configured() and authorization_ready},
        )

    def do_POST(self) -> None:  # noqa: N802
        supported_paths = {
            "/v1/plant-assistant/analyze",
            "/v1/fertilizer-assistant/organic-alternatives",
        }
        if self.path not in supported_paths:
            self._json(HTTPStatus.NOT_FOUND, {"error": "NOT_FOUND"})
            return
        authorization_method = _authorization_method(self.headers, _token())
        if not authorization_method:
            self._json(HTTPStatus.UNAUTHORIZED, {"error": "UNAUTHORIZED"})
            return
        if authorization_method == "LEGACY":
            LOGGER.warning("Plant vision request used legacy authorization.")
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_BODY_BYTES:
            self._json(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, {"error": "REQUEST_TOO_LARGE"})
            return
        try:
            body = json.loads(self.rfile.read(length).decode("utf-8"))
            if self.path == "/v1/plant-assistant/analyze":
                result = SERVICE.analyze(
                    body["image_base64"],
                    body.get("mime_type", "image/jpeg"),
                    body.get("context", {}),
                )
            else:
                result = SERVICE.advise_organic(body.get("context", {}))
            self._json(HTTPStatus.OK, result)
        except ValueError as error:
            self._json(HTTPStatus.BAD_REQUEST, {"error": str(error)})
        except RuntimeError as error:
            LOGGER.warning("Plant vision request failed: %s", error)
            self._json(HTTPStatus.BAD_GATEWAY, {"error": str(error)})
        except Exception:
            LOGGER.exception("Unexpected plant vision error")
            self._json(HTTPStatus.INTERNAL_SERVER_ERROR, {"error": "ANALYSIS_FAILED"})

    def log_message(self, format: str, *args: object) -> None:
        LOGGER.info("%s - %s", self.address_string(), format % args)

    def _json(self, status: HTTPStatus, payload: dict) -> None:
        content = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(content)))
        self.end_headers()
        self.wfile.write(content)


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    try:
        _initialize_app_check()
    except Exception:
        LOGGER.exception("Plant vision Firebase App Check initialization failed.")
        if not _token():
            raise
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    LOGGER.info("Plant vision server listening on %s:%s", HOST, PORT)
    server.serve_forever()


if __name__ == "__main__":
    main()
