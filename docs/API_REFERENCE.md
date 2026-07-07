# Referência da API — GLPI Integration

Middleware Spring Boot na porta **8081** (padrão). Base: `http://localhost:8081`

Todas as rotas abaixo são deste projeto. O GLPI em si é chamado internamente via `apirest.php` (legado).

**Documentação interativa:** [SWAGGER_APIDOG.md](SWAGGER_APIDOG.md) — Swagger UI (`/swagger-ui.html`) e importação no Apidog (`/v3/api-docs`).

---

## Métodos HTTP — o que significam

| Método | Significado | Uso típico |
|--------|-------------|------------|
| **GET** | **Ler** dados sem alterar nada | Listar, consultar um item, validar configuração, health check |
| **POST** | **Criar** recurso ou **executar ação** | Upload de planilha, sync em lote, diagnóstico, criar item no GLPI (via sync) |
| **PUT** | **Substituir / atualizar** um recurso existente | Atualizar um Computer ou item customizado por id (JSON no body) |
| **PATCH** | Atualização **parcial** (só alguns campos) | *Não exposto* neste middleware; o GLPI legado aceita PUT |
| **DELETE** | **Remover** um recurso | *Não exposto* neste middleware |

**Neste projeto:** só existem rotas **GET**, **POST** e **PUT**. Não há DELETE nem PATCH expostos na API do middleware.

---

## Sistema

| Método | Rota | Para que serve |
|--------|------|----------------|
| GET | `/actuator/health` | Verifica se a aplicação está no ar (Spring Actuator). |

---

## Consultas auxiliares (lookup)

Listas **id + nome** (ou id + login) do GLPI para montar planilhas e resolver textos → IDs no sync.

| Método | Rota | Query (opcional) | Para que serve |
|--------|------|------------------|----------------|
| GET | `/api/users/summary` | `range=0-999` | Usuários: id e login (coluna `responsavel` nas planilhas). |
| GET | `/api/states/summary` | `range=0-999` | Status: id e nome (coluna `status` no Chip). |
| GET | `/api/locations/summary` | `range=0-999` | Localidades GLPI nativas (dropdown `locations_id` em Computer). |
| GET | `/api/groups/summary` | `range=0-999` | Grupos: id e nome. |
| GET | `/api/computer-types/summary` | `range=0-999` | Tipos de computer. |
| GET | `/api/manufacturers/summary` | `range=0-999` | Fabricantes. |
| GET | `/api/computer-models/summary` | `range=0-999` | Modelos de computer (id + nome). |
| GET | `/api/computer-models` | `range=0-999` | Modelos com **todos os campos** retornados pelo GLPI. |

---

## Computer (equipamento nativo GLPI)

| Método | Rota | Body / query | Para que serve |
|--------|------|--------------|----------------|
| GET | `/api/computers/summary` | `range=0-999` | Lista resumida: id + nome de cada Computer. |
| GET | `/api/computers` | `range`, `expandDropdowns=false` | Lista completa de Computers (JSON bruto GLPI). |
| GET | `/api/computers/report` | `range=0-999` | Relatório enriquecido com garantias relacionadas por patrimônio e serial. |
| GET | `/api/computers/{id}` | `expandDropdowns=false` | Um Computer por id. |
| PUT | `/api/computers/{id}` | JSON `ComputerUpdateRequest` | Atualiza **um** Computer (campos como `users_id`, `name`, `serial`, etc.). |

Exemplo PUT:

```http
PUT /api/computers/42
Content-Type: application/json

{
  "usersId": 96,
  "name": "PC-001",
  "comment": "Atualizado via API"
}
```

---

## Ativos customizados — rotas gerais

Válidas para `starlink`, `chip`, `celular`, `colaborador` e `garantia` onde aparece `{assetKey}`.

| Método | Rota | Para que serve |
|--------|------|----------------|
| GET | `/api/custom-assets/discover` | Testa itemtypes GLPI 11 para Starlink, Chip, Celular, Colaborador e Garantia; sugere linhas para `application-local.properties`. |
| GET | `/api/custom-assets/definitions` | Lista definições de ativo cadastradas no GLPI (`system_name`, rótulo). |
| GET | `/api/custom-assets/config` | Configuração atual do middleware: itemtypes, colunas da planilha → campos GLPI. |
| GET | `/api/custom-assets/{assetKey}/discover` | Descobre itemtype de **um** ativo (`starlink`, `chip`, `celular`, `colaborador` ou `garantia`). |
| GET | `/api/custom-assets/{assetKey}/discover?systemName=Starlink` | Mesmo discover com nome do sistema informado manualmente. |
| GET | `/api/custom-assets/{assetKey}/summary` | `range=0-999` — id + nome dos itens já cadastrados no GLPI. |
| GET | `/api/custom-assets/{assetKey}/probe` | Testa se o itemtype configurado responde (lista 0-0). |
| GET | `/api/custom-assets/{assetKey}/items/{itemId}` | Item completo no GLPI + lista `customFieldKeys` (nomes API dos campos custom). |
| POST | `/api/custom-assets/{assetKey}/items/{itemId}/update-probe` | JSON opcional — testa PUT campo a campo (diagnóstico de `ERROR_GLPI_UPDATE`). |

