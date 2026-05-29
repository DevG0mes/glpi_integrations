# Validação da API GLPI

Execute no PowerShell (substitua host, tokens e caminhos).

## Teste A — API legada (`apirest.php`)

```powershell
curl -s -X GET `
  -H "Content-Type: application/json" `
  -H "Authorization: user_token SEU_USER_TOKEN" `
  -H "App-Token: SEU_APP_TOKEN" `
  "http://SEU_HOST/glpi/apirest.php/initSession/"
```

Configuração da aplicação:

```yaml
glpi.api.style: LEGACY_APIREST
glpi.api.base-url: http://SEU_HOST/glpi/apirest.php
```

## Teste B — Legada via roteador (`api.php/v1`)

```powershell
curl -s -X GET `
  -H "Content-Type: application/json" `
  -H "Authorization: user_token SEU_USER_TOKEN" `
  -H "App-Token: SEU_APP_TOKEN" `
  "http://SEU_HOST/glpi/api.php/v1/initSession/"
```

Configuração:

```yaml
glpi.api.style: LEGACY_VIA_ROUTER
glpi.api.base-url: http://SEU_HOST/glpi/api.php/v1
```

## Teste C — API v2 (somente se A/B falharem)

Consulte o Swagger em `http://SEU_HOST/glpi/api.php/doc`. O client atual deste projeto usa o fluxo legado (`initSession` + `PUT Computer/{id}`).

## Critério

Use o endpoint que retornar HTTP 200 com `session_token` no JSON.
