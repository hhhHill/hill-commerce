#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "$ROOT_DIR"

if [[ -n "${ACR_REGISTRY:-}" && -n "${ACR_NAMESPACE:-}" && -f docker-compose.prod.yml ]]; then
  export ACR_REGISTRY ACR_NAMESPACE
  docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile app up -d --no-build backend frontend nginx gorse
else
  docker compose --profile app up -d backend frontend nginx gorse
fi

docker compose --profile app ps
