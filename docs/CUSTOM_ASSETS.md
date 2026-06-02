# Ativos customizados GLPI 11 — Starlink, Chip, Celular

## Objetivo

Três categorias de inventário que **não existem nativamente** no GLPI, modeladas como **definições de ativo** no GLPI 11 e sincronizadas via middleware.

## Passo 1 — Criar definições no GLPI (administração)

1. Acesse **Configuração → Ativos** (ou **Assets → Asset definitions** no GLPI 11).
2. Crie três definições:

| Nome no GLPI | Uso |
|--------------|-----|
| Starlink | Links satelitais |
| Chip | Chips de dados |
| Celular | Aparelhos móveis |

3. Para cada definição, cadastre os campos:

### Starlink

| Campo | Tipo GLPI sugerido |
|-------|-------------------|
| projeto | Texto |
| users_id | Usuário (responsável) |
| email | Texto |
| senha_conta_starlink | Senha / texto protegido |
| senha_roteador | Senha / texto protegido |
| locations_id | Localização |

Chave natural recomendada: campo `name` (ex. `STARLINK-{projeto}-{local}`).

### Chip

| Campo | Tipo |
|-------|------|
| iccid | Texto (único) |
| numero | Texto |
| users_id | Usuário |
| states_id | Status |

### Celular

| Campo | Tipo |
|-------|------|
| imei | Texto (único) |
| modelo | Texto |
| users_id | Usuário |

### Colaborador

| Campo | Tipo |
|-------|------|
| Nome | Texto (`name`) |
| Departamento | Texto |
| Email | Texto (chave natural) |
| Ativo | Texto |

**API dedicada:** `GET/POST /api/colaboradores`, `PUT /api/colaboradores/{id}`  
**Sync planilha:** `POST /api/sync/colaborador` (colunas: `nome`, `email`, `departamento`, `ativo`)

## Conferência com os formulários GLPI (campos criados)

Os rótulos na tela batem com o middleware:

| GLPI (tela) | Campo API | Planilha |
|-------------|-----------|----------|
| Nome | `name` | `nome` ou chave natural (Starlink) |
| projeto | `custom_projeto` | `projeto` |
| senha_conta_starlink | `custom_senha_conta_starlink` | `senha_conta` |
| senha_roteador | `custom_senha_roteador` | `senha_roteador` |
| Usuário | `users_id` | `responsavel` ou `usuario` |
| email | `custom_email` | `email` |
| Localização (texto) | `custom_localizacao` | `localidade` ou `localizacao` |
| iccid / numero / Status | `custom_iccid`, `custom_numero`, `states_id` | Chip |
| imei / modelo | `custom_imei`, `custom_modelo` | Celular |
| Departamento / Email / Ativo | `custom_departamento`, `custom_email`, `custom_ativo` | Colaborador |

Se o PUT falhar, use `GET /api/custom-assets/colaborador/items/{id}` e confira `customFieldKeys` — ajuste o mapeamento em `GlpiCustomAssetsProperties` se os nomes no GLPI forem diferentes.

## Passo 2 — Descobrir o `itemtype` na API

No **GLPI 11**, o `itemtype` dos **itens** (não da definição em si) segue o **nome do sistema** da definição:

| Nome do sistema (Configuração → Ativos) | Classe (documentação GLPI) | Itemtype na API legada |
|----------------------------------------|----------------------------|-------------------------|
| `Example` | `GlpiCustomAssetExampleAsset` | ver abaixo |
| `Starlink` | `GlpiCustomAssetStarlinkAsset` | ver abaixo |

**API legada GLPI 11:**

| Operação | Itemtype |
|----------|----------|
| Listar / descobrir | `Glpi\Asset\AssetDefinition/Starlink` |
| PUT / POST (sync) | `Glpi\CustomAsset\StarlinkAsset` (o middleware converte automaticamente) |

Listagem: `http://HOST/apirest.php/Glpi%5CAsset%5CAssetDefinition/Starlink/?range=0-0`

O rótulo na tela («Starlink») pode ser igual ao nome do sistema, mas **o que importa para a API é o nome do sistema** (campo imutável após criar a definição).

Após criar as definições, descubra o nome exato usado na API legada:

