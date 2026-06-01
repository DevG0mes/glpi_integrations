#!/usr/bin/env bash
# Instala Docker Engine + Compose plugin no Ubuntu (22.04 / 24.04).
# Uso: sudo ./deploy/ubuntu/install-docker.sh
set -euo pipefail

if [[ "${EUID:-}" -ne 0 ]]; then
  echo "Execute com sudo." >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg

install -m 0755 -d /etc/apt/keyrings
if [[ ! -f /etc/apt/keyrings/docker.gpg ]]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
fi

ARCH="$(dpkg --print-architecture)"
CODENAME="$(. /etc/os-release && echo "${VERSION_CODENAME:-$VERSION_ID}")"
echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${CODENAME} stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable docker
systemctl start docker

echo ""
echo "Docker instalado: $(docker --version)"
echo "Compose: $(docker compose version)"
echo ""
echo "Para rodar deploy sem sudo, adicione seu usuário ao grupo docker:"
echo "  sudo usermod -aG docker \$USER"
echo "  (faça logout/login depois)"
