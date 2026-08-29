"""Assign one Firebase user to the AVORA device with a custom claim."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import firebase_admin
from firebase_admin import auth
from firebase_admin import credentials
from firebase_admin.exceptions import FirebaseError


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from core.config import AppConfig  # noqa: E402
from core.config import FirebaseConfig  # noqa: E402


CLAIM_NAME = "avora_device_id"
UID_PATTERN = re.compile(r"^[A-Za-z0-9_-]{1,128}$")
DEVICE_ID_PATTERN = re.compile(r"^[A-Za-z0-9_-]{1,64}$")


def normalize_uid(value: str) -> str:
    uid = value.strip()
    if not UID_PATTERN.fullmatch(uid):
        raise ValueError("Firebase kullanıcı kimliği geçersiz.")
    return uid


def normalize_device_id(value: str) -> str:
    device_id = value.strip()
    if not DEVICE_ID_PATTERN.fullmatch(device_id):
        raise ValueError("AVORA cihaz kimliği geçersiz.")
    return device_id


def merge_device_claim(
    current_claims: dict | None,
    device_id: str,
    *,
    replace_existing: bool = False,
) -> dict:
    claims = dict(current_claims or {})
    existing_device_id = str(claims.get(CLAIM_NAME, "")).strip()
    if (
        existing_device_id
        and existing_device_id != device_id
        and not replace_existing
    ):
        raise ValueError(
            "Kullanıcı başka bir AVORA cihazına bağlı. "
            "Bilinçli değişiklik için --replace-existing kullanın.",
        )
    claims[CLAIM_NAME] = device_id
    return claims


def remove_device_claim(
    current_claims: dict | None,
    device_id: str,
) -> dict:
    claims = dict(current_claims or {})
    existing_device_id = str(claims.get(CLAIM_NAME, "")).strip()
    if existing_device_id and existing_device_id != device_id:
        raise ValueError(
            "Kullanıcı belirtilen AVORA cihazına bağlı değil.",
        )
    claims.pop(CLAIM_NAME, None)
    return claims


def initialize_firebase_admin() -> None:
    try:
        firebase_admin.get_app()
        return
    except ValueError:
        pass

    credentials_path = Path(FirebaseConfig.CREDENTIALS_FILE)
    if not credentials_path.is_absolute():
        credentials_path = (
            Path(__file__).resolve().parents[1] / credentials_path
        )
    if not credentials_path.is_file():
        raise FileNotFoundError(
            f"Firebase hizmet hesabı bulunamadı: {credentials_path}",
        )
    firebase_admin.initialize_app(credentials.Certificate(credentials_path))


def assign_device_owner(
    uid: str,
    device_id: str,
    *,
    replace_existing: bool = False,
) -> None:
    user = auth.get_user(uid)
    claims = merge_device_claim(
        user.custom_claims,
        device_id,
        replace_existing=replace_existing,
    )
    auth.set_custom_user_claims(uid, claims)

    verified_user = auth.get_user(uid)
    verified_claims = verified_user.custom_claims or {}
    if verified_claims.get(CLAIM_NAME) != device_id:
        raise RuntimeError("Firebase cihaz sahipliği doğrulanamadı.")


def revoke_device_owner(uid: str, device_id: str) -> None:
    user = auth.get_user(uid)
    claims = remove_device_claim(user.custom_claims, device_id)
    auth.set_custom_user_claims(uid, claims or None)

    verified_user = auth.get_user(uid)
    verified_claims = verified_user.custom_claims or {}
    if CLAIM_NAME in verified_claims:
        raise RuntimeError("Firebase cihaz sahipliği kaldırılamadı.")


def filter_device_owner_uids(users, device_id: str) -> list[str]:
    owners = []
    for user in users:
        claims = user.custom_claims or {}
        if str(claims.get(CLAIM_NAME, "")).strip() == device_id:
            owners.append(user.uid)
    return sorted(owners)


def list_device_owners(device_id: str) -> list[str]:
    return filter_device_owner_uids(
        auth.list_users().iterate_all(),
        device_id,
    )


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Firebase kullanıcısına yalnız belirtilen AVORA cihazı için "
            "erişim yetkisi verir."
        ),
    )
    parser.add_argument(
        "--uid",
        help="Geri bildirim e-postasında görünen Firebase kullanıcı kimliği.",
    )
    parser.add_argument(
        "--device-id",
        default=AppConfig.DEVICE_ID,
        help="Yetkilendirilecek AVORA cihaz kimliği.",
    )
    parser.add_argument(
        "--replace-existing",
        action="store_true",
        help="Kullanıcının mevcut farklı cihaz yetkisini bilinçli değiştirir.",
    )
    parser.add_argument(
        "--remove",
        action="store_true",
        help="Belirtilen kullanıcının AVORA cihaz erişimini kaldırır.",
    )
    parser.add_argument(
        "--list",
        action="store_true",
        help="Belirtilen AVORA cihazına yetkili kullanıcıları salt-okunur listeler.",
    )
    arguments = parser.parse_args()

    try:
        device_id = normalize_device_id(arguments.device_id)
        initialize_firebase_admin()
        if arguments.list:
            if arguments.uid or arguments.remove or arguments.replace_existing:
                parser.error("--list başka bir kullanıcı değiştirme seçeneğiyle kullanılamaz.")
            owners = list_device_owners(device_id)
            print(f"AVORA cihazı: {device_id}")
            print(f"Yetkili kullanıcı sayısı: {len(owners)}")
            for owner_uid in owners:
                print(f"- {owner_uid}")
            return

        if not arguments.uid:
            parser.error("Yetki vermek veya kaldırmak için --uid gereklidir.")
        uid = normalize_uid(arguments.uid)
        if arguments.remove:
            revoke_device_owner(uid, device_id)
        else:
            assign_device_owner(
                uid,
                device_id,
                replace_existing=arguments.replace_existing,
            )
    except auth.UserNotFoundError:
        parser.error("Firebase kullanıcı kimliği bulunamadı.")
    except (FirebaseError, FileNotFoundError, RuntimeError, ValueError) as exc:
        parser.error(str(exc))

    action_label = "kaldırıldı" if arguments.remove else "güvenle kaydedildi"
    print(f"Firebase cihaz sahipliği {action_label}.")
    print(f"Kullanıcı kimliği: {uid}")
    print(f"AVORA cihazı: {device_id}")
    print("Yetki değişikliğini uygulamak için Android uygulamasını kapatıp açın.")


if __name__ == "__main__":
    main()
