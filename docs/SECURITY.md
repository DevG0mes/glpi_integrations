# Segurança

## Tokens da API GLPI

- Nunca commite `application-local.properties`, `.env`, planilhas reais (`.csv`/`.xlsx` fora de `src/test`) ou tokens em `application.yml`.
- Use `application-local.properties.example` e `.env.example` como modelo; valores reais ficam só na máquina local (gitignored).
- Use variáveis de ambiente `GLPI_APP_TOKEN` e `GLPI_USER_TOKEN`.
- Se tokens foram expostos em repositório ou chat, **rotacione** no GLPI:
  - App-Token: Configuração → Geral → API
  - User-Token: Preferências do usuário de automação → chave de acesso remoto

## Logs

A aplicação não registra tokens de sessão completos. O teste de startup exibe apenas um prefixo do `session_token`.

## Senhas (Starlink)

- Campos `senha_conta_starlink` e `senha_roteador` são tratados como **sensíveis**.
- Endpoints `/validate` retornam `***` no lugar das senhas no relatório.
- Não inclua senhas reais em planilhas versionadas no Git.
- No GLPI, use tipo de campo **senha** quando disponível na definição de ativo.
- Restrinja no GLPI quem pode visualizar/editar esses campos.
