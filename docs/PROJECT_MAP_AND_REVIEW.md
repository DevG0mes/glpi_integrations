# Mapeamento do Projeto e Review

Gerado em 2026-07-01 com foco no fluxo `CSV/XLSX -> parser -> resolução de campos -> payload PUT/POST -> GLPI`.

## Fluxo do campo `vencimento` no Chip

1. Template expõe a coluna `Vencimento` em [`src/main/resources/static/app/templates/template_chip.csv`](../src/main/resources/static/app/templates/template_chip.csv).
2. Leitura da planilha preserva o valor textual em [`src/main/java/com/devgomes/glpi_integration/sync/CustomAssetSpreadsheetReader.java`](../src/main/java/com/devgomes/glpi_integration/sync/CustomAssetSpreadsheetReader.java).
3. Mapeamento do ativo `chip` associa `vencimento -> custom_vencimento` com tipo `DATE` em [`src/main/java/com/devgomes/glpi_integration/config/GlpiCustomAssetsProperties.java`](../src/main/java/com/devgomes/glpi_integration/config/GlpiCustomAssetsProperties.java).
4. Normalização converte `DD/MM/YYYY` para `YYYY-MM-DD` em [`src/main/java/com/devgomes/glpi_integration/sync/SyncFieldResolver.java`](../src/main/java/com/devgomes/glpi_integration/sync/SyncFieldResolver.java).
5. Sincronização envia o mapa para `GlpiIntegrationService.updateItem(...)` via [`src/main/java/com/devgomes/glpi_integration/sync/GenericItemSyncBatchService.java`](../src/main/java/com/devgomes/glpi_integration/sync/GenericItemSyncBatchService.java).
6. Antes do PUT, o serviço filtrava o payload comparando as chaves do `GET` do item com as chaves propostas. Quando o GLPI não devolvia `custom_vencimento` no `GET`, o campo era descartado antes da gravação.

## Causa raiz observada

- O parser e a normalização de data já estavam corretos.
- O gargalo estava no filtro de payload em [`src/main/java/com/devgomes/glpi_integration/service/GlpiIntegrationService.java`](../src/main/java/com/devgomes/glpi_integration/service/GlpiIntegrationService.java), que chama `GlpiFieldFilter.retainFieldsKnownToItem(...)`.
- O filtro removia campos ausentes no `GET`, inclusive `custom_*`, embora alguns ativos customizados do GLPI aceitem esses campos no `PUT` mesmo sem expô-los na leitura.

## Ajuste aplicado

- [`src/main/java/com/devgomes/glpi_integration/sync/GlpiFieldFilter.java`](../src/main/java/com/devgomes/glpi_integration/sync/GlpiFieldFilter.java)
  - Agora preserva campos `custom_*` mesmo quando o `GET` do GLPI não os devolve.
- [`src/test/java/com/devgomes/glpi_integration/sync/GlpiFieldFilterTest.java`](../src/test/java/com/devgomes/glpi_integration/sync/GlpiFieldFilterTest.java)
  - Cobertura para `custom_vencimento` não ser descartado.
- [`src/test/java/com/devgomes/glpi_integration/sync/SyncFieldResolverTest.java`](../src/test/java/com/devgomes/glpi_integration/sync/SyncFieldResolverTest.java)
  - Cobertura para `vencimento` virar `custom_vencimento=2026-06-30`.

## Pastas e arquivos

### Raiz

- `.dockerignore` - exclusões do contexto Docker.
- `.env.example` - exemplo de variáveis de ambiente do serviço.
- `.gitattributes` - regras de atributos Git.
- `.gitignore` - arquivos ignorados no repositório.
- `docker-compose.yml` - composição local com contêineres.
- `Dockerfile` - imagem da aplicação.
- `HELP.md` - ajuda padrão gerada pelo Spring Initializr.
- `mvnw` - wrapper Maven para Unix.
- `mvnw.cmd` - wrapper Maven para Windows.
- `pom.xml` - dependências, plugins e build Maven.
- `README.md` - visão geral, setup e uso do projeto.
- `run-tests.ps1` - atalho PowerShell para execução de testes.