`{assetKey}`: `starlink` | `chip` | `celular` | `colaborador` | `garantia`

---

## Starlink — rotas por tipo

### Consulta e diagnóstico

| Método | Rota | Para que serve |
|--------|------|----------------|
| GET | `/api/custom-assets/starlink/summary` | Lista antenas Starlink (id + nome). |
| GET | `/api/custom-assets/starlink/items/{id}` | Detalhe de uma antena; ver campos `custom_*` válidos. |
| GET | `/api/custom-assets/starlink/probe` | Testa conectividade do itemtype Starlink. |
| GET | `/api/custom-assets/starlink/discover` | Descobre itemtype correto no seu GLPI. |
| POST | `/api/custom-assets/starlink/items/{id}/update-probe` | Testa atualização campo a campo. |

### Sincronização por planilha

| Método | Rota | Body | Para que serve |
|--------|------|------|----------------|
| POST | `/api/sync/starlink/validate` | `multipart/form-data`, campo **`file`** (CSV/XLSX) | **Dry-run:** mostra o que seria criado/atualizado, **sem gravar** no GLPI. |
| POST | `/api/sync/starlink` | `multipart/form-data`, campo **`file`** | **Sincroniza:** cria ou atualiza linhas no GLPI (PUT/POST no GLPI). |

**Colunas da planilha:** `id_ativo`, `nome`, `projeto`, `responsavel`, `email`, `senha_conta`, `senha_roteador`, `localidade`

**Chave natural:** `nome` (campo GLPI `name`). Ver [CUSTOM_ASSETS.md](CUSTOM_ASSETS.md).

---

## Chip — rotas por tipo

### Consulta e diagnóstico

| Método | Rota | Para que serve |
|--------|------|----------------|
| GET | `/api/custom-assets/chip/summary` | Lista chips (id + identificador). |
| GET | `/api/custom-assets/chip/items/{id}` | Detalhe de um chip. |
| GET | `/api/custom-assets/chip/probe` | Testa itemtype Chip. |
| GET | `/api/custom-assets/chip/discover` | Descobre itemtype Chip. |
| POST | `/api/custom-assets/chip/items/{id}/update-probe` | Diagnóstico de PUT. |

### Sincronização por planilha

| Método | Rota | Body | Para que serve |
|--------|------|------|----------------|
| POST | `/api/sync/chip/validate` | `file` = CSV/XLSX | Valida planilha sem gravar. |
| POST | `/api/sync/chip` | `file` = CSV/XLSX | Sincroniza chips (criar/atualizar). |

**Colunas:** `id_ativo`, `iccid`, `nome` (opcional), `numero`, `responsavel`, `status`, `vencimento`

**Chave natural:** `iccid` → `custom_iccid`

**Data de vencimento:** `vencimento` aceita `YYYY-MM-DD` ou `DD/MM/YYYY` e é enviado ao GLPI como `YYYY-MM-DD`.

---

## Celular — rotas por tipo

### Consulta e diagnóstico

| Método | Rota | Para que serve |
|--------|------|----------------|
| GET | `/api/custom-assets/celular/summary` | Lista celulares. |
| GET | `/api/custom-assets/celular/items/{id}` | Detalhe de um aparelho. |
| GET | `/api/custom-assets/celular/probe` | Testa itemtype Celular. |
| GET | `/api/custom-assets/celular/discover` | Descobre itemtype Celular. |
| POST | `/api/custom-assets/celular/items/{id}/update-probe` | Diagnóstico de PUT. |

### Sincronização por planilha

| Método | Rota | Body | Para que serve |
|--------|------|------|----------------|
| POST | `/api/sync/celular/validate` | `file` = CSV/XLSX | Valida planilha sem gravar. |
| POST | `/api/sync/celular` | `file` = CSV/XLSX | Sincroniza celulares (criar/atualizar). |

