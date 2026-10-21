#!/usr/bin/env bash
#
# Runs the Docker-backed Java integration suite against the test backends in
# a4-test/src/test/resources/docker-compose.yml.
#
# Usage:
#   ./a4-test/e2e.sh
#   A4_TEST_CLASS=A4ServiceTest ./a4-test/e2e.sh
#   KEEP_COMPOSE=1 ./a4-test/e2e.sh
#   SHOW_LOG=1 ./a4-test/e2e.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT/a4-test/src/test/resources/docker-compose.yml"

if docker compose version >/dev/null 2>&1; then
  DC=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  DC=(docker-compose)
else
  echo "error: docker compose is required" >&2
  exit 1
fi

cleanup() {
  if [[ "${KEEP_COMPOSE:-0}" != "1" ]]; then
    echo "Stopping test backends..."
    "${DC[@]}" -f "$COMPOSE_FILE" down -v >/dev/null 2>&1 || true
  else
    echo "KEEP_COMPOSE=1 - leaving test backends running"
  fi
}
trap cleanup EXIT INT TERM

for host in momo.localhost sdr.localhost; do
  if command -v getent >/dev/null 2>&1; then
    resolves=0
    getent hosts "$host" >/dev/null 2>&1 && resolves=1
  elif command -v dscacheutil >/dev/null 2>&1; then
    resolves=0
    dscacheutil -q host -a name "$host" 2>/dev/null | grep -q ip_address && resolves=1
  else
    resolves=1
  fi
  if [[ "$resolves" == "0" ]]; then
    echo "warning: $host does not resolve." >&2
    echo "         add this line to /etc/hosts: 127.0.0.1 momo.localhost sdr.localhost" >&2
  fi
done

echo "Starting Docker test backends..."
"${DC[@]}" -f "$COMPOSE_FILE" up -d --build

wait_tcp() {
  local host=$1
  local port=$2
  for _ in $(seq 1 60); do
    if (echo >"/dev/tcp/$host/$port") >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "error: timed out waiting for $host:$port" >&2
  return 1
}

echo "Waiting for HTTP test backends..."
for port in 4000 4001 4002 4010 4011 4020 4021 4022; do
  wait_tcp 127.0.0.1 "$port"
done
# UDP listeners cannot be probed with /dev/tcp; give them a moment to bind.
sleep 2

GRADLE_ARGS=(:a4-test:cleanTest :a4-test:test)
if [[ "${SHOW_LOG:-0}" == "1" ]]; then
  GRADLE_ARGS+=(--info)
fi
if [[ -n "${A4_TEST_CLASS:-}" ]]; then
  GRADLE_ARGS+=(--tests "io.vacco.a4lb.${A4_TEST_CLASS}")
fi

echo "Running integration tests..."
A4_INTEGRATION_TESTS=1 "$ROOT/gradlew" "${GRADLE_ARGS[@]}"