```powershell
$base = "http://SEU_SERVIDOR/apirest.php"
$appToken = "SEU_APP_TOKEN"
$userToken = "SEU_USER_TOKEN"

# Sessão
$session = (Invoke-RestMethod -Uri "$base/initSession/" -Headers @{
  "App-Token" = $appToken
  "Authorization" = "user_token $userToken"
}).session_token

# Listar opções de busca (ajuda a achar itemtypes)
Invoke-RestMethod -Uri "$base/listSearchOptions/Computer/" -Headers @{
  "App-Token" = $appToken
  "Session-Token" = $session
}
```

Teste listagem do tipo customizado (substitua `SEU_ITEMTYPE`):

```powershell
Invoke-RestMethod -Uri "$base/SEU_ITEMTYPE/?range=0-5" -Headers @{
  "App-Token" = $appToken
  "Session-Token" = $session
}
```

Configure no `.env` ou `application-local.properties` (ajuste o nome da classe conforme seu GLPI):

```properties
GLPI_ITEMTYPE_STARLINK=Glpi\\Asset\\AssetDefinition/Starlink
GLPI_ITEMTYPE_CHIP=Glpi\\Asset\\AssetDefinition/Chip
GLPI_ITEMTYPE_CELULAR=Glpi\\Asset\\AssetDefinition/Celular
```

**Atalho no middleware** (com a app rodando):

1. `GET /api/custom-assets/definitions` — lista definições do GLPI (`system_name`, rótulo)
2. `GET /api/custom-assets/discover` — testa candidatos (prioriza `Glpi\Asset\AssetDefinition/{system_name}`)
3. `GET /api/custom-assets/starlink/discover?systemName=SEU_NOME` — testa um nome do sistema informado manualmente
4. `GET /api/custom-assets/config` — mostra itemtypes atuais (`configured: false` = ainda placeholder)
3. `GET /api/custom-assets/starlink/probe` — testa o itemtype **já configurado**
4. `GET /api/custom-assets/starlink/summary` — lista itens cadastrados

Enquanto `configured` for `false`, adicione em `application-local.properties`:

```properties
GLPI_ITEMTYPE_STARLINK=Glpi\\Asset\\AssetDefinition/Starlink
GLPI_ITEMTYPE_CHIP=Glpi\\Asset\\AssetDefinition/Chip
GLPI_ITEMTYPE_CELULAR=Glpi\\Asset\\AssetDefinition/Celular
```

(use o valor exato retornado por `/discover`, não o exemplo acima)

Lista completa de rotas (GET/POST/PUT): [API_REFERENCE.md](API_REFERENCE.md).

## Passo 3 — Planilhas e endpoints

### Starlink

Colunas: `id_ativo` (opcional), `nome`, `projeto`, `responsavel`, `email`, `senha_conta`, `senha_roteador`, `localidade`

| Coluna planilha | Campo GLPI | Notas |
|-----------------|------------|--------|
| `id_ativo` | — | Se preenchido, **sempre atualiza** esse id (ex. `1` = antena já cadastrada) |
| `nome` | `name` | Chave natural; sem `id_ativo`, se o nome **não existir** no GLPI → **cria** novo item |
| `localidade` / `localizacao` | `custom_localizacao` | Texto (campo custom «Localização»); não é mais `locations_id` |

**Criar vs atualizar**

| Situação | Comportamento |
|----------|----------------|
| `id_ativo` = `1` | Atualiza o item id 1 |
| `id_ativo` vazio + `nome` já existe no GLPI | Atualiza o item com esse nome |
| `id_ativo` vazio + `nome` novo (único) | **Cria** um novo Starlink |

Para cadastrar uma **segunda antena** sem alterar a primeira: deixe `id_ativo` em branco e use um `nome` que ainda não exista (ex. outro KIT).

| Ação | Método |
|------|--------|
| Validar | `POST /api/sync/starlink/validate` |
| Sincronizar | `POST /api/sync/starlink` |

### Chip

Colunas: `id_ativo` (opcional), `iccid`, `nome` (opcional), `numero`, `responsavel`, `status`

| Coluna planilha | Campo GLPI | Notas |
|-----------------|------------|--------|
| `id_ativo` | — | Preenchido → **atualiza** esse id |
| `iccid` | `custom_iccid` | Chave natural; iccid novo sem `id_ativo` → **cria** chip |
| `nome` | `name` | Rótulo no GLPI (opcional na criação; se vazio, usa o iccid como nome) |
| `status` | `states_id` | Nome do status (ex. «Em uso») |

