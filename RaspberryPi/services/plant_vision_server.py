"""Small LAN-only HTTP gateway for the Android Plant Doctor screen."""

from __future__ import annotations

import json
import logging
import os
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from services.plant_vision_service import PlantVisionService

HOST = os.getenv("SMARTGARDEN_VISION_HOST", "0.0.0.0")
PORT = int(os.getenv("SMARTGARDEN_VISION_PORT", "8787"))
MAX_BODY_BYTES = 7 * 1024 * 1024
TOKEN_PATH = Path(os.getenv("SMARTGARDEN_VISION_TOKEN_FILE", "vision_client_token.txt"))
SERVICE = PlantVisionService()
LOGGER = logging.getLogger("smartgarden.plant_vision")


def _token() -> str:
    try:
        return TOKEN_PATH.read_text(encoding="utf-8").strip()
    except OSError:
        return ""


class Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        if self.path != "/health":
            self._json(HTTPStatus.NOT_FOUND, {"error": "NOT_FOUND"})
            return
        self._json(HTTPStatus.OK, {"configured": SERVICE.configured() and bool(_token())})

    def do_POST(self) -> None:  # noqa: N802
        if self.path != "/v1/plant-doctor/analyze":
            self._json(HTTPStatus.NOT_FOUND, {"error": "NOT_FOUND"})
            return
        if not _token() or self.headers.get("X-SmartGarden-Token", "") != _token():
            self._json(HTTPStatus.UNAUTHORIZED, {"error": "UNAUTHORIZED"})
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_BODY_BYTES:
            self._json(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, {"error": "REQUEST_TOO_LARGE"})
            return
        try:
            body = json.loads(self.rfile.read(length).decode("utf-8"))
            result = SERVICE.analyze(body["image_base64"], body.get("mime_type", "image/jpeg"), body.get("context", {}))
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
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    LOGGER.info("Plant vision server listening on %s:%s", HOST, PORT)
    server.serve_forever()


if __name__ == "__main__":
    main()
