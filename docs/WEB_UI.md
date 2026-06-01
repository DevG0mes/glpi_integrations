# Interface web — GLPI Integration

Painel no navegador para operar o middleware sem Insomnia/Apidog (upload de planilhas, consultas e diagnóstico).

## Acesso

Com a aplicação rodando (porta **8081** por padrão):

| URL | Conteúdo |
|-----|----------|
| http://localhost:8081/ | Redireciona para a interface |
| http://localhost:8081/app/ | Mesma interface |
| http://localhost:8081/swagger-ui.html | Documentação OpenAPI (desenvolvedores) |

## Telas

| Menu | Função |
|------|--------|
| **Painel** | Health da API, conexão GLPI (`/api/glpi/connection-info`), probe Starlink/Chip/Celular |
| **Sincronizar** | Upload CSV/XLSX, validar (dry-run) e sincronizar Computers / Starlink / Chip / Celular |
| **Inventário** | Listas id + nome (até 500 itens por tipo), com filtro local |
| **Consultas auxiliares** | Usuários, status, localidades, etc. — exportar CSV |
| **Detalhe do item** | JSON completo de um ativo customizado por id |
| **Documentação** | Referência da API (rotas, autenticação, exemplos curl) |

O link antigo **Swagger** no rodapé foi substituído por esta aba. Em produção o Swagger fica **desligado** por padrão (`application-prod.yml`). Para habilitar: `GLPI_ENABLE_SWAGGER=true` no `.env` (também exige `X-API-Key`).

## Modelos de planilha

Baixe na tela **Sincronizar** ou diretamente:

- `/app/templates/template_starlink.csv`
- `/app/templates/template_chip.csv`
- `/app/templates/template_celular.csv`
- `/app/templates/template_computers.csv`

## Requisitos

- GLPI configurado em `application-local.properties` (URL + tokens)
- Mesma origem: a UI chama a API no mesmo host/porta (sem CORS extra)

## Arquivos

| Caminho | Descrição |
|---------|-----------|
| `src/main/resources/static/app/` | HTML, CSS, JavaScript |
| `WebUiController.java` | Encaminha `/` → `/app/index.html` |

A UI é estática (HTML + JS); evoluções futuras podem migrar para React/Vue em pasta separada se necessário.