| Situação | Comportamento |
|----------|----------------|
| `id_ativo` vazio + `iccid` novo | **Cria** |
| `id_ativo` vazio + `iccid` já existe | **Atualiza** |
| `id_ativo` = `N` | **Atualiza** id N |

| Ação | Método |
|------|--------|
| Validar | `POST /api/sync/chip/validate` |
| Sincronizar | `POST /api/sync/chip` |

### Celular

Colunas: `id_ativo` (opcional), `nome`, `imei`, `modelo`, `responsavel`

| Coluna planilha | Campo GLPI | Notas |
|-----------------|------------|--------|
| `id_ativo` | — | Preenchido → **atualiza** esse id |
| `imei` | `custom_imei` | Chave natural; imei novo sem `id_ativo` → **cria** |
| `nome` | `name` | Nome de exibição (se vazio na criação, usa o imei) |
| `modelo` | `custom_modelo` | Texto |

| Situação | Comportamento |
|----------|----------------|
| `id_ativo` vazio + `imei` novo | **Cria** |
| `id_ativo` vazio + `imei` já existe | **Atualiza** |
| `id_ativo` = `N` | **Atualiza** id N |

| Ação | Método |
|------|--------|
| Validar | `POST /api/sync/celular/validate` |
| Sincronizar | `POST /api/sync/celular` |

**Permissões:** em cada definição (Chip / Celular), aba **Perfis** → **Criar** + **Atualizar todos** para o perfil do token API.

Body: `multipart/form-data`, campo `file` = CSV ou XLSX.

Planilhas modelo em `docs/templates/` (`template_*.csv` e, após `mvn -Dtest=SpreadsheetTemplatesGeneratorTest test`, `template_*.xlsx`).

### Resumo de IDs

| Recurso | Endpoint |
|---------|----------|
| Usuários | `GET /api/users/summary` |
| Status | `GET /api/states/summary` |
| Localidades | `GET /api/locations/summary` |
| Starlink | `GET /api/custom-assets/starlink/summary` |
| Chip | `GET /api/custom-assets/chip/summary` |
| Celular | `GET /api/custom-assets/celular/summary` |

## Senhas (Starlink)

- Não commitar senhas em planilhas de exemplo.
- O endpoint `/validate` mascara senhas na resposta (`***`).
- Logs do middleware não devem imprimir campos `senha_*`.

## Permissões GLPI (obrigatório para sync)

No GLPI 11, ativos customizados **não têm permissão de escrita por padrão**.

1. **Configuração → Ativos → Starlink** (ou Chip / Celular)
2. Aba **Perfis**
3. No perfil do usuário usado pelo token da API, marque pelo menos:
   - **Visualizar todos**
   - **Criar** (para linhas novas na planilha)
   - **Atualizar todos** (ou *Atualizar atribuídos*, se aplicável)

Sem **Atualizar todos**, a API retorna `ERROR_GLPI_UPDATE`. Sem **Criar**, falha ao incluir chip/celular/starlink novos.

## Erro `ERROR_GLPI_UPDATE` no sync

Causas frequentes no GLPI 11:

1. **PUT no itemtype errado** — listagem usa `Glpi\Asset\AssetDefinition/Starlink`, mas alteração deve ir para `Glpi\CustomAsset\StarlinkAsset` (o middleware usa o segundo automaticamente).
2. **Nome de campo `custom_*` divergente** — o nome na API é `custom_` + *nome do sistema* do campo na definição GLPI, não o rótulo da tela. Ex.: se o sistema for `senha_conta`, a API espera `custom_senha_conta`, não `custom_senha_conta_starlink`.
3. **Perfil sem permissão de atualização** no tipo de ativo.
4. **Campos de senha** rejeitados pelo tipo Password do GLPI.

### Diagnóstico

```http
GET /api/custom-assets/starlink/items/1
POST /api/custom-assets/starlink/items/1/update-probe
Content-Type: application/json

{"name":"KIT-teste","custom_projeto":"PCS","users_id":96,"custom_email":"a@b.com"}
```

- `GET .../items/1` — `customFieldKeys` com nomes aceitos no PUT
- `POST .../update-probe` — testa PUT campo a campo e indica o que o GLPI aceita ou rejeita

O sync ignora campos que não existem no GET do item e, se o PUT em lote falhar, tenta campo a campo (sem senhas) para concluir o que for possível.
