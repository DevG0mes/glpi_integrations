#!/usr/bin/env bash
# Verifica conectividade API + GLPI após deploy.
# Uso: ./deploy/ubuntu/verify.sh [base_url]
set -euo pipefail

BASE="${1:-http://127.0.0.1:8081}"

echo "=== Health ==="
curl -sf "${BASE}/actuator/health" | head -c 500
echo ""

echo ""
echo "=== Conexão GLPI ==="
curl -sf "${BASE}/api/glpi/connection-info" | head -c 800
echo ""

echo ""
echo "=== Probe Starlink ==="
curl -sf "${BASE}/api/custom-assets/starlink/probe" | head -c 400
echo ""

echo ""
echo "Se connection-info.credentialsPresent=false, edite .env ou /etc/glpi-integration/env"
