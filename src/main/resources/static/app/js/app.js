/**
 * Interface web — GLPI Integration
 */
(() => {
  const SYNC_TYPES = [
    { key: "computers", label: "Computers", validate: "/api/sync/computers/validate", sync: "/api/sync/computers", template: "/app/templates/template_computers.csv", note: "Apenas atualiza itens existentes (não cria)." },
    { key: "starlink", label: "Starlink", validate: "/api/sync/starlink/validate", sync: "/api/sync/starlink", template: "/app/templates/template_starlink.csv" },
    { key: "chip", label: "Chip", validate: "/api/sync/chip/validate", sync: "/api/sync/chip", template: "/app/templates/template_chip.csv" },
    { key: "celular", label: "Celular", validate: "/api/sync/celular/validate", sync: "/api/sync/celular", template: "/app/templates/template_celular.csv" },
  ];

  const LOOKUPS = [
    { path: "/api/users/summary", label: "Usuários (login)" },
    { path: "/api/states/summary", label: "Status" },
    { path: "/api/locations/summary", label: "Localidades" },
    { path: "/api/groups/summary", label: "Grupos" },
    { path: "/api/computer-types/summary", label: "Tipos de computer" },
    { path: "/api/manufacturers/summary", label: "Fabricantes" },
    { path: "/api/computer-models/summary", label: "Modelos de computer" },
    { path: "/api/computers/summary", label: "Computers (id + nome)" },
  ];

  const ASSET_KEYS = ["starlink", "chip", "celular"];

  let assetConfig = [];
  let currentRoute = "dashboard";
  let syncTypeKey = "starlink";
  let selectedFile = null;

  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => document.querySelectorAll(sel);

  function toast(message, type = "info") {
    const el = $("#toast");
    el.textContent = message;
    el.className = "toast" + (type === "error" ? " error" : type === "success" ? " success" : "");
    el.hidden = false;
    clearTimeout(el._timer);
    el._timer = setTimeout(() => {
      el.hidden = true;
    }, 4500);
  }

  function escapeHtml(s) {
    if (s == null) return "";
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function parseRoute() {
    const hash = (location.hash || "#dashboard").slice(1);
    const parts = hash.split("/").filter(Boolean);
    return { view: parts[0] || "dashboard", sub: parts[1] || null };
  }

  function setActiveNav(view) {
    $$(".nav-link").forEach((a) => {
      const route = a.getAttribute("data-route");
      a.classList.toggle("active", route === view || (view === "sync" && route === "sync"));
    });
  }

  function showView(viewId) {
    $$(".view").forEach((v) => {
      v.hidden = v.id !== "view-" + viewId;
    });
    const titles = {
      dashboard: "Painel",
      sync: "Sincronizar planilha",
      inventory: "Inventário",
      lookups: "Consultas auxiliares",
      item: "Detalhe do item",
      docs: "Documentação da API",
    };
    $("#page-title").textContent = titles[viewId] || viewId;
    setActiveNav(viewId);
    currentRoute = viewId;
  }

  async function loadAssetConfig() {
    try {
      assetConfig = await GlpiApi.get("/api/custom-assets/config");
    } catch {
      assetConfig = [];
    }
  }

  async function refreshApiStatus() {
    const badge = $("#api-status");
    try {
      const health = await GlpiApi.get("/actuator/health");
      const up = health.status === "UP";
      badge.textContent = up ? "API online" : "API degradada";
      badge.className = "badge " + (up ? "badge-ok" : "badge-warn");
    } catch {
      badge.textContent = "API indisponível";
      badge.className = "badge badge-err";
    }
  }

  function renderSyncReport(report, container) {
    if (!report) {
      container.innerHTML = '<p class="empty">Sem resultado.</p>';
      return;
    }
    const lines = report.lines || [];
    let html = `
      <div class="grid grid-3" style="margin-bottom:1rem">
        <div class="card"><div class="stat">${report.total ?? 0}</div><div class="stat-label">Linhas</div></div>
        <div class="card"><div class="stat" style="color:var(--success)">${report.successCount ?? 0}</div><div class="stat-label">Sucesso</div></div>
        <div class="card"><div class="stat" style="color:var(--danger)">${report.failureCount ?? 0}</div><div class="stat-label">Falhas</div></div>
      </div>
    `;
    if (report.source) {
      html += `<p class="hint">Fonte: <code>${escapeHtml(report.source)}</code></p>`;
    }
    if (lines.length === 0) {
      html += '<p class="empty">Nenhuma linha processada.</p>';
    } else {
      html += `<div class="table-wrap"><table class="data"><thead><tr>
        <th>Linha</th><th>ID GLPI</th><th>Status</th><th>Mensagem</th>
      </tr></thead><tbody>`;
      for (const line of lines) {
        const cls = line.success ? "row-ok" : "row-err";
        html += `<tr class="${cls}">
          <td>${line.lineNumber}</td>
          <td>${line.glpiId || "—"}</td>
          <td>${line.success ? "OK" : "Erro"}</td>
          <td>${escapeHtml(line.message)}</td>
        </tr>`;
      }
      html += "</tbody></table></div>";
    }
    container.innerHTML = html;
  }

  function renderIdNameTable(items, secondColLabel = "Nome") {
    if (!items || items.length === 0) {
      return '<p class="empty">Nenhum registro retornado.</p>';
    }
    let html = `<div class="table-wrap"><table class="data"><thead><tr><th>ID</th><th>${escapeHtml(secondColLabel)}</th></tr></thead><tbody>`;
    for (const row of items) {
      html += `<tr><td>${row.id}</td><td>${escapeHtml(row.name)}</td></tr>`;
    }
    html += "</tbody></table></div>";
    return html;
  }

  async function renderDashboard() {
    const el = $("#view-dashboard");
    el.innerHTML = '<p class="hint">Carregando painel…</p>';

    let healthHtml = '<span class="badge badge-muted">—</span>';
    let connHtml = "";
    let probesHtml = "";

    try {
      const health = await GlpiApi.get("/actuator/health");
      const up = health.status === "UP";
      healthHtml = `<span class="badge ${up ? "badge-ok" : "badge-warn"}">${escapeHtml(health.status)}</span>`;
    } catch (e) {
      healthHtml = `<span class="badge badge-err">Erro</span><p class="hint">${escapeHtml(e.message)}</p>`;
    }

    try {
      const conn = await GlpiApi.get("/api/glpi/connection-info");
      const warn = conn.localhostWarning;
      connHtml = `
        <p><strong>URL resolvida:</strong> <code class="mono">${escapeHtml(conn.baseUrlResolved)}</code></p>
        <p><strong>Credenciais:</strong> ${conn.credentialsPresent ? "configuradas" : "ausentes"}</p>
        ${warn ? `<p class="hint" style="color:var(--warning)">${escapeHtml(conn.hint || conn.localPropertiesHint)}</p>` : ""}
      `;
    } catch (e) {
      connHtml = `<p class="hint">${escapeHtml(e.message)}</p>`;
    }

    const probeCards = [];
    for (const key of ASSET_KEYS) {
      try {
        const p = await GlpiApi.get(`/api/custom-assets/${key}/probe`);
        const ok = p.reachable;
        probeCards.push(`
          <div class="card">
            <h3>${escapeHtml(key)}</h3>
            <span class="badge ${ok ? "badge-ok" : "badge-err"}">${ok ? "Conectado" : "Falha"}</span>
            ${!ok && p.error ? `<p class="hint">${escapeHtml(p.error)}</p>` : ""}
          </div>
        `);
      } catch (e) {
        probeCards.push(`<div class="card"><h3>${escapeHtml(key)}</h3><span class="badge badge-err">Erro</span><p class="hint">${escapeHtml(e.message)}</p></div>`);
      }
    }
    probesHtml = `<div class="grid grid-3">${probeCards.join("")}</div>`;

    el.innerHTML = `
      <div class="grid grid-2">
        <div class="card">
          <h2>Saúde da aplicação</h2>
          ${healthHtml}
          <p class="hint">Endpoint: <code>/actuator/health</code></p>
        </div>
        <div class="card">
          <h2>Conexão GLPI</h2>
          ${connHtml}
        </div>
      </div>
      <section style="margin-top:1.25rem">
        <h2 style="font-size:1rem;margin-bottom:0.75rem">Ativos customizados (probe)</h2>
        ${probesHtml}
      </section>
      <section style="margin-top:1.25rem" class="card">
        <h2>Fluxo recomendado</h2>
        <ol class="hint" style="margin:0;padding-left:1.2rem">
          <li>Baixe o modelo CSV em <strong>Sincronizar</strong></li>
          <li>Preencha a planilha (use <strong>Consultas auxiliares</strong> para ids)</li>
          <li>Execute <strong>Validar</strong> (dry-run) antes de gravar</li>
          <li>Execute <strong>Sincronizar</strong> para enviar ao GLPI</li>
        </ol>
      </section>
    `;
  }

  function getSyncType(key) {
    return SYNC_TYPES.find((t) => t.key === key) || SYNC_TYPES[1];
  }

  function renderSyncView() {
    const el = $("#view-sync");
    const type = getSyncType(syncTypeKey);
    const cfg = assetConfig.find((c) => c.key === syncTypeKey);
    const columns = cfg?.spreadsheetColumns || [];

    const tabs = SYNC_TYPES.map(
      (t) => `<button type="button" class="tab ${t.key === syncTypeKey ? "active" : ""}" data-sync-type="${t.key}">${escapeHtml(t.label)}</button>`
    ).join("");

    const colHint =
      columns.length > 0
        ? `<div class="summary-chips">${columns.map((c) => `<span class="chip">${escapeHtml(c)}</span>`).join("")}</div>`
        : syncTypeKey === "computers"
          ? `<p class="hint">Colunas comuns: <code>id_ativo</code>, <code>serial</code>, <code>responsavel</code>, <code>local</code>, <code>grupo</code>, <code>tipo</code>, <code>fabricante</code>, <code>nome</code>, <code>observacao</code></p>`
          : "";

    el.innerHTML = `
      <div class="tabs" id="sync-tabs">${tabs}</div>
      <div class="card">
        <h2>Enviar planilha — ${escapeHtml(type.label)}</h2>
        ${type.note ? `<p class="hint">${escapeHtml(type.note)}</p>` : ""}
        ${colHint}
        <div class="file-drop" id="file-drop">
          <p>Arraste um arquivo <strong>.csv</strong> ou <strong>.xlsx</strong> aqui</p>
          <p class="hint">ou selecione abaixo</p>
        </div>
        <div class="form-row">
          <div class="field" style="flex:1;min-width:220px">
            <label for="sync-file">Arquivo</label>
            <input type="file" id="sync-file" accept=".csv,.xlsx,.xls">
          </div>
          <a class="btn btn-secondary" href="${type.template}" download>Baixar modelo CSV</a>
        </div>
        <p id="file-name" class="hint"></p>
        <div class="actions-bar">
          <button type="button" class="btn btn-secondary" id="btn-validate">Validar (dry-run)</button>
          <button type="button" class="btn btn-primary" id="btn-sync">Sincronizar no GLPI</button>
        </div>
      </div>
      <div class="card" id="sync-result" style="margin-top:1rem">
        <h3>Resultado</h3>
        <p class="hint">Execute validação ou sincronização para ver o relatório.</p>
      </div>
    `;

    bindSyncEvents(type);
  }

  function bindSyncEvents(type) {
    const fileInput = $("#sync-file");
    const drop = $("#file-drop");
    const fileName = $("#file-name");

    function setFile(file) {
      selectedFile = file;
      fileName.textContent = file ? `Selecionado: ${file.name} (${Math.round(file.size / 1024)} KB)` : "";
    }

    fileInput.addEventListener("change", () => setFile(fileInput.files[0] || null));

    drop.addEventListener("dragover", (e) => {
      e.preventDefault();
      drop.classList.add("dragover");
    });
    drop.addEventListener("dragleave", () => drop.classList.remove("dragover"));
    drop.addEventListener("drop", (e) => {
      e.preventDefault();
      drop.classList.remove("dragover");
      const f = e.dataTransfer.files[0];
      if (f) {
        fileInput.files = e.dataTransfer.files;
        setFile(f);
      }
    });

    $$("#sync-tabs .tab").forEach((btn) => {
      btn.addEventListener("click", () => {
        syncTypeKey = btn.getAttribute("data-sync-type");
        const route = parseRoute();
        location.hash = route.sub ? `#sync/${syncTypeKey}` : `#sync`;
        renderSyncView();
      });
    });

    $("#btn-validate").addEventListener("click", () => runSync(type.validate, "Validação concluída"));
    $("#btn-sync").addEventListener("click", () => {
      if (!confirm("Gravar alterações no GLPI? Confirme que já validou a planilha.")) return;
      runSync(type.sync, "Sincronização concluída");
    });
  }

  async function runSync(path, successMsg) {
    if (!selectedFile) {
      toast("Selecione um arquivo primeiro.", "error");
      return;
    }
    const resultEl = $("#sync-result");
    resultEl.innerHTML = "<h3>Resultado</h3><p class=\"hint\">Processando…</p>";
    const buttons = $("#btn-validate");
    const syncBtn = $("#btn-sync");
    buttons.disabled = true;
    syncBtn.disabled = true;
    try {
      const report = await GlpiApi.postMultipart(path, selectedFile);
      resultEl.innerHTML = "<h3>Resultado</h3>";
      const inner = document.createElement("div");
      renderSyncReport(report, inner);
      resultEl.appendChild(inner);
      toast(successMsg, "success");
    } catch (e) {
      resultEl.innerHTML = `<h3>Resultado</h3><p class="hint" style="color:var(--danger)">${escapeHtml(e.message)}</p>`;
      toast(e.message, "error");
    } finally {
      const b1 = $("#btn-validate");
      const b2 = $("#btn-sync");
      if (b1) b1.disabled = false;
      if (b2) b2.disabled = false;
    }
  }

  async function renderInventory() {
    const el = $("#view-inventory");
    el.innerHTML = '<p class="hint">Carregando inventário…</p>';

    const sections = [];

    try {
      const computers = await GlpiApi.get("/api/computers/summary?range=0-499");
      sections.push({
        title: "Computers",
        filter: true,
        items: computers,
      });
    } catch (e) {
      sections.push({ title: "Computers", error: e.message });
    }

    for (const key of ASSET_KEYS) {
      try {
        const items = await GlpiApi.get(`/api/custom-assets/${key}/summary?range=0-499`);
        const cfg = assetConfig.find((c) => c.key === key);
        sections.push({
          title: cfg?.key ? key.charAt(0).toUpperCase() + key.slice(1) : key,
          filter: true,
          items,
          assetKey: key,
        });
      } catch (e) {
        sections.push({ title: key, error: e.message });
      }
    }

    let html = "";
    for (const sec of sections) {
      if (sec.error) {
        html += `<div class="card" style="margin-bottom:1rem"><h2>${escapeHtml(sec.title)}</h2><p class="hint">${escapeHtml(sec.error)}</p></div>`;
        continue;
      }
      const count = sec.items?.length ?? 0;
      html += `
        <div class="card inv-section" style="margin-bottom:1rem" data-section="${escapeHtml(sec.title)}">
          <h2>${escapeHtml(sec.title)} <span class="badge badge-muted">${count}</span></h2>
          <input type="search" class="inv-filter" placeholder="Filtrar por id ou nome…" style="width:100%;max-width:320px;margin-bottom:0.75rem;padding:0.5rem 0.75rem;border-radius:8px;border:1px solid var(--border);background:var(--bg);color:var(--text)">
          <div class="inv-table">${renderIdNameTable(sec.items)}</div>
        </div>
      `;
    }
    el.innerHTML = html || '<p class="empty">Nenhum dado.</p>';

    $$(".inv-section").forEach((section) => {
      const input = section.querySelector(".inv-filter");
      const tableWrap = section.querySelector(".inv-table");
      const original = tableWrap.innerHTML;
      input.addEventListener("input", () => {
        const q = input.value.trim().toLowerCase();
        if (!q) {
          tableWrap.innerHTML = original;
          return;
        }
        const card = section.closest(".card");
        const title = section.getAttribute("data-section");
        const sec = sections.find((s) => s.title === title);
        if (!sec?.items) return;
        const filtered = sec.items.filter(
          (r) => String(r.id).includes(q) || (r.name || "").toLowerCase().includes(q)
        );
        tableWrap.innerHTML = renderIdNameTable(filtered);
      });
    });
  }

  function renderLookups() {
    const el = $("#view-lookups");
    const options = LOOKUPS.map((l, i) => `<option value="${l.path}" ${i === 0 ? "selected" : ""}>${escapeHtml(l.label)}</option>`).join("");

    el.innerHTML = `
      <div class="card">
        <h2>Listas do GLPI (id + nome)</h2>
        <p class="hint">Use os valores nas colunas das planilhas (ex.: responsável = login do usuário).</p>
        <div class="form-row">
          <div class="field" style="min-width:240px">
            <label for="lookup-select">Tipo</label>
            <select id="lookup-select">${options}</select>
          </div>
          <button type="button" class="btn btn-primary" id="btn-load-lookup">Carregar</button>
          <button type="button" class="btn btn-secondary" id="btn-export-csv">Exportar CSV</button>
        </div>
        <div id="lookup-result"><p class="hint">Clique em Carregar.</p></div>
      </div>
    `;

    let lastItems = [];

    $("#btn-load-lookup").addEventListener("click", async () => {
      const path = $("#lookup-select").value;
      const out = $("#lookup-result");
      out.innerHTML = "<p class=\"hint\">Carregando…</p>";
      try {
        lastItems = await GlpiApi.get(path + "?range=0-999");
        out.innerHTML = renderIdNameTable(lastItems);
        toast(`${lastItems.length} registros`, "success");
      } catch (e) {
        out.innerHTML = `<p class="hint" style="color:var(--danger)">${escapeHtml(e.message)}</p>`;
        toast(e.message, "error");
      }
    });

    $("#btn-export-csv").addEventListener("click", () => {
      if (!lastItems.length) {
        toast("Carregue uma lista antes.", "error");
        return;
      }
      const lines = ["id;name", ...lastItems.map((r) => `${r.id};${(r.name || "").replace(/;/g, ",")}`)];
      const blob = new Blob(["\ufeff" + lines.join("\n")], { type: "text/csv;charset=utf-8" });
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = "lookup-export.csv";
      a.click();
      URL.revokeObjectURL(a.href);
      toast("CSV exportado", "success");
    });
  }

  function renderItemView() {
    const el = $("#view-item");
    const assetOptions = ASSET_KEYS.map((k) => `<option value="${k}">${k}</option>`).join("");

    el.innerHTML = `
      <div class="card">
        <h2>Consultar item no GLPI</h2>
        <div class="form-row">
          <div class="field">
            <label for="item-asset">Tipo</label>
            <select id="item-asset">${assetOptions}</select>
          </div>
          <div class="field">
            <label for="item-id">ID no GLPI</label>
            <input type="number" id="item-id" min="1" placeholder="ex.: 1">
          </div>
          <button type="button" class="btn btn-primary" id="btn-load-item">Carregar</button>
        </div>
        <p class="hint">Computers: use a aba Inventário ou <code>GET /api/computers/{id}</code> (ver Documentação).</p>
        <div id="item-result"></div>
      </div>
    `;

    $("#btn-load-item").addEventListener("click", async () => {
      const key = $("#item-asset").value;
      const id = parseInt($("#item-id").value, 10);
      const out = $("#item-result");
      if (!id || id < 1) {
        toast("Informe um ID válido.", "error");
        return;
      }
      out.innerHTML = "<p class=\"hint\">Carregando…</p>";
      try {
        const data = await GlpiApi.get(`/api/custom-assets/${key}/items/${id}`);
        const keys = data.customFieldKeys || [];
        out.innerHTML = `
          <p class="hint">Itemtype: <code class="mono">${escapeHtml(data.itemType)}</code> · mutation: <code class="mono">${escapeHtml(data.mutationItemType || "")}</code></p>
          ${keys.length ? `<p class="hint">Campos custom: ${keys.map((k) => `<code>${escapeHtml(k)}</code>`).join(", ")}</p>` : ""}
          <pre class="json mono">${escapeHtml(JSON.stringify(data.item, null, 2))}</pre>
        `;
      } catch (e) {
        out.innerHTML = `<p class="hint" style="color:var(--danger)">${escapeHtml(e.message)}</p>`;
      }
    });
  }

  async function navigate() {
    const { view, sub } = parseRoute();
    if (view === "sync" && sub && SYNC_TYPES.some((t) => t.key === sub)) {
      syncTypeKey = sub;
    }
    showView(view);

    await loadAssetConfig();

    switch (view) {
      case "dashboard":
        await renderDashboard();
        break;
      case "sync":
        renderSyncView();
        break;
      case "inventory":
        await renderInventory();
        break;
      case "lookups":
        renderLookups();
        break;
      case "item":
        renderItemView();
        break;
      case "docs":
        $("#view-docs").innerHTML = "<p class=\"hint\">Carregando documentação…</p>";
        await ApiDocs.render($("#view-docs"));
        break;
      default:
        showView("dashboard");
        await renderDashboard();
    }
  }

  window.addEventListener("hashchange", () => navigate());
  $("#btn-refresh").addEventListener("click", async () => {
    await refreshApiStatus();
    await navigate();
    toast("Atualizado", "success");
  });

  function updateApiKeyBadge() {
    const badge = $("#api-key-badge");
    if (!badge) return;
    const key = GlpiApi.getApiKey();
    if (key) {
      badge.hidden = false;
      badge.textContent = "Chave configurada";
      badge.className = "badge badge-ok";
    } else {
      badge.hidden = true;
    }
  }

  function bindApiKeyDialog() {
    const dialog = $("#api-key-dialog");
    const input = $("#api-key-input");
    $("#btn-api-key")?.addEventListener("click", () => {
      input.value = GlpiApi.getApiKey();
      dialog.showModal();
    });
    $("#btn-close-api-key")?.addEventListener("click", () => dialog.close());
    $("#btn-clear-api-key")?.addEventListener("click", () => {
      GlpiApi.setApiKey("");
      input.value = "";
      updateApiKeyBadge();
      toast("Chave removida", "success");
    });
    dialog?.querySelector("form")?.addEventListener("submit", (e) => {
      e.preventDefault();
      GlpiApi.setApiKey(input.value);
      updateApiKeyBadge();
      dialog.close();
      toast("Chave salva neste navegador", "success");
    });
  }

  async function ensureApiKeyIfRequired() {
    try {
      const status = await GlpiApi.securityStatus();
      if (status.apiKeyRequired && !GlpiApi.getApiKey()) {
        toast("Servidor exige chave API — clique em «Chave API»", "error");
        $("#api-key-dialog")?.showModal();
      }
    } catch {
      /* servidor antigo ou offline */
    }
  }

  (async function init() {
    bindApiKeyDialog();
    await refreshApiStatus();
    await ensureApiKeyIfRequired();
    updateApiKeyBadge();
    if (!location.hash) location.hash = "#dashboard";
    await navigate();
  })();
})();
