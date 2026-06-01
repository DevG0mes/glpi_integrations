# Deploy no Ubuntu — GLPI Integration

Guia para rodar o middleware em servidor **Ubuntu 22.04 / 24.04** (recomendado: **Docker**).

## Pré-requisitos

| Item | Detalhe |
|------|---------|
| Ubuntu | 22.04 LTS ou 24.04 LTS |
| Rede | Servidor Ubuntu alcança o GLPI (`GLPI_API_BASE_URL`) |
| GLPI | API REST habilitada, App-Token e User-Token |
| Porta | **8081** liberada no firewall (ou atrás de Nginx) |

## Opção A — Docker (recomendado)

### 1. Clonar o projeto no servidor

```bash
cd /opt
sudo git clone https://github.com/DevG0mes/glpi_integrations.git glpi-integration
sudo chown -R "$USER:$USER" glpi-integration
cd glpi-integration
```

### 2. Instalar Docker (uma vez)

```bash
chmod +x deploy/ubuntu/*.sh
sudo ./deploy/ubuntu/install-docker.sh
# logout/login se adicionou usuário ao grupo docker
```

### 3. Configurar ambiente

```bash
cp .env.example .env
nano .env
```

Preencha obrigatoriamente:

```env
GLPI_API_BASE_URL=http://192.168.1.98/apirest.php
GLPI_APP_TOKEN=seu_app_token
GLPI_USER_TOKEN=seu_user_token
GLPI_API_KEY=$(openssl rand -hex 32)
SPRING_PROFILES_ACTIVE=prod
```

Guarde `GLPI_API_KEY` — use no botão **Chave API** da interface web ou no header `X-API-Key` em integrações.

**GLPI na mesma máquina que o Docker:**

```env
GLPI_API_BASE_URL=http://host.docker.internal/apirest.php
```

(O `docker-compose.yml` já define `extra_hosts: host.docker.internal`.)

**Itemtypes GLPI 11** (ajuste conforme seu ambiente):

```env
GLPI_ITEMTYPE_STARLINK=Glpi\Asset\AssetDefinition/Starlink
GLPI_ITEMTYPE_CHIP=Glpi\Asset\AssetDefinition/Chip
GLPI_ITEMTYPE_CELULAR=Glpi\Asset\AssetDefinition/Celular
```

### 4. Subir a aplicação

```bash
./deploy/ubuntu/deploy-docker.sh
```

### 5. Verificar

```bash
./deploy/ubuntu/verify.sh
# Interface web: http://IP_DO_SERVIDOR:8081/
```

### Comandos úteis

```bash
docker compose logs -f
docker compose restart
docker compose down
docker compose build --no-cache && docker compose up -d
```

### Planilhas no servidor

Coloque CSV/XLSX em `./data/` (montado em `/data` no container):

```bash
curl -X POST "http://localhost:8081/api/sync/starlink" \
  -F "file=@data/planilha.csv"
```

Ou use a **interface web** em `http://servidor:8081/`.

---

## Opção B — Systemd (JAR, sem Docker)

### 1. Java 21 + build

```bash
sudo apt-get install -y openjdk-21-jdk-headless
cd /opt/glpi-integration
chmod +x mvnw
./mvnw -B package -DskipTests
```

### 2. Instalar serviço

```bash
sudo ./deploy/ubuntu/install-systemd.sh
sudo nano /etc/glpi-integration/env
sudo systemctl start glpi-integration
sudo systemctl status glpi-integration
```

Variáveis em `/etc/glpi-integration/env` (mesmos nomes do `.env.example`).

Logs: `journalctl -u glpi-integration -f`

---

## Firewall (UFW)

```bash
sudo ufw allow 8081/tcp comment 'GLPI Integration API'
sudo ufw reload
```

Para expor só via Nginx na porta 443, **não** abra 8081 publicamente; use proxy reverso local.

---

## Nginx (opcional, HTTPS)

Exemplo `/etc/nginx/sites-available/glpi-integration`:

```nginx
server {
    listen 80;
    server_name glpi-api.seudominio.com.br;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 25M;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/glpi-integration /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

Use Certbot para TLS se necessário.

---

## Checklist pós-deploy

| Verificação | Como |
|-------------|------|
| API no ar | `curl http://localhost:8081/actuator/health` |
| Tokens configurados | `curl http://localhost:8081/api/glpi/connection-info` → `credentialsPresent: true` |
| GLPI alcançável | Painel web → **Painel** → probes Starlink/Chip/Celular |
| Itemtypes corretos | `GET /api/custom-assets/discover` ou tela **Painel** |
| Sync de teste | Interface **Sincronizar** → **Validar** antes de **Sincronizar** |

---

## Problemas comuns

| Sintoma | Solução |
|---------|---------|
| `ConnectException` / GLPI unreachable | `GLPI_API_BASE_URL` errada; do container use IP da rede ou `host.docker.internal` |
| `credentialsPresent: false` | Preencher `GLPI_APP_TOKEN` e `GLPI_USER_TOKEN` no `.env` e `docker compose up -d` de novo |
| Probe `reachable: false` | Permissões GLPI no perfil da API; itemtype incorreto — rode `/api/custom-assets/discover` |
| Porta 8081 em uso | Altere `SERVER_PORT` no `.env` (ex.: 8082) e `docker compose up -d` |
| Upload grande falha | Aumente `client_max_body_size` no Nginx |

---

## Atualizar versão

```bash
cd /opt/glpi-integration
git pull
./deploy/ubuntu/deploy-docker.sh
```

Documentação relacionada: [WEB_UI.md](WEB_UI.md), [SECURITY.md](SECURITY.md), [CUSTOM_ASSETS.md](CUSTOM_ASSETS.md).
