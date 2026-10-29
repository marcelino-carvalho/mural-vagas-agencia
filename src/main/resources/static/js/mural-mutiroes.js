/**
 * Lógica do Mural de Mutirões no Smartphone do Cidadão
 * Agência do Trabalhador de Curitiba
 */
  const MODULO = "MUTIROES";
  const contentEl = document.getElementById("content");
  const statusEl = document.getElementById("status");
  const sheetEl = document.getElementById("sheet");
  const sheetBodyEl = document.getElementById("sheetBody");
  const sheetTitleEl = document.getElementById("sheetTitle");
  let dataAtual = null;
  let expiraEm = 0;
  let sessionToken = null;
  const CACHE_TTL_MS = 60000;
  const CACHE_KEY_PREFIX = "mural_mutiroes_cache_v1:";

  function atualizarAlturaApp() {
    const altura = window.visualViewport ? window.visualViewport.height : window.innerHeight;
    document.documentElement.style.setProperty("--app-height", `${Math.round(altura)}px`);
  }

  function chaveCacheMural(token) {
    return `${CACHE_KEY_PREFIX}${token || "anon"}`;
  }

  function salvarCacheMural(token, payload) {
    if (!token || !payload) return;
    try {
      localStorage.setItem(chaveCacheMural(token), JSON.stringify({
        savedAt: Date.now(),
        payload
      }));
    } catch (_) {}
  }

  function lerCacheMural(token, ttlMs = CACHE_TTL_MS) {
    if (!token) return null;
    try {
      const raw = localStorage.getItem(chaveCacheMural(token));
      if (!raw) return null;
      const cache = JSON.parse(raw);
      const savedAt = Number(cache && cache.savedAt);
      if (!savedAt || !cache.payload) return null;
      if (Date.now() - savedAt > ttlMs) return null;
      return cache.payload;
    } catch (_) {
      return null;
    }
  }

  function aplicarDadosMural(payload) {
    dataAtual = payload;
    if (dataAtual && dataAtual.infos) {
      aplicarTemaMutirao(dataAtual.infos.mutiraoTemaCor);
    }
    expiraEm = dataAtual.expiresAtEpochSeconds || 0;
    renderConteudo();
    statusSessao();
  }

  function hexToRgb(hex) {
    const value = (hex || "").replace("#", "");
    if (value.length !== 6) return null;
    return {
      r: parseInt(value.slice(0, 2), 16),
      g: parseInt(value.slice(2, 4), 16),
      b: parseInt(value.slice(4, 6), 16)
    };
  }

  function mixColor(hex, targetHex, weight) {
    const a = hexToRgb(hex);
    const b = hexToRgb(targetHex);
    if (!a || !b) return hex;
    const mix = (x, y) => Math.round(x + (y - x) * weight);
    const toHex = (n) => n.toString(16).padStart(2, "0");
    return `#${toHex(mix(a.r, b.r))}${toHex(mix(a.g, b.g))}${toHex(mix(a.b, b.b))}`;
  }

  function aplicarTemaMutirao(cor) {
    const tema = /^#[0-9a-fA-F]{6}$/.test(cor || "") ? cor : "#1f4f8a";
    document.documentElement.style.setProperty("--brand", tema);
    document.documentElement.style.setProperty("--brand-deep", mixColor(tema, "#000000", 0.25));
    document.documentElement.style.setProperty("--brand-soft", mixColor(tema, "#ffffff", 0.86));
    document.documentElement.style.setProperty("--bg", mixColor(tema, "#ffffff", 0.93));
    document.documentElement.style.setProperty("--panel", mixColor(tema, "#ffffff", 0.96));
    document.documentElement.style.setProperty("--line", mixColor(tema, "#ffffff", 0.84));
  }

  function escaparHtml(valor) {
    return (valor || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
  }

  function sanitizeRichHtml(html) {
    const root = document.createElement("div");
    root.innerHTML = html || "";

    function walk(node) {
      if (node.nodeType === Node.TEXT_NODE) {
        return escaparHtml(node.textContent || "");
      }

      if (node.nodeType !== Node.ELEMENT_NODE) {
        return "";
      }

      const tag = node.tagName.toLowerCase();
      const children = Array.from(node.childNodes).map(walk).join("");
      const fontWeight = (node.style && node.style.fontWeight ? node.style.fontWeight : "").toLowerCase();
      const isBold = tag === "b" || tag === "strong" || fontWeight === "bold" || Number(fontWeight) >= 600;

      if (tag === "br") {
        return "<br>";
      }

      if (tag === "div" || tag === "p") {
        return children ? `${children}<br>` : "";
      }

      if (isBold) {
        return `<strong>${children}</strong>`;
      }

      return children;
    }

    const sanitized = Array.from(root.childNodes).map(walk).join("");
    return sanitized.replace(/(<br>\s*)+$/i, "") || "-";
  }

  function formatarTextoPlano(html) {
    const temp = document.createElement("div");
    temp.innerHTML = html || "";
    return (temp.textContent || temp.innerText || "").trim();
  }

  function formatarDataHora(epochSeconds) {
    if (!epochSeconds) return "-";
    return new Date(epochSeconds * 1000).toLocaleString("pt-BR");
  }

  function statusSessao() {
    if (!expiraEm) return;
    const agora = Math.floor(Date.now() / 1000);
    const restante = Math.max(0, expiraEm - agora);
    const min = Math.floor(restante / 60);
    const sec = restante % 60;
    statusEl.textContent = `Ativo até ${formatarDataHora(expiraEm)} (${min}m ${sec}s)`;
  }

  function abrirDetalhes(vaga) {
    sheetTitleEl.textContent = vaga.empresa || "Mutirão";
    sheetBodyEl.innerHTML = `
      <div class="detail-grid">
        <div class="detail-card">
          <div class="label">Empresa</div>
          <div class="value">${escaparHtml(vaga.empresa || "-")}</div>
        </div>
        <div class="detail-card">
          <div class="label">Local</div>
          <div class="value">${escaparHtml(vaga.local || "-")}</div>
        </div>
        <div class="detail-card">
          <div class="label">Informações</div>
          <div class="value">${sanitizeRichHtml(vaga.atribuicoes || "-")}</div>
        </div>
      </div>
    `;
    sheetEl.classList.add("show");
  }

  function renderConteudo() {
    const vagas = (dataAtual && dataAtual.vagas) || [];
    if (!vagas.length) {
      contentEl.innerHTML = "<div class='empty'>Nenhum mutirão disponível no momento.</div>";
      return;
    }

    const html = vagas.map(v => {
      const preview = formatarTextoPlano(v.atribuicoes || "-");
      return `
        <article class="card" data-id="${v.id}">
          <div class="card-top">
            <h2 class="empresa">${escaparHtml(v.empresa || "-")}</h2>
            <div class="local">${v.local ? `📍 ${escaparHtml(v.local)}` : ""}</div>
          </div>
          <div class="preview">${escaparHtml(preview || "-")}</div>
          <div class="card-bottom">
            <span class="hint">Painel exclusivo de mutirões</span>
            <span class="cta">Abrir detalhes</span>
          </div>
        </article>
      `;
    }).join("");

    contentEl.innerHTML = `<section class="list">${html}</section>`;
    contentEl.querySelectorAll(".card").forEach(el => {
      const id = Number(el.getAttribute("data-id"));
      const vaga = vagas.find(item => item.id === id);
      el.addEventListener("click", () => abrirDetalhes(vaga || {}));
    });
  }

  async function apiFetch(url) {
    return fetch(url, { method: "GET", cache: "no-store", credentials: "same-origin" });
  }

  const PAINEL_INICIO_MINUTOS = (7 * 60) + 10;
  const PAINEL_FIM_MINUTOS = (17 * 60) + 10;

  function painelPodeConsultar(agora = new Date()) {
    const dia = agora.getDay();
    const minutos = (agora.getHours() * 60) + agora.getMinutes();
    //return dia >= 1 && dia <= 5 && minutos >= PAINEL_INICIO_MINUTOS && minutos < PAINEL_FIM_MINUTOS;
    return dia >= 1 && dia <= 6 && minutos >= PAINEL_INICIO_MINUTOS && minutos < PAINEL_FIM_MINUTOS;
  }

  async function carregarDados() {
    if (!painelPodeConsultar()) {
      const cache = dataAtual || lerCacheMural(sessionToken, Number.MAX_SAFE_INTEGER);
      if (cache) {
        aplicarDadosMural(cache);
        statusEl.textContent = "Fora do horario de consulta. Exibindo dados em cache.";
        return;
      }
      throw new Error("Consultas disponiveis de segunda a sexta, das 07:10 as 17:10.");
    }
    const res = await apiFetch(`/api/mural/data?t=${encodeURIComponent(sessionToken)}&m=${MODULO}`);
    if (!res.ok) throw new Error("Acesso negado");
    const payload = await res.json();
    aplicarDadosMural(payload);
    salvarCacheMural(sessionToken, payload);
  }

  async function iniciarSessaoComQr(token) {
    if (!token) throw new Error("Token ausente no link");
    const res = await apiFetch(`/api/mural/session?t=${encodeURIComponent(token)}`);
    if (!res.ok) throw new Error("QR Code inválido. Escaneie novamente na TV.");
    const s = await res.json();
    sessionToken = s.token;
    expiraEm = s.expiresAtEpochSeconds || 0;
    window.history.replaceState({}, "", `${window.location.pathname}?t=${encodeURIComponent(sessionToken)}`);
  }

  async function tentarUsarSessaoAtual(token) {
    if (!token) return false;
    const res = await apiFetch(`/api/mural/validate?t=${encodeURIComponent(token)}`);
    if (!res.ok) return false;
    const v = await res.json();
    if (v.valid) {
      sessionToken = token;
      expiraEm = v.expiresAtEpochSeconds || 0;
      return true;
    }
    if (v.wrongNetwork) throw new Error("Este acesso só funciona dentro da agência.");
    return false;
  }

  function renderErro(msg) {
    contentEl.innerHTML = `<div class="error">${msg}</div>`;
    statusEl.textContent = "Acesso indisponível.";
  }

  document.getElementById("sheetClose").addEventListener("click", () => sheetEl.classList.remove("show"));
  sheetEl.addEventListener("click", (e) => {
    if (e.target === sheetEl) sheetEl.classList.remove("show");
  });

  window.addEventListener("resize", atualizarAlturaApp);
  window.addEventListener("orientationchange", atualizarAlturaApp);
  if (window.visualViewport) {
    window.visualViewport.addEventListener("resize", atualizarAlturaApp);
  }
  window.addEventListener("pageshow", (event) => {
    if (!event.persisted) return;
    atualizarAlturaApp();
    const cache = lerCacheMural(sessionToken);
    if (cache) aplicarDadosMural(cache);
    carregarDados().catch(() => {});
  });
  atualizarAlturaApp();

  (async () => {
    try {
      const tokenAtual = new URLSearchParams(window.location.search).get("t");
      const sessaoValida = await tentarUsarSessaoAtual(tokenAtual);
      if (!sessaoValida) await iniciarSessaoComQr(tokenAtual);
      const cache = lerCacheMural(sessionToken);
      if (cache) {
        aplicarDadosMural(cache);
        statusEl.textContent = "Exibindo dados em cache. Atualizando...";
      }
      await carregarDados().catch((e) => {
        if (cache) {
          statusEl.textContent = "Sem conexão no momento. Exibindo dados recentes em cache.";
          return;
        }
        throw e;
      });
      setInterval(statusSessao, 1000);
      setInterval(async () => {
        try {
          await carregarDados();
        } catch (_) {
          const cacheAtual = lerCacheMural(sessionToken);
          if (cacheAtual) {
            aplicarDadosMural(cacheAtual);
            statusEl.textContent = "Falha temporária de rede. Exibindo dados recentes em cache.";
            return;
          }
          renderErro("Sessão expirada ou indisponível. Escaneie novamente o QR do painel de mutirões.");
        }
      }, 60000);
    } catch (e) {
      renderErro(e.message || "Não foi possível carregar o mural.");
    }
  })();
