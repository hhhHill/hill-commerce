#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "== Host =="
date
uname -a
echo

echo "== Memory =="
free -h
echo

echo "== Swap =="
swapon --show
echo

echo "== Disk =="
df -h /
echo

echo "== Compose Status =="
cd "$ROOT_DIR"
docker compose --profile app ps
echo

echo "== Health Endpoints =="
curl -fsS --max-time 5 http://127.0.0.1:8080/api/health || true
echo
curl -I --max-time 5 http://127.0.0.1/ || true
