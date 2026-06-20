#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="${ROOT_DIR}/ops/logs"
LOG_FILE="${LOG_DIR}/monitor.log"

mkdir -p "$LOG_DIR"

log() {
  printf '[%s] %s\n' "$(date +'%F %T')" "$*" >> "$LOG_FILE"
}

cd "$ROOT_DIR"

compose_cmd=(docker compose --profile app)
if [[ -n "${ACR_REGISTRY:-}" && -n "${ACR_NAMESPACE:-}" && -f docker-compose.prod.yml ]]; then
  export ACR_REGISTRY ACR_NAMESPACE
  compose_cmd=(docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile app)
fi

if ! "${compose_cmd[@]}" ps --format json >/tmp/hill-commerce-ps.json 2>/tmp/hill-commerce-ps.err; then
  log "compose ps failed: $(cat /tmp/hill-commerce-ps.err)"
  exit 1
fi

if ! curl -fsS --max-time 5 http://127.0.0.1:8080/api/health >/dev/null; then
  log "backend health check failed, attempting restart"
  if [[ ${#compose_cmd[@]} -gt 4 ]]; then
    "${compose_cmd[@]}" up -d --no-build backend nginx >> "$LOG_FILE" 2>&1 || true
  else
    "${compose_cmd[@]}" up -d backend nginx >> "$LOG_FILE" 2>&1 || true
  fi
fi

if ! curl -fsS --max-time 5 http://127.0.0.1/ >/dev/null; then
  log "root endpoint failed, attempting app restart"
  if [[ ${#compose_cmd[@]} -gt 4 ]]; then
    "${compose_cmd[@]}" up -d --no-build frontend nginx >> "$LOG_FILE" 2>&1 || true
  else
    "${compose_cmd[@]}" up -d frontend nginx >> "$LOG_FILE" 2>&1 || true
  fi
fi

if free -m | awk 'NR==2 {exit !($7 < 256)}'; then
  log "low available memory detected"
fi

if swapon --show | awk 'NR>1 {found=1} END {exit !found}'; then
  :
else
  log "swap is not enabled"
fi
