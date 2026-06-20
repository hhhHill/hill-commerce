#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

: "${ACR_REGISTRY:?ACR_REGISTRY is required}"
: "${ACR_NAMESPACE:?ACR_NAMESPACE is required}"

cd "$ROOT_DIR"
export ACR_REGISTRY ACR_NAMESPACE

compose_cmd=(docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile app)

"${compose_cmd[@]}" pull backend frontend
"${compose_cmd[@]}" up -d --no-build --remove-orphans
"${compose_cmd[@]}" ps
