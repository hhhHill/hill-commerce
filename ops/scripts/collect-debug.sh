#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="${ROOT_DIR}/ops/debug"
STAMP="$(date +%Y%m%d-%H%M%S)"
DEST="${OUT_DIR}/${STAMP}"

mkdir -p "$DEST"

{
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
  df -h
} > "${DEST}/host.txt"

cd "$ROOT_DIR"
docker compose --profile app ps > "${DEST}/compose-ps.txt" 2>&1
docker ps -a > "${DEST}/docker-ps.txt" 2>&1
docker logs --tail 200 hill-commerce-nginx > "${DEST}/nginx.log" 2>&1 || true
docker logs --tail 200 hill-commerce-backend > "${DEST}/backend.log" 2>&1 || true
docker logs --tail 200 hill-commerce-frontend > "${DEST}/frontend.log" 2>&1 || true
docker logs --tail 200 hill-commerce-mysql > "${DEST}/mysql.log" 2>&1 || true
docker logs --tail 200 hc-gorse > "${DEST}/gorse.log" 2>&1 || true
curl -fsS --max-time 5 http://127.0.0.1:8080/api/health > "${DEST}/backend-health.json" 2>&1 || true
curl -I --max-time 5 http://127.0.0.1/ > "${DEST}/root-head.txt" 2>&1 || true

echo "Debug bundle written to ${DEST}"