### `data`

- `data/README.md` - explica a finalidade do diretório de dados locais.

### `deploy/ubuntu`

- `deploy/ubuntu/deploy-docker.sh` - automação de deploy com Docker.
- `deploy/ubuntu/env.example` - exemplo de variáveis para deploy Ubuntu.
- `deploy/ubuntu/glpi-integration.service` - unit file do systemd.
- `deploy/ubuntu/install-docker.sh` - instalação de Docker no host.
- `deploy/ubuntu/install-systemd.sh` - instalação/habilitação do serviço systemd.
- `deploy/ubuntu/nginx-glpi-integration.conf` - configuração Nginx reversa.
- `deploy/ubuntu/verify.sh` - verificação pós-deploy.

### `docs`

- `docs/API_REFERENCE.md` - referência de endpoints.
- `docs/API_VALIDATION.md` - guia de validação/testes de API.
- `docs/CUSTOM_ASSETS.md` - documentação dos ativos customizados GLPI.
- `docs/DEPLOY_UBUNTU.md` - passo a passo de deploy Ubuntu.
- `docs/SECURITY.md` - práticas e decisões de segurança.
- `docs/SWAGGER_APIDOG.md` - uso de Swagger/Apidog.
- `docs/WEB_UI.md` - documentação da interface web.
- `docs/templates/README.md` - instruções dos templates.
- `docs/PROJECT_MAP_AND_REVIEW.md` - este mapeamento e review.

### `src/main/java/com/devgomes/glpi_integration`

- `GlpiIntegrationApplication.java` - bootstrap Spring Boot.

### `src/main/java/com/devgomes/glpi_integration/client`

- `GlpiApiClient.java` - cliente HTTP para `initSession`, listagem, GET, PUT e POST no GLPI.
- `GlpiApiErrorParser.java` - interpreta erros brutos do GLPI em mensagens legíveis.
- `GlpiApiException.java` - exceção de integração com status HTTP.
- `GlpiItemTypePath.java` - converte item types em segmentos de rota/mutation path.

### `src/main/java/com/devgomes/glpi_integration/config`

- `GlpiApiUrlResolver.java` - resolve/normaliza URL base do GLPI.
- `GlpiClientConfig.java` - instancia/configura cliente HTTP.
- `GlpiCustomAssetsProperties.java` - catálogo padrão dos ativos customizados e mapeamento de colunas.
- `GlpiProperties.java` - propriedades principais de conexão com GLPI.
- `GlpiSecurityProperties.java` - propriedades de segurança da API local.
- `GlpiStartupLogger.java` - logs de inicialização/configuração.
- `GlpiSyncProperties.java` - propriedades de sincronização, range e throttle.
- `OpenApiConfig.java` - configuração Swagger/OpenAPI.
- `SchedulingConfig.java` - agendamento global.
- `SecurityWebConfig.java` - configuração Spring Security/web.

### `src/main/java/com/devgomes/glpi_integration/dto`

- `ColaboradorWriteRequest.java` - request de criação/atualização de colaborador.
- `ComputerListResponse.java` - resposta de listagem de itens GLPI.
- `ComputerUpdateRequest.java` - request de atualização de `Computer`.
- `CustomAssetConfigItem.java` - DTO de configuração de ativo customizado.
- `IdNameItem.java` - DTO simples `id + nome`.

### `src/main/java/com/devgomes/glpi_integration/health`

- `GlpiHealthIndicator.java` - healthcheck da integração GLPI.

### `src/main/java/com/devgomes/glpi_integration/security`

- `ApiKeyAuthenticationFilter.java` - autenticação por API key da aplicação.
- `SecurityHeadersFilter.java` - cabeçalhos defensivos HTTP.

