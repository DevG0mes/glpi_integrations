/**
 * Documentação da API — conteúdo estático (disponível em produção sem Swagger).
 */
const ApiDocs = (() => {
  const SECTIONS = [
    {
      id: "auth",
      title: "Autenticação",
      body: `
        <p>Em produção (<code>SPRING_PROFILES_ACTIVE=prod</code>), defina <code>GLPI_API_KEY</code> no servidor.</p>
        <p>Envie em todas as requisições <code>/api/**</code>:</p>
        <pre class="mono">X-API-Key: sua_chave</pre>
        <p>Ou: <code>Authorization: Bearer sua_chave</code></p>
        <p>Rotas públicas (sem chave): <code>/actuator/health</code>, <code>/app/**</code>, <code>/api/security/status</code>.</p>
        <p>Na interface: botão <strong>Chave API</strong> (salva no navegador).</p>
      `,
    },
    {
      id: "fluxo",
      title: "Fluxo recomendado",
      body: `
        <ol class="hint" style="margin:0;padding-left:1.2rem">
          <li>Consultas auxiliares ou Inventário — conferir ids</li>
          <li>Montar planilha (modelos em Sincronizar)</li>
          <li><code>POST /api/sync/{tipo}/validate</code> — dry-run</li>
          <li><code>POST /api/sync/{tipo}</code> — gravar no GLPI</li>
        </ol>
      `,
    },
    {
      id: "sistema",
      title: "Sistema",
      routes: [
        { method: "GET", path: "/actuator/health", desc: "Saúde da aplicação (público)" },
        { method: "GET", path: "/api/glpi/connection-info", desc: "URL GLPI e credenciais (sem expor tokens)" },
        { method: "GET", path: "/api/security/status", desc: "Se a API exige chave (público)" },
      ],
    },
    {
      id: "lookups",
      title: "Consultas auxiliares (lookup)",
      routes: [
        { method: "GET", path: "/api/users/summary", desc: "Usuários id + login" },
        { method: "GET", path: "/api/states/summary", desc: "Status" },
        { method: "GET", path: "/api/locations/summary", desc: "Localidades" },
        { method: "GET", path: "/api/groups/summary", desc: "Grupos" },
        { method: "GET", path: "/api/computer-types/summary", desc: "Tipos de computer" },
        { method: "GET", path: "/api/manufacturers/summary", desc: "Fabricantes" },
        { method: "GET", path: "/api/computer-models/summary", desc: "Modelos (resumo)" },
        { method: "GET", path: "/api/computers/summary", desc: "Computers id + nome" },
      ],
    },
    {
      id: "computers",
      title: "Computers",
      routes: [
        { method: "GET", path: "/api/computers/summary", desc: "Lista resumida" },
        { method: "GET", path: "/api/computers/report", desc: "Relatório enriquecido com garantias relacionadas por patrimônio/serial" },
        { method: "GET", path: "/api/computers/{id}", desc: "Detalhe" },
        { method: "PUT", path: "/api/computers/{id}", desc: "Atualizar um computer (JSON)" },
        { method: "POST", path: "/api/sync/computers/validate", desc: "Validar planilha (multipart file)" },
        { method: "POST", path: "/api/sync/computers", desc: "Sync planilha (só atualiza existentes)" },
      ],
    },
    {
      id: "colaboradores",
      title: "Colaboradores (API dedicada)",
      routes: [
        { method: "GET", path: "/api/colaboradores/summary", desc: "Lista id + nome" },
        { method: "GET", path: "/api/colaboradores", desc: "Lista completa (range, expandDropdowns)" },
        { method: "GET", path: "/api/colaboradores/{id}", desc: "Detalhe por id" },
        { method: "GET", path: "/api/colaboradores/by-email/{email}", desc: "Detalhe por e-mail" },
        { method: "POST", path: "/api/colaboradores", desc: "Criar (body: nome, email, departamento, ativo)" },
        { method: "PUT", path: "/api/colaboradores/{id}", desc: "Atualizar" },
        { method: "POST", path: "/api/sync/colaborador/validate", desc: "Validar planilha" },
        { method: "POST", path: "/api/sync/colaborador", desc: "Sync planilha" },
      ],
    },
    {
      id: "custom",
      title: "Ativos customizados (starlink | chip | celular | colaborador | garantia)",
      routes: [
        { method: "GET", path: "/api/custom-assets/config", desc: "Colunas e itemtypes configurados" },
        { method: "GET", path: "/api/custom-assets/discover", desc: "Descobrir itemtypes GLPI 11" },
        { method: "GET", path: "/api/custom-assets/{key}/summary", desc: "Lista id + nome" },
        { method: "GET", path: "/api/custom-assets/{key}/items/{id}", desc: "Item completo + customFieldKeys" },
        { method: "GET", path: "/api/custom-assets/{key}/probe", desc: "Teste de conectividade" },
        { method: "POST", path: "/api/sync/starlink/validate", desc: "Validar Starlink" },
        { method: "POST", path: "/api/sync/starlink", desc: "Sync Starlink" },
        { method: "POST", path: "/api/sync/chip/validate", desc: "Validar Chip" },
        { method: "POST", path: "/api/sync/chip", desc: "Sync Chip" },
        { method: "POST", path: "/api/sync/celular/validate", desc: "Validar Celular" },
        { method: "POST", path: "/api/sync/celular", desc: "Sync Celular" },
        { method: "POST", path: "/api/sync/garantia/validate", desc: "Validar Garantia" },
        { method: "POST", path: "/api/sync/garantia", desc: "Sync Garantia" },
      ],
    },
    {
      id: "colaborador",
      title: "Colaborador (JSON)",
      routes: [
        { method: "GET", path: "/api/colaboradores/summary", desc: "Lista id + nome" },
        { method: "GET", path: "/api/colaboradores", desc: "Lista completa (?range=0-999)" },
        { method: "GET", path: "/api/colaboradores/{id}", desc: "Um colaborador" },
        { method: "POST", path: "/api/colaboradores", desc: "Criar (body: nome, email, departamento, ativo)" },
        { method: "PUT", path: "/api/colaboradores/{id}", desc: "Atualizar" },
        { method: "POST", path: "/api/sync/colaborador/validate", desc: "Validar planilha" },
        { method: "POST", path: "/api/sync/colaborador", desc: "Sync planilha" },
      ],
    },
    {
      id: "upload",
      title: "Upload de planilha",
      body: `
        <p><code>Content-Type: multipart/form-data</code> · campo <code>file</code> · CSV ou XLSX · máx. 10 MB</p>
        <pre class="mono">curl -X POST "http://servidor:8081/api/sync/starlink" \\
  -H "X-API-Key: SUA_CHAVE" \\
  -F "file=@planilha.csv"</pre>
      `,
    },
  ];

  function methodClass(method) {
    const m = (method || "GET").toUpperCase();
    if (m === "POST") return "method-post";
    if (m === "PUT") return "method-put";
    return "method-get";
  }

  function renderRoutesTable(routes) {
    if (!routes?.length) return "";
    let html = `<table class="data docs-table"><thead><tr><th>Método</th><th>Rota</th><th>Descrição</th></tr></thead><tbody>`;
    for (const r of routes) {
      html += `<tr>
        <td><span class="method-badge ${methodClass(r.method)}">${r.method}</span></td>
        <td><code class="mono">${r.path}</code></td>
        <td>${r.desc}</td>
      </tr>`;
    }
    html += "</tbody></table>";
    return html;
  }

  function renderNav() {
    return `<nav class="docs-nav">${SECTIONS.map((s) => `<a href="#docs-${s.id}">${s.title}</a>`).join("")}</nav>`;
  }

  function renderSections(configItems) {
    let html = "";
    for (const sec of SECTIONS) {
      html += `<section class="docs-section card" id="docs-${sec.id}">`;
      html += `<h2>${sec.title}</h2>`;
      if (sec.body) html += sec.body;
      if (sec.routes) html += renderRoutesTable(sec.routes);
      html += "</section>";
    }
    if (configItems?.length) {
      html += `<section class="docs-section card" id="docs-config"><h2>Configuração atual (GLPI)</h2>`;
      for (const c of configItems) {
        html += `<h3>${c.key}</h3><p class="hint mono">${c.itemType || ""}</p>`;
        if (c.spreadsheetColumns?.length) {
          html += `<div class="summary-chips">${c.spreadsheetColumns.map((col) => `<span class="chip">${col}</span>`).join("")}</div>`;
        }
      }
      html += "</section>";
    }
    return html;
  }

  async function checkOpenApi() {
    try {
      const headers = { Accept: "application/json" };
      const key = typeof GlpiApi !== "undefined" ? GlpiApi.getApiKey() : "";
      if (key) headers["X-API-Key"] = key;
      const res = await fetch("/v3/api-docs", { headers });
      return res.ok;
    } catch {
      return false;
    }
  }

  async function render(container) {
    const openApiOk = await checkOpenApi();
    let swaggerBox = `
      <div class="card docs-swagger">
        <h2>Swagger / OpenAPI</h2>
        <p class="hint">No servidor de <strong>produção</strong> o Swagger fica desligado por segurança (<code>application-prod.yml</code>).</p>
    `;
    if (openApiOk) {
      swaggerBox += `
        <p>OpenAPI disponível neste ambiente:</p>
        <p class="hint">No Swagger: clique em <strong>Authorize</strong> e informe a mesma <code>GLPI_API_KEY</code> do servidor (header X-API-Key).</p>
        <div class="actions-bar">
          <a class="btn btn-primary" href="/swagger-ui.html" target="_blank" rel="noopener">Abrir Swagger UI</a>
          <a class="btn btn-secondary" href="/v3/api-docs" target="_blank" rel="noopener">JSON OpenAPI</a>
        </div>
      `;
    } else {
      swaggerBox += `
        <p>Use esta aba <strong>Documentação</strong> como referência. Para Swagger local, rode sem perfil <code>prod</code> ou defina <code>GLPI_ENABLE_SWAGGER=true</code> (ver documentação de deploy).</p>
      `;
    }
    swaggerBox += "</div>";

    let configItems = [];
    try {
      configItems = await GlpiApi.get("/api/custom-assets/config");
    } catch {
      /* sem chave ou offline */
    }

    container.innerHTML = `
      ${swaggerBox}
      <div class="docs-layout">
        ${renderNav()}
        <div class="docs-content">
          ${renderSections(configItems)}
        </div>
      </div>
    `;
  }

  return { render };
})();
