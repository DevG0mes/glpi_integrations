#!/usr/bin/env bash
# Verifica conectividade API + GLPI após deploy.
# Uso: ./deploy/ubuntu/verify.sh [base_url]
# Lê GLPI_API_KEY do .env na raiz do projeto, se existir.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BASE="${1:-http://127.0.0.1:8081}"

API_KEY=""
if [[ -f "$ROOT/.env" ]]; then
  API_KEY="$(grep -E '^GLPI_API_KEY=' "$ROOT/.env" | head -1 | cut -d= -f2- | tr -d '\r"' | xargs)"
fi

CURL_OPTS=(-sf)
if [[ -n "$API_KEY" ]]; then
  CURL_OPTS+=(-H "X-API-Key: ${API_KEY}")
  echo "(usando GLPI_API_KEY do .env)"
else
  echo "(GLPI_API_KEY vazia — requisições /api sem autenticação)"
fi
echo ""

echo "=== Health (público) ==="
curl -sf "${BASE}/actuator/health" | head -c 500
echo ""

echo ""
echo "=== Conexão GLPI ==="
curl "${CURL_OPTS[@]}" "${BASE}/api/glpi/connection-info" | head -c 800
echo ""

echo ""
echo "=== Probe Starlink ==="
curl "${CURL_OPTS[@]}" "${BASE}/api/custom-assets/starlink/probe" | head -c 400
echo ""

echo ""
echo "Se connection-info.credentialsPresent=false, edite .env e: docker compose up -d"
