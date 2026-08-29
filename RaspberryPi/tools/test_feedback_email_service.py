"""Regression checks for Raspberry Pi feedback email delivery."""

from __future__ import annotations

import sys
import time
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from services.feedback_email_service import (  # noqa: E402
    FeedbackEmailService,
    FeedbackEmailSettings,
    build_feedback_email,
)
from tools.configure_feedback_email import (  # noqa: E402
    normalize_app_password,
    render_environment,
)


class FakeRepository:
    def __init__(self, activation_epoch: int, feedback: dict[str, dict]) -> None:
        self.activation_epoch = activation_epoch
        self.feedback = feedback

    def get_user_feedback(self) -> dict[str, dict]:
        return self.feedback

    def get_or_create_feedback_email_activation_epoch(
        self,
        now_epoch: int,
    ) -> int:
        del now_epoch
        return self.activation_epoch

    def claim_user_feedback_email(
        self,
        feedback_id: str,
        *,
        lease_token: str,
        now_epoch: int,
        lease_seconds: int,
    ) -> dict | None:
        del lease_seconds
        item = self.feedback[feedback_id]
        delivery = item.setdefault("email_delivery", {})
        if delivery.get("status") in {"sending", "sent"}:
            return None
        attempt_count = int(delivery.get("attempt_count", 0)) + 1
        delivery.update(
            {
                "status": "sending",
                "lease_token": lease_token,
                "claimed_at_epoch": now_epoch,
                "attempt_count": attempt_count,
            },
        )
        return dict(delivery)

    def mark_user_feedback_email_sent(
        self,
        feedback_id: str,
        *,
        lease_token: str,
        message_id: str,
        now_epoch: int,
    ) -> None:
        delivery = self.feedback[feedback_id]["email_delivery"]
        assert delivery["lease_token"] == lease_token
        delivery.update(
            {
                "status": "sent",
                "message_id": message_id,
                "sent_at_epoch": now_epoch,
            },
        )

    def mark_user_feedback_email_failed(self, *args, **kwargs) -> None:
        feedback_id = args[0]
        delivery = self.feedback[feedback_id]["email_delivery"]
        assert delivery["lease_token"] == kwargs["lease_token"]
        delivery.update(
            {
                "status": "failed",
                "last_error": kwargs["error_message"],
                "failed_at_epoch": kwargs["now_epoch"],
                "next_attempt_at_epoch": kwargs["next_attempt_at_epoch"],
            },
        )


class FakeTransport:
    def __init__(self) -> None:
        self.messages = []

    def send(self, message) -> None:
        self.messages.append(message)


class FailingTransport:
    def __init__(self, error_message: str) -> None:
        self.error_message = error_message

    def send(self, message) -> None:
        del message
        raise RuntimeError(self.error_message)


def main() -> None:
    now_epoch = int(time.time())
    settings = FeedbackEmailSettings(
        enabled=True,
        sender="alidogukan@gmail.com",
        recipient="alidogukan@gmail.com",
        app_password="abcdefghijklmnop",
    )
    feedback = {
        "old-test": {
            "created_at": (now_epoch - 60) * 1000,
            "type": "problem",
            "subject": "Eski deneme",
            "description": "Gönderilmemeli",
        },
        "new-feedback": {
            "created_at": (now_epoch + 1) * 1000,
            "type": "suggestion",
            "area_label": "Sulama ve donanım",
            "subject": "Pompa ekranı",
            "description": "Daha anlaşılır bir durum açıklaması eklenebilir.",
            "contact_email": "user@example.com",
            "diagnostics": {
                "app_version": "2.10.0",
                "api_key": "must-not-leak",
            },
        },
    }
    repository = FakeRepository(now_epoch, feedback)
    transport = FakeTransport()
    service = FeedbackEmailService(
        repository,
        settings=settings,
        transport=transport,
    )

    assert service.process_once() == 1
    assert service.process_once() == 0
    assert len(transport.messages) == 1
    message = transport.messages[0]
    assert message["To"] == "alidogukan@gmail.com"
    assert message["Reply-To"] == "user@example.com"
    assert "Öneri" in str(message["Subject"])
    body = message.get_content()
    assert "Sulama ve donanım" in body
    assert "app_version" in body
    assert "must-not-leak" not in body
    assert feedback["new-feedback"]["email_delivery"]["status"] == "sent"
    assert "email_delivery" not in feedback["old-test"]

    unsafe = build_feedback_email(
        "line\nbreak",
        {
            "created_at": now_epoch * 1000,
            "type": "problem\nBcc: attacker@example.com",
            "subject": "Başlık\nBcc: attacker@example.com",
            "description": "Açıklama",
            "contact_email": "victim@example.com\nBcc: attacker@example.com",
        },
        settings,
    )
    assert "\n" not in str(unsafe["Subject"])
    assert unsafe["Reply-To"] is None

    failed_feedback = {
        "smtp-failure": {
            "created_at": (now_epoch + 2) * 1000,
            "type": "problem",
            "subject": "SMTP denemesi",
            "description": "Geçici hata yeniden denenmeli.",
        },
    }
    failed_repository = FakeRepository(now_epoch, failed_feedback)
    failed_service = FeedbackEmailService(
        failed_repository,
        settings=settings,
        transport=FailingTransport(
            f"authentication failed: {settings.app_password}",
        ),
    )
    assert failed_service.process_once() == 0
    failed_delivery = failed_feedback["smtp-failure"]["email_delivery"]
    assert failed_delivery["status"] == "failed"
    assert settings.app_password not in failed_delivery["last_error"]
    assert "<redacted>" in failed_delivery["last_error"]
    assert failed_delivery["next_attempt_at_epoch"] > now_epoch

    assert normalize_app_password("abcd efgh ijkl mnop") == "abcdefghijklmnop"
    environment = render_environment(
        "alidogukan@gmail.com",
        "abcdefghijklmnop",
    )
    assert "SMARTGARDEN_FEEDBACK_EMAIL_ENABLED=true" in environment
    assert "SMARTGARDEN_GMAIL_APP_PASSWORD=abcdefghijklmnop" in environment
    assert "SMARTGARDEN_FEEDBACK_EMAIL_SEND_EXISTING=false" in environment

    print("[PASS] Raspberry Pi feedback email delivery tests.")


if __name__ == "__main__":
    main()
