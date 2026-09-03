#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Bu kurulum root yetkisi gerektirir." >&2
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_SOURCE="${SCRIPT_DIR}/avahi/avora-mqtt.service"
SERVICE_TARGET="/etc/avahi/services/avora-mqtt.service"

if [[ ! -f "${SERVICE_SOURCE}" ]]; then
  echo "mDNS hizmet dosyasi bulunamadi: ${SERVICE_SOURCE}" >&2
  exit 1
fi

if ! command -v avahi-daemon >/dev/null 2>&1 \
    || ! command -v avahi-browse >/dev/null 2>&1; then
  apt-get update
  apt-get install -y --no-install-recommends avahi-daemon avahi-utils
fi

install -d -o root -g root -m 0755 /etc/avahi/services
install -o root -g root -m 0644 "${SERVICE_SOURCE}" "${SERVICE_TARGET}"
systemctl enable --now avahi-daemon.service
systemctl restart avahi-daemon.service

echo "AVORA MQTT mDNS ilani kuruldu."
systemctl --no-pager --full status avahi-daemon.service
