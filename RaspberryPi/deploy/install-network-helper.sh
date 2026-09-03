#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
HELPER_SOURCE="$SCRIPT_DIR/avora-network-config"
SUDOERS_SOURCE="$SCRIPT_DIR/avora-network-config.sudoers"
SUDOERS_TARGET="/etc/sudoers.d/avora-network-config"

if [ "$(id -u)" -ne 0 ]; then
    echo "Bu kurulum sudo ile çalıştırılmalıdır." >&2
    exit 77
fi

command -v nmcli >/dev/null 2>&1 || {
    echo "NetworkManager (nmcli) bulunamadı; hiçbir değişiklik yapılmadı." >&2
    exit 1
}
command -v visudo >/dev/null 2>&1 || {
    echo "visudo bulunamadı; hiçbir değişiklik yapılmadı." >&2
    exit 1
}

visudo -cf "$SUDOERS_SOURCE"
install -d -o root -g root -m 0700 /var/lib/avora/network
install -o root -g root -m 0755 "$HELPER_SOURCE" /usr/local/sbin/avora-network-config
install -o root -g root -m 0440 "$SUDOERS_SOURCE" "$SUDOERS_TARGET"
visudo -cf "$SUDOERS_TARGET"

echo "AVORA güvenli ağ yardımcısı kuruldu."
