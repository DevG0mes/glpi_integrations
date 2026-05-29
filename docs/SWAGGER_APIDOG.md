# Swagger UI e Apidog — como começar

## Swagger ou Apidog?

| Ferramenta | Papel |
|------------|--------|
| **OpenAPI** | Padrão (arquivo JSON/YAML descrevendo a API) |
| **Swagger UI** | Interface web para **testar** a API no navegador |
| **Apidog** | Cliente tipo Postman/Insomnia + documentação em equipe; **importa OpenAPI** |

Recomendação: gerar **OpenAPI no código** (automático) e usar **os dois**:

1. **Swagger UI** — desenvolvimento local, testar rotas na hora  
2. **Apidog** — compartilhar com o time, mocks, coleções, histórico de requests  

Não é “Swagger **ou** Apidog”: o Apidog consome o mesmo contrato que o Swagger gera.

---

## O que já está no projeto

Dependência **springdoc-openapi** (v3.0.3, compatível com Spring Boot 4):

- Gera o contrato OpenAPI 3 a partir dos controllers
- Expõe **Swagger UI** embutido na aplicação

---

## Passo 1 — Subir a aplicação

```powershell
cd "C:\Users\joao.gomes\OneDrive - PSI Energy\Documentos\glpi-integration"
.\mvnw.cmd spring-boot:run
```

## Passo 2 — Abrir o Swagger UI

No navegador:

| URL | Conteúdo |
|-----|----------|
| http://localhost:8081/swagger-ui.html | Interface interativa (testar GET/POST/PUT) |
| http://localhost:8081/v3/api-docs | JSON OpenAPI (para importar no Apidog) |
| http://localhost:8081/v3/api-docs.yaml | Mesmo contrato em YAML |

Rotas agrupadas por tags: **Sincronização**, **Ativos customizados**, **Computer**, **Consultas auxiliares**, etc.

### Upload de planilha no Swagger UI

1. Abra `POST /api/sync/starlink` (ou chip/celular)  
2. **Try it out**  
3. Em `file`, escolha o `.xlsx` ou `.csv`  
4. **Execute**

---

## Passo 3 — Importar no Apidog

1. Instale o [Apidog](https://www.apidog.com/) (desktop ou web)  
2. **Criar projeto** → **Importar**  
3. Escolha **OpenAPI / Swagger**  
4. URL de importação (com a app rodando):

   ```
   http://localhost:8081/v3/api-docs
   ```

   Ou salve o arquivo:

   ```powershell
   curl -o openapi.json http://localhost:8081/v3/api-docs
   ```

   e importe o `openapi.json` no Apidog.

5. Após importar, organize pastas por tag (Starlink, Chip, Celular, Computer, …).

### Atualizar documentação no Apidog

Sempre que mudar rotas no código:

1. Reinicie a app (ou hot-reload)  
2. No Apidog: **Reimport** ou **Sync** a partir de `http://localhost:8081/v3/api-docs`

---

## Passo 4 — Manter documentação alinhada

| Camada | Quando usar |
|--------|-------------|
| **Código** (`@Tag`, `@Operation` nos controllers) | Descrição curta que aparece no Swagger/Apidog |
| **docs/API_REFERENCE.md** | Texto longo, regras de negócio (criar vs atualizar, colunas) |
| **docs/CUSTOM_ASSETS.md** | GLPI, permissões, campos custom |

O OpenAPI **não substitui** o `API_REFERENCE.md` (regras de planilha e GLPI); complementa com contrato técnico e “Try it out”.

---

## Desativar em produção (opcional)

No servidor de produção, desligue a UI pública:

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

Ou só a UI, mantendo JSON interno:

```properties
springdoc.swagger-ui.enabled=false
```

---

## Próximos passos (evolução)

1. Adicionar `@Operation` nos endpoints que faltam (chip/celular validate/sync)  
2. Documentar schemas (`SyncReport`, `ComputerUpdateRequest`) com `@Schema` nos DTOs  
3. CI: gerar `openapi.json` no build e publicar como artefato  
4. Apidog: ambientes `local` / `homolog` com `baseUrl` diferente  

---

## Links

- [springdoc.org](https://springdoc.org/) — propriedades e opções  
- [API_REFERENCE.md](API_REFERENCE.md) — lista completa de rotas em português  
