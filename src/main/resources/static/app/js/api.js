/**
 * Cliente HTTP para a API glpi-integration (mesma origem).
 * Chave opcional: sessionStorage "glpiApiKey" (header X-API-Key).
 */
const GlpiApi = (() => {
  const base = "";
  const STORAGE_KEY = "glpiApiKey";

  function getApiKey() {
    try {
      return sessionStorage.getItem(STORAGE_KEY) || "";
    } catch {
      return "";
    }
  }

  function setApiKey(value) {
    try {
      if (value && value.trim()) {
        sessionStorage.setItem(STORAGE_KEY, value.trim());
      } else {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      /* ignore */
    }
  }

  function authHeaders(extra = {}) {
    const headers = { Accept: "application/json", ...extra };
    const key = getApiKey();
    if (key) {
      headers["X-API-Key"] = key;
    }
    return headers;
  }

  async function request(path, options = {}) {
    const url = base + path;
    const response = await fetch(url, {
      ...options,
      headers: authHeaders(options.headers || {}),
    });

    const contentType = response.headers.get("content-type") || "";
    let body = null;
    if (contentType.includes("application/json")) {
      body = await response.json();
    } else {
      body = await response.text();
    }

    if (!response.ok) {
      const message =
        typeof body === "object" && body !== null
          ? body.message || body.error || JSON.stringify(body)
          : String(body || response.statusText);
      const err = new Error(message);
      err.status = response.status;
      err.body = body;
      throw err;
    }
    return body;
  }

  function get(path) {
    return request(path, { method: "GET" });
  }

  function postJson(path, data) {
    return request(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data || {}),
    });
  }

  function putJson(path, data) {
    return request(path, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data || {}),
    });
  }

  async function postMultipart(path, file) {
    const form = new FormData();
    form.append("file", file);
    const url = base + path;
    const headers = authHeaders();
    const response = await fetch(url, {
      method: "POST",
      headers,
      body: form,
    });
    const body = await response.json();
    if (!response.ok) {
      const err = new Error(body.message || body.error || response.statusText);
      err.status = response.status;
      err.body = body;
      throw err;
    }
    return body;
  }

  async function securityStatus() {
    const url = base + "/api/security/status";
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    return response.json();
  }

  return { get, postJson, putJson, postMultipart, request, getApiKey, setApiKey, securityStatus };
})();