**Colunas:** `id_ativo`, `nome`, `imei`, `modelo`, `responsavel`

**Chave natural:** `imei` → `custom_imei`

---

## Computer — sincronização por planilha

| Método | Rota | Body | Para que serve |
|--------|------|------|----------------|
| POST | `/api/sync/computers/validate` | `file` = CSV/XLSX | Dry-run da planilha de Computers. |
| POST | `/api/sync/computers` | `file` = CSV/XLSX | Sincroniza Computers no GLPI: atualiza por `id_ativo`/nome quando existir e cria quando não existir. |
| POST | `/api/sync/computers/run` | — | Sync usando caminho fixo em `glpi.sync.input-path` (config). |

**Regra do `id_ativo` em Computers:** se o ID existir no GLPI, a linha é atualizada; se o ID informado não existir, um novo Computer é criado. Se `id_ativo` vier textual, ele é tratado como nome do Computer: atualiza quando encontra e cria quando não encontra.

**Mapeamento de patrimônio em Computers:** as colunas `nome`, `ATIVO`, `patrimonio` e `numero_patrimonio` são aceitas como origem do campo `name` no GLPI.

**Campos opcionais adicionais em Computers:** `vencimento_garantia` e `cod_mega`. O campo `vencimento_garantia` aceita `YYYY-MM-DD`, `DD/MM/YYYY`, `YYYY-MM-DD HH:mm[:ss]` ou `DD/MM/YYYY HH:mm[:ss]` e é enviado ao GLPI como `YYYY-MM-DD HH:mm:ss`. O campo `cod_mega` é texto livre.

**Relatório enriquecido de Computers:** `GET /api/computers/report` cruza `Computer.name` com o patrimônio da garantia (`name`) e `Computer.serial` com `custom_numero_de_serie`, retornando para cada equipamento as garantias relacionadas com `Status`, `Vencimento Garantia`, `Custo`, `NFS` e `Modelo Garantia`.

---

## Garantia — sincronização por planilha

| Método | Rota | Body | Para que serve |
|--------|------|------|----------------|
| POST | `/api/sync/garantia/validate` | `file` = CSV/XLSX | Dry-run da planilha de Garantias. |
| POST | `/api/sync/garantia` | `file` = CSV/XLSX | Sincroniza Garantias no GLPI: atualiza por `nome` quando existir e cria quando não existir. |

**Colunas esperadas em Garantia:** `Nome`, `Status`, `Vencimento Garantia`, `Numero de Serie`, `Custo`, `NFS`, `Modelo Garantia`.

---

## Upload de planilha (todas as rotas POST /api/sync/*)

| Item | Valor |
|------|--------|
| Content-Type | `multipart/form-data` |
| Campo obrigatório | `file` |
| Formatos | `.csv` (separador `;` ou `,`) ou `.xlsx` / `.xls` |

Exemplo (Insomnia / curl):

```bash
curl -X POST "http://localhost:8081/api/sync/starlink/validate" \
  -F "file=@docs/templates/template_starlink.xlsx"
```

---

## Resposta típica do sync (`SyncReport`)

```json
{
  "source": "caminho-do-arquivo",
  "total": 2,
  "successCount": 2,
  "failureCount": 0,
  "lines": [
    {
      "lineNumber": 2,
      "glpiId": 3,
      "success": true,
      "message": "OK (criado id=3)"
    },
    {
      "lineNumber": 3,
      "glpiId": 1,
      "success": true,
      "message": "OK (atualizado id=1, id_ativo=1)"
    }
  ]
}
```

---

## Fluxo recomendado

```mermaid
flowchart LR
  A[GET summary / discover] --> B[Montar planilha]
  B --> C[POST validate]
  C --> D{OK?}
  D -->|sim| E[POST sync]
  D -->|não| B
```

1. **GET** `/api/custom-assets/{tipo}/summary` ou lookups — conferir ids e nomes no GLPI.  
2. **POST** `.../validate` — simular alterações.  
3. **POST** sync definitivo — gravar no GLPI.  

Se PUT falhar: **GET** `items/{id}` e **POST** `update-probe`.

---

## Documentação relacionada

- [CUSTOM_ASSETS.md](CUSTOM_ASSETS.md) — campos, permissões GLPI, criar vs atualizar  
- [API_VALIDATION.md](API_VALIDATION.md) — testar GLPI direto com curl  
- [SECURITY.md](SECURITY.md) — tokens e senhas  
- [README.md](../README.md) — como subir o projeto  
