# GLPI Integration (Spring Boot)

Middleware para integração com o GLPI via API REST legada (`apirest.php` ou `api.php/v1`), com autenticação por tokens e atualização de ativos por ID numérico.

## Requisitos

- Java 21
- Maven 3.9+
- GLPI com API REST habilitada

## Configuração

1. Copie `src/main/resources/application-local.properties.example` para `application-local.properties`.
2. Defina URL, estilo de API e tokens:

```properties
glpi.api.style=LEGACY_APIREST
glpi.api.base-url=http://seu-servidor/glpi/apirest.php
glpi.api.app-token=SEU_APP_TOKEN
glpi.api.user-token=SEU_USER_TOKEN
glpi.test-on-startup=true
```

Ou via variáveis de ambiente: `GLPI_API_BASE_URL`, `GLPI_APP_TOKEN`, `GLPI_USER_TOKEN`, `GLPI_API_STYLE`.

3. Valide a API no seu servidor: [docs/API_VALIDATION.md](docs/API_VALIDATION.md)

**Segurança:** não commite tokens. Se já foram expostos, rotacione-os no GLPI.

## Fase 1 — Checklist de validação

1. Copiar `application-local.properties.example` → `application-local.properties` e preencher tokens.
2. Confirmar URL com [docs/API_VALIDATION.md](docs/API_VALIDATION.md) (`LEGACY_APIREST` ou `LEGACY_VIA_ROUTER`).
3. `.\mvnw.cmd test` — todos os testes devem passar.
4. `glpi.test-on-startup=true` e `.\mvnw.cmd spring-boot:run` — log deve mostrar sessão OK.
5. Testar `PUT` em um Computer de homologação (IDs numéricos conhecidos).

## Executar

```powershell
cd glpi-integration
.\mvnw.cmd test
# ou: .\run-tests.ps1
.\mvnw.cmd spring-boot:run
```

## Empacotar JAR

```powershell
.\mvnw.cmd -q package -DskipTests
java -jar target\glpi-integration-0.0.1-SNAPSHOT.jar
```

Com variáveis de ambiente no mesmo shell ou em `application-local.properties`.

## Sincronização em lote (planilha)

### Colunas do seu CSV (mapeamento automático)

| Coluna na planilha | Campo GLPI atualizado |
|--------------------|------------------------|
| Service TAG | `serial` |
| id_ativo | **ID numérico** do Computer no GLPI (ex.: `1558`) |
| id_model | `computermodels_id` (**principal — modelo do computer**) |
| RESPONSAVEL | login do usuário (ex.: `evellyn.cavalcante`) ou ID numérico; `null` = vazio |
| Status | nome do status (ex.: `Em uso`, `Estoque`, `Disponivel`) ou ID numérico |

Também aceita `id_ativo` como **name** textual (se não for número) ou coluna `glpi_id`.

Descubra o ID do modelo: `GET /api/computer-models/summary`

### Via API REST

```powershell
curl -X POST http://localhost:8080/api/sync/computers -F "file=@ativos.csv"
```

### Via arquivo configurado

```properties
glpi.sync.enabled=true
glpi.sync.input-path=C:\caminho\ativos.csv
```

Disparo manual: `POST http://localhost:8080/api/sync/computers/run`

## Health check

`GET http://localhost:8080/actuator/health` — verifica credenciais e `initSession` (sem expor tokens nos logs).

## Uso programático

```java
glpiIntegrationService.updateComputer(10, new ComputerUpdateRequest(
    42, null, null, 1, 11, null, null, null, "SN-123", null, null
));
```