### `src/main/java/com/devgomes/glpi_integration/service`

- `CustomAssetItemTypeDiscoveryService.java` - descobre o `itemType` correto de ativos customizados no GLPI.
- `CustomAssetItemWriteService.java` - cria/atualiza ativos customizados via JSON.
- `CustomAssetUpdateProbeService.java` - testa atualizações campo a campo para diagnóstico.
- `GlpiAssetDefinitionCatalog.java` - lista definições de ativo do GLPI e faz o vínculo com `assetKey`.
- `GlpiCustomAssetItemTypeCandidates.java` - gera candidatos de `itemType` para discovery.
- `GlpiIntegrationService.java` - fachada principal de sessão, lookup, listagem, GET, PUT e POST no GLPI.

### `src/main/java/com/devgomes/glpi_integration/session`

- `GlpiSessionManager.java` - controle do ciclo de vida da sessão GLPI.

### `src/main/java/com/devgomes/glpi_integration/sync`

- `AssetSpreadsheetReader.java` - leitor de planilhas para o fluxo de `Computer`.
- `AssetTypeRegistry.java` - resolve definições de ativos customizados por `assetKey`.
- `AssetUpdateRow.java` - linha tipada do fluxo de `Computer`.
- `CsvFormatSupport.java` - leitura CSV com suporte a formatos/separadores.
- `CustomAssetNaturalKeySupport.java` - regras de chave natural para ativos customizados.
- `CustomAssetRow.java` - linha tipada do fluxo de ativos customizados.
- `CustomAssetSpreadsheetReader.java` - parser CSV/XLSX de ativos customizados.
- `CustomAssetTargetResolver.java` - decide create/update por `id_ativo` ou chave natural.
- `GenericItemSyncBatchService.java` - orquestra sync/validate de ativos customizados.
- `GlpiFieldFilter.java` - filtra/preserva campos antes do PUT no GLPI.
- `ResolvedAssetTarget.java` - resultado da resolução de create/update.
- `ScheduledComputerSyncJob.java` - job agendado do fluxo de computadores.
- `SensitiveDataMasker.java` - mascara campos sensíveis em relatórios/logs.
- `SyncFieldResolver.java` - converte valores da planilha em payload GLPI.
- `SyncLineResult.java` - resultado por linha de sync.
- `SyncLookupIndexes.java` - índices auxiliares de usuários, estados, locais, grupos e fabricantes.
- `SyncReport.java` - relatório agregado da execução.
- `ComputerSyncBatchService.java` - orquestra sync/validate do fluxo `Computer`.

### `src/main/java/com/devgomes/glpi_integration/web`

- `ColaboradorController.java` - endpoints específicos de colaborador.
- `ComputerController.java` - endpoints REST de `Computer`.
- `ComputerModelController.java` - endpoints para modelos de computador.
- `CustomAssetController.java` - endpoints de ativos customizados, probe, config e GET bruto.
- `GlpiApiExceptionHandler.java` - tratamento central de exceções HTTP.
- `GlpiConfigController.java` - endpoints de configuração/diagnóstico.
- `LookupSummaryController.java` - endpoints de lookup resumidos.
- `SecurityStatusController.java` - status da camada de segurança.
- `SyncController.java` - upload e execução de sync/validate via planilha.
- `WebUiController.java` - entrega da interface web estática.

### `src/main/resources`

- `application.properties` - propriedades base do Spring.
- `application.yml` - configuração principal do app e dos ativos.
- `application-local.properties.example` - exemplo de configuração local.
- `application-prod.yml` - configuração para produção.

### `src/main/resources/static/app`

- `css/app.css` - estilos da interface web.
- `index.html` - shell HTML da UI.
- `js/api.js` - cliente JS para chamadas HTTP da UI.
- `js/app.js` - lógica principal da interface.
- `js/docs.js` - renderização de documentação/ajuda dentro da UI.
- `templates/template_celular.csv` - template CSV de Celular.
- `templates/template_chip.csv` - template CSV de Chip.
- `templates/template_colaborador.csv` - template CSV de Colaborador.
- `templates/template_computers.csv` - template CSV de Computers.
- `templates/template_starlink.csv` - template CSV de Starlink.

