#!/usr/bin/env bash
# Deploy glpi-integration com Docker Compose (Ubuntu).
# Uso: ./deploy/ubuntu/deploy-docker.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[OK]${NC} $*"; }
warn()  { echo -e "${YELLOW}[!]${NC} $*"; }
fail()  { echo -e "${RED}[ERRO]${NC} $*" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || fail "Docker não instalado. Rode: sudo ./deploy/ubuntu/install-docker.sh"

if ! docker compose version >/dev/null 2>&1; then
  fail "Docker Compose v2 não encontrado (plugin 'docker compose')."
fi

if [[ ! -f .env ]]; then
  if [[ -f .env.example ]]; then
    cp .env.example .env
    warn "Arquivo .env criado a partir de .env.example — edite GLPI_API_BASE_URL e tokens antes de usar em produção."
  else
    fail "Crie o arquivo .env na raiz do projeto (veja .env.example)."
  fi
fi

mkdir -p data

# Valida variáveis mínimas
# shellcheck disable=SC1091
set +u
source .env 2>/dev/null || true
set -u

if [[ -z "${GLPI_APP_TOKEN:-}" || -z "${GLPI_USER_TOKEN:-}" ]]; then
  warn "GLPI_APP_TOKEN ou GLPI_USER_TOKEN vazios no .env — a API não conectará ao GLPI até preencher."
fi

PROFILE="${SPRING_PROFILES_ACTIVE:-prod}"
if [[ "$PROFILE" == "prod" && -z "${GLPI_API_KEY:-}" ]]; then
  warn "GLPI_API_KEY vazia com SPRING_PROFILES_ACTIVE=prod — defina uma chave (openssl rand -hex 32) antes de expor na rede."
fi

if [[ "${GLPI_API_BASE_URL:-}" == *"localhost"* && "${GLPI_API_BASE_URL:-}" != *"host.docker.internal"* ]]; then
  warn "GLPI_API_BASE_URL aponta para localhost. Dentro do Docker use o IP do servidor GLPI ou host.docker.internal."
fi

info "Build da imagem…"
docker compose build

info "Subindo container…"
docker compose up -d

PORT="${SERVER_PORT:-8081}"
info "Aguardando health (porta ${PORT})…"
for i in $(seq 1 30); do
  if curl -sf "http://127.0.0.1:${PORT}/actuator/health" >/dev/null 2>&1; then
    info "Aplicação no ar: http://$(hostname -I | awk '{print $1}'):${PORT}/"
    info "Interface web: http://127.0.0.1:${PORT}/"
    curl -sf "http://127.0.0.1:${PORT}/actuator/health" || true
    echo ""
    exit 0
  fi
  sleep 2
done

warn "Health ainda não respondeu. Verifique: docker compose logs -f"
exit 1
