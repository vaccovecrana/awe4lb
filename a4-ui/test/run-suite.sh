#!/usr/bin/env bash
#
# Starts a throwaway awe4lb API/UI instance in the background, runs one of the
# browser suites against it, and stops it again.
#
# Usage:
#   ./a4-ui/test/run-suite.sh e2e
#   ./a4-ui/test/run-suite.sh visual
#   A4_API_PORT=18080 ./a4-ui/test/run-suite.sh visual
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MODE="${1:-e2e}"
PORT="${A4_API_PORT:-17070}"
BIN="$ROOT/a4-lb/build/install/a4lb-linux-amd64/bin/a4lb-linux-amd64"

case "$MODE" in
  e2e|visual) ;;
  *)
    echo "usage: $0 [e2e|visual]" >&2
    exit 2
    ;;
esac

if [[ ! -x "$BIN" ]]; then
  (cd "$ROOT" && ./gradlew :a4lb-linux-amd64:installDist)
fi

TMP="$(mktemp -d)"
PID=""
cleanup() {
  if [[ -n "$PID" ]]; then
    kill "$PID" 2>/dev/null || true
    for _ in $(seq 1 50); do
      kill -0 "$PID" 2>/dev/null || break
      sleep 0.1
    done
    kill -9 "$PID" 2>/dev/null || true
    wait "$PID" 2>/dev/null || true
  fi
  rm -rf "$TMP"
}
trap cleanup EXIT INT TERM

"$BIN" --config="$TMP" --api-port="$PORT" --log-level=error </dev/null >"$TMP/app.log" 2>&1 &
PID=$!

for _ in $(seq 1 100); do
  if curl -fsS "http://127.0.0.1:$PORT/" >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "error: UI server exited" >&2
    cat "$TMP/app.log" >&2
    exit 1
  fi
  sleep 0.1
done

if ! curl -fsS "http://127.0.0.1:$PORT/" >/dev/null 2>&1; then
  echo "error: timed out waiting for the UI server" >&2
  cat "$TMP/app.log" >&2
  exit 1
fi

export A4_UI_URL="http://127.0.0.1:$PORT"
cd "$ROOT"

if [[ "$MODE" == "visual" ]]; then
  npm --prefix a4-ui run visual
else
  npm --prefix a4-ui run test:e2e
fi
