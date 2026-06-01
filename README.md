# GLPI Integration (Spring Boot)

Middleware para integração com o GLPI via API REST legada (`apirest.php`), com sincronização por planilha de **Computers** e ativos customizados (**Starlink**, **Chip**, **Celular**).

## Requisitos

- Java 21
- Maven 3.9+
- GLPI 11.x com API REST habilitada

## Configuração

1. Copie `src/main/resources/application-local.properties.example` → `application-local.properties`.
2. Preencha URL, tokens e (opcional) itemtypes customizados:

```properties
glpi.api.style=LEGACY_APIREST
glpi.api.base-url=http://seu-servidor/apirest.php
glpi.api.app-token=SEU_APP_TOKEN
glpi.api.user-token=SEU_USER_TOKEN
GLPI_ITEMTYPE_STARLINK=SEU_ITEMTYPE_Starlink
GLPI_ITEMTYPE_CHIP=SEU_ITEMTYPE_Chip
GLPI_ITEMTYPE_CELULAR=SEU_ITEMTYPE_Celular
```

Ou use `.env` (veja `.env.example`). **Não commite tokens.**

3. Ativos customizados no GLPI: [docs/CUSTOM_ASSETS.md](docs/CUSTOM_ASSETS.md)

## Executar

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Porta padrão: **8081** (`server.port` em `application.properties`).

## Docker

```powershell
copy .env.example .env
# Edite .env com GLPI_API_BASE_URL, GLPI_APP_TOKEN, GLPI_USER_TOKEN

docker compose build
docker compose up -d
curl http://localhost:8081/actuator/health
```

Planilhas em `./data` (montado read-only). Sync:

```powershell
curl.exe -X POST http://localhost:8081/api/sync/computers -F "file=@data/computers.csv"
```

## Deploy Ubuntu (servidor)

Guia completo: **[docs/DEPLOY_UBUNTU.md](docs/DEPLOY_UBUNTU.md)**

```bash
chmod +x deploy/ubuntu/*.sh
sudo ./deploy/ubuntu/install-docker.sh   # primeira vez
cp .env.example .env && nano .env
./deploy/ubuntu/deploy-docker.sh
./deploy/ubuntu/verify.sh
```

Interface: `http://IP_DO_SERVIDOR:8081/`

## Interface web

Painel operacional no navegador (upload de planilhas, inventário, consultas):

```text
http://localhost:8081/
```

Detalhes: [docs/WEB_UI.md](docs/WEB_UI.md)

## API

| Recurso | Uso |
|---------|-----|
| [docs/WEB_UI.md](docs/WEB_UI.md) | **Interface web** para operação no dia a dia |
| [docs/API_REFERENCE.md](docs/API_REFERENCE.md) | Todas as rotas em português (por ativo e método HTTP) |
| [docs/SWAGGER_APIDOG.md](docs/SWAGGER_APIDOG.md) | **Swagger UI + Apidog** — como começar |
| http://localhost:8081/swagger-ui.html | Testar API no navegador (com a app rodando) |
| http://localhost:8081/v3/api-docs | OpenAPI JSON (importar no Apidog) |

Resumo rápido:

| Área | Exemplos |
|------|----------|
| Saúde | `GET /actuator/health` |
| Lookups (ids para planilha) | `GET /api/users/summary`, `/api/states/summary`, … |
| Computer | `GET/PUT /api/computers`, sync `POST /api/sync/computers` |
| Starlink / Chip / Celular | `GET /api/custom-assets/{tipo}/…`, sync `POST /api/sync/{tipo}` |

Métodos expostos neste middleware: **GET**, **POST** e **PUT** (não há DELETE nem PATCH).

Detalhes de colunas, criar vs atualizar e permissões GLPI: [docs/CUSTOM_ASSETS.md](docs/CUSTOM_ASSETS.md). Planilhas modelo: `docs/templates/template_*.csv`.

## Health

`GET http://localhost:8081/actuator/health`

## Documentação

- [docs/API_REFERENCE.md](docs/API_REFERENCE.md) — **todas as rotas** (por ativo e por método HTTP)
- [docs/SWAGGER_APIDOG.md](docs/SWAGGER_APIDOG.md) — Swagger UI e importação Apidog
- [docs/CUSTOM_ASSETS.md](docs/CUSTOM_ASSETS.md) — Starlink, Chip, Celular (campos e permissões)
- [docs/API_VALIDATION.md](docs/API_VALIDATION.md) — testar API GLPI com curl
- [docs/SECURITY.md](docs/SECURITY.md) — tokens e senhas
- [docs/DEPLOY_UBUNTU.md](docs/DEPLOY_UBUNTU.md) — deploy em servidor Ubuntu (Docker / systemd)
