# Segurança

## Tokens da API GLPI

- Nunca commite `application-local.properties`, `.env`, planilhas reais (`.csv`/`.xlsx` fora de `src/test`) ou tokens em `application.yml`.
- Use `application-local.properties.example` e `.env.example` como modelo; valores reais ficam só na máquina/servidor (gitignored).
- Variáveis: `GLPI_APP_TOKEN`, `GLPI_USER_TOKEN`.
- Se tokens foram expostos, **rotacione** no GLPI (App-Token e User-Token do usuário de automação).

## Chave da API do middleware (`GLPI_API_KEY`)

Em **produção** (perfil `prod`, padrão no Docker):

1. Gere uma chave forte:
   ```bash
   openssl rand -hex 32
   ```
2. Defina no `.env` ou `/etc/glpi-integration/env`:
   ```env
   GLPI_API_KEY=sua_chave_gerada
   SPRING_PROFILES_ACTIVE=prod
   ```
3. Clientes (curl, Apidog, interface web) enviam:
   ```http
   X-API-Key: sua_chave_gerada
   ```
   ou `Authorization: Bearer sua_chave_gerada`.

**Comportamento:**

| `GLPI_API_KEY` | Efeito |
|----------------|--------|
| Vazia | API `/api/**` aberta (somente desenvolvimento local) |
| Preenchida | Todas as rotas `/api/**` exigem a chave |

Rotas **públicas** (sem chave): `/actuator/health`, `/app/**` (interface web), `/api/security/status`.

Na interface web: botão **Chave API** → salva em `sessionStorage` do navegador (não vai para o Git).

## Perfil `prod`

Com `SPRING_PROFILES_ACTIVE=prod`:

- Swagger/OpenAPI **desligados** (`/swagger-ui.html` indisponível)
- Actuator expõe apenas `health`
- Respostas de erro sem stack trace

## Rede e servidor

- Exponha a porta **8081** só na rede interna ou atrás de **Nginx + HTTPS** (veja [DEPLOY_UBUNTU.md](DEPLOY_UBUNTU.md)).
- Use **UFW** para bloquear acesso externo direto se não for necessário.
- O container Docker roda como usuário não-root (`glpi`).

## Upload de planilhas

- Limite: **10 MB** por arquivo (`spring.servlet.multipart`).
- Valide sempre com `POST .../validate` antes do sync definitivo.

## Logs

- Tokens GLPI e `GLPI_API_KEY` **não** são logados.
- Teste de startup (`glpi.test-on-startup=true`) exibe só prefixo do `session_token`.

## Senhas (Starlink)

- Campos de senha são mascarados em `/validate` (`***`).
- Não versione planilhas com senhas reais.
- No GLPI, use campo tipo **senha** e restrinja permissões de leitura.

## Checklist antes do deploy

- [ ] `.env` com `GLPI_API_KEY` definida (produção)
- [ ] `GLPI_APP_TOKEN` e `GLPI_USER_TOKEN` preenchidos
- [ ] `GLPI_API_BASE_URL` acessível **de dentro** do container/servidor
- [ ] Firewall / Nginx configurados
- [ ] Tokens GLPI com permissão mínima necessária (criar/atualizar ativos)
- [ ] Repositório sem `application-local.properties` nem planilhas reais
