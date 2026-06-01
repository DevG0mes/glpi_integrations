#!/usr/bin/env bash
# Instala glpi-integration como serviço systemd (JAR, sem Docker).
# Uso: sudo ./deploy/ubuntu/install-systemd.sh
set -euo pipefail

if [[ "${EUID:-}" -ne 0 ]]; then
  echo "Execute com sudo." >&2
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

JAR="$(ls -1 target/glpi-integration-*.jar 2>/dev/null | head -1 || true)"
if [[ -z "$JAR" ]]; then
  echo "JAR não encontrado. Compile antes:"
  echo "  ./mvnw -B package -DskipTests"
  exit 1
fi

apt-get update
apt-get install -y openjdk-21-jre-headless curl

id -u glpi-integration >/dev/null 2>&1 || useradd --system --home /opt/glpi-integration --shell /usr/sbin/nologin glpi-integration

install -d -o glpi-integration -g glpi-integration /opt/glpi-integration
install -d -o glpi-integration -g glpi-integration /var/lib/glpi-integration/data
install -m 644 "$JAR" /opt/glpi-integration/app.jar
chown glpi-integration:glpi-integration /opt/glpi-integration/app.jar

install -d -m 750 /etc/glpi-integration
if [[ ! -f /etc/glpi-integration/env ]]; then
  install -m 600 deploy/ubuntu/env.example /etc/glpi-integration/env
  echo "Edite: sudo nano /etc/glpi-integration/env"
fi

install -m 644 deploy/ubuntu/glpi-integration.service /etc/systemd/system/glpi-integration.service
systemctl daemon-reload
systemctl enable glpi-integration

echo ""
echo "Instalado em /opt/glpi-integration/app.jar"
echo "Configure tokens: sudo nano /etc/glpi-integration/env"
echo "Inicie: sudo systemctl start glpi-integration"
echo "Status: sudo systemctl status glpi-integration"
echo "Logs:   journalctl -u glpi-integration -f"
