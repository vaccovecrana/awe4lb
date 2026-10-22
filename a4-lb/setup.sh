#!/usr/bin/env bash
#
# awe4lb installer / upgrader for a rootless, systemd-based deployment.
#
# Usage:
#   sudo bash a4-lb/setup.sh --binary ./a4lb-linux-amd64 --config ./production.json
#   sudo bash a4-lb/setup.sh                 # uses the local native build, if present
#
# The service runs as an unprivileged user. Low ports are granted through the
# unit's AmbientCapabilities=CAP_NET_BIND_SERVICE; no setcap is used.
#
set -euo pipefail

export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${PATH:-}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_SLUG="vaccovecrana/awe4lb"
USER_NAME="a4lb"
GROUP_NAME="a4lb"
CONFIG_DIR="/etc/a4lb"
INSTALL_DIR="/usr/local/bin"
UNIT_PATH="/etc/systemd/system/a4lb.service"
BINARY=""
CONFIG=""
START=1

usage() {
  sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'
}

die() {
  echo "error: $*" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --user) USER_NAME="$2"; GROUP_NAME="$2"; shift 2 ;;
    --group) GROUP_NAME="$2"; shift 2 ;;
    --config-dir) CONFIG_DIR="$2"; shift 2 ;;
    --install-dir) INSTALL_DIR="$2"; shift 2 ;;
    --binary) BINARY="$2"; shift 2 ;;
    --config) CONFIG="$2"; shift 2 ;;
    --no-start) START=0; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown option: $1 (try --help)" ;;
  esac
done

[ "$(id -u)" -eq 0 ] || die "run as root (sudo bash $0 ...)"
for tool in install systemctl useradd groupadd getent id; do
  command -v "$tool" >/dev/null 2>&1 || die "$tool is required"
done
for value in "$USER_NAME" "$GROUP_NAME" "$CONFIG_DIR" "$INSTALL_DIR"; do
  case "$value" in
    *[!A-Za-z0-9_./-]*) die "unsupported character in path/name: $value" ;;
  esac
done

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

# --- binary -----------------------------------------------------------------
if [ -z "$BINARY" ]; then
  if [ -x "$ROOT/a4-lb/build/native/nativeCompile/a4lb-linux-amd64" ]; then
    BINARY="$ROOT/a4-lb/build/native/nativeCompile/a4lb-linux-amd64"
  elif [ -x "$ROOT/a4lb-linux-amd64" ]; then
    BINARY="$ROOT/a4lb-linux-amd64"
  else
    BINARY="$TMP_DIR/a4lb-linux-amd64"
    URL="https://github.com/${REPO_SLUG}/releases/latest/download/a4lb-linux-amd64"
    echo "downloading ${URL}"
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 -o "$BINARY" "$URL"
      curl -fL --retry 3 -o "$BINARY.sha256" "$URL.sha256" 2>/dev/null || true
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$BINARY" "$URL"
      wget -O "$BINARY.sha256" "$URL.sha256" 2>/dev/null || true
    else
      die "curl or wget is required to download the release binary"
    fi
    if [ -f "$BINARY.sha256" ]; then
      command -v sha256sum >/dev/null 2>&1 || die "sha256sum is required to verify the download"
      (cd "$(dirname "$BINARY")" && sha256sum -c "$(basename "$BINARY").sha256")
    fi
  fi
fi
[ -f "$BINARY" ] || die "binary not found: $BINARY"

# --- user / group -----------------------------------------------------------
if ! getent group "$GROUP_NAME" >/dev/null 2>&1; then
  groupadd --system "$GROUP_NAME"
fi
if ! id "$USER_NAME" >/dev/null 2>&1; then
  useradd --system --gid "$GROUP_NAME" --home-dir "$CONFIG_DIR" \
    --shell /usr/sbin/nologin "$USER_NAME"
fi

# --- directories ------------------------------------------------------------
install -d -o "$USER_NAME" -g "$GROUP_NAME" -m 0750 "$CONFIG_DIR"
install -d -m 0755 "$INSTALL_DIR"
install -m 0755 "$BINARY" "$INSTALL_DIR/a4lb"

# --- boot configuration -----------------------------------------------------
if [ -n "$CONFIG" ]; then
  [ -f "$CONFIG" ] || die "configuration not found: $CONFIG"
  BASE="$(basename "$CONFIG")"
  install -m 0640 -o "$USER_NAME" -g "$GROUP_NAME" "$CONFIG" "$CONFIG_DIR/$BASE"
  if [ "$BASE" != "active.json" ]; then
    ln -sfn "$BASE" "$CONFIG_DIR/active.json"
  fi
elif [ ! -e "$CONFIG_DIR/active.json" ]; then
  # A placeholder lets the UI start even before the first config is written.
  install -m 0640 -o "$USER_NAME" -g "$GROUP_NAME" /dev/null "$CONFIG_DIR/active.json"
  echo "created empty $CONFIG_DIR/active.json; the UI will start without an active config"
fi

# --- unit -------------------------------------------------------------------
if [ -f "$ROOT/a4-lb/a4lb.service" ]; then
  UNIT_SRC="$ROOT/a4-lb/a4lb.service"
elif [ -f "$(dirname "${BASH_SOURCE[0]}")/a4lb.service" ]; then
  UNIT_SRC="$(dirname "${BASH_SOURCE[0]}")/a4lb.service"
else
  die "unit not found (expected a4-lb/a4lb.service next to this script or in the repository)"
fi
UNIT_TMP="$TMP_DIR/a4lb.service"
sed \
  -e "s|/etc/a4lb|$CONFIG_DIR|g" \
  -e "s|/usr/local/bin/a4lb|$INSTALL_DIR/a4lb|g" \
  -e "s|^User=.*|User=$USER_NAME|" \
  -e "s|^Group=.*|Group=$GROUP_NAME|" \
  "$UNIT_SRC" > "$UNIT_TMP"
install -m 0644 "$UNIT_TMP" "$UNIT_PATH"
systemctl daemon-reload
systemctl enable a4lb >/dev/null

if [ "$START" -eq 1 ]; then
  systemctl restart a4lb
fi

echo
echo "a4lb installed."
echo "  user:       $USER_NAME"
echo "  config dir: $CONFIG_DIR"
echo "  boot file:  $CONFIG_DIR/active.json"
echo "  API:        http://127.0.0.1:7070 (localhost only by default)"
echo
echo "Use an SSH tunnel or authenticated reverse proxy to reach the UI."
echo "Check logs with: journalctl -u a4lb -f"