### `src/test/java/com/devgomes/glpi_integration`

- `GlpiIntegrationApplicationTests.java` - smoke test de contexto Spring.

### `src/test/java/com/devgomes/glpi_integration/client`

- `GlpiApiClientInputTest.java` - valida enriquecimento do payload do cliente GLPI.
- `GlpiApiClientTest.java` - testes HTTP do cliente GLPI.
- `GlpiApiErrorParserTest.java` - testes de parsing de erro.
- `GlpiItemTypePathTest.java` - testes de codificação/segmentação de item types.

### `src/test/java/com/devgomes/glpi_integration/config`

- `GlpiApiUrlResolverTest.java` - testes de resolução de URL do GLPI.

### `src/test/java/com/devgomes/glpi_integration/dto`

- `ComputerUpdateRequestTest.java` - testes do DTO de atualização de computador.

### `src/test/java/com/devgomes/glpi_integration/security`

- `ApiKeyAuthenticationFilterTest.java` - testes do filtro de API key.

### `src/test/java/com/devgomes/glpi_integration/service`

- `CustomAssetItemWriteServiceTest.java` - testes do serviço JSON de escrita de ativos customizados.
- `GlpiCustomAssetItemTypeCandidatesTest.java` - testes dos candidatos de discovery.
- `GlpiIntegrationServiceTest.java` - testes da fachada de integração com GLPI.

### `src/test/java/com/devgomes/glpi_integration/session`

- `GlpiSessionManagerTest.java` - testes do gerenciador de sessão.

### `src/test/java/com/devgomes/glpi_integration/sync`

- `AssetSpreadsheetReaderTest.java` - testes do parser de planilha de `Computer`.
- `ComputerSyncBatchServiceTest.java` - testes do batch de `Computer`.
- `CsvFormatSupportTest.java` - testes de parsing CSV.
- `CustomAssetNaturalKeySupportTest.java` - testes de aliases/chave natural dos ativos customizados.
- `CustomAssetSpreadsheetReaderTest.java` - testes do parser de planilha de ativos customizados.
- `CustomAssetTargetResolverTest.java` - testes da decisão create/update por alvo.
- `GenericItemSyncBatchServiceTest.java` - testes do batch genérico de ativos customizados.
- `GlpiFieldFilterTest.java` - testes do filtro de payload do PUT.
- `SensitiveDataMaskerTest.java` - testes da máscara de campos sensíveis.
- `SyncFieldResolverTest.java` - testes da resolução de campos e datas.

### `src/test/java/com/devgomes/glpi_integration/templates`

- `SpreadsheetTemplatesGeneratorTest.java` - geração/verificação de templates XLSX/CSV.

### `src/test/java/com/devgomes/glpi_integration/web`

- `ComputerControllerTest.java` - testes web do controller de `Computer`.

### `src/test/resources`

- `application-test.yml` - configuração de testes.
- `sample-celular.csv` - amostra de CSV para Celular.
- `sample-chip.csv` - amostra de CSV para Chip.
- `sample-computers-semicolon.csv` - amostra de CSV de computadores com `;`.
- `sample-computers.csv` - amostra de CSV de computadores.
- `sample-starlink.csv` - amostra de CSV para Starlink.

## Observações do review

- O fluxo de `Chip` está bem separado por responsabilidade: configuração em `config`, parsing em `sync`, IO GLPI em `service/client`, exposição em `web`.
- O projeto tem boa base de testes de unidade para parsing, resolução de campos e cliente HTTP.
- Existe hoje uma quebra global de compilação nos testes, independente do ajuste do `vencimento`, que impede validação completa por `mvn test`.
