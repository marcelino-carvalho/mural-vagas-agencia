/**
 * Lógica do Mural Digital no Smartphone do Cidadão
 * Agência do Trabalhador de Curitiba
 */
  const categorias = [
    { key: "CONVENCIONAL", label: "Convencional" },
    { key: "SUPERIOR", label: "Superior" },
    { key: "PCD", label: "PCD" },
    { key: "INFO", label: "Info" }
  ];
  const coresCategoria = {
    CONVENCIONAL: "#0056b3",
    SUPERIOR: "#d35400",
    PCD: "#16a085",
    INFO: "#6f42c1"
  };

  const qrToken = new URLSearchParams(window.location.search).get("t");
  const tabsEl = document.getElementById("tabs");
  const contentEl = document.getElementById("content");
  const statusEl = document.getElementById("status");
  const sheetEl = document.getElementById("sheet");
  const sheetBodyEl = document.getElementById("sheetBody");
  const sheetTitleEl = document.getElementById("sheetTitle");
  let dataAtual = null;
  let abaAtual = "CONVENCIONAL";
  let expiraEm = 0;
  let sessionToken = null;
  const CACHE_TTL_MS = 60000;
  const CACHE_KEY_PREFIX = "mural_data_cache_v1:";
  const PAINEL_INICIO_MINUTOS = (7 * 60) + 10;
  const PAINEL_FIM_MINUTOS = (17 * 60) + 10;

  function painelPodeConsultar(agora = new Date()) {
    const dia = agora.getDay();
    const minutos = (agora.getHours() * 60) + agora.getMinutes();
    return dia >= 1 && dia <= 6 && minutos >= PAINEL_INICIO_MINUTOS && minutos < PAINEL_FIM_MINUTOS;
  //return dia >= 1 && dia <= 5 && minutos >= PAINEL_INICIO_MINUTOS && minutos < PAINEL_FIM_MINUTOS;
  }

  function atualizarAlturaApp() {
    const altura = window.visualViewport ? window.visualViewport.height : window.innerHeight;
    document.documentElement.style.setProperty("--app-height", `${Math.round(altura)}px`);
  }

  async function apiFetch(url) {
    return fetch(url, {
      method: "GET",
      cache: "no-store",
      credentials: "same-origin"
    });
  }

  function chaveCacheMural(token) {
    return `${CACHE_KEY_PREFIX}${token || "anon"}`;
  }

  function salvarCacheMural(token, payload) {
    if (!token || !payload) return;
    try {
      const cache = {
        savedAt: Date.now(),
        payload
      };
      localStorage.setItem(chaveCacheMural(token), JSON.stringify(cache));
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
      const idade = Date.now() - savedAt;
      if (idade > ttlMs) return null;
      return cache.payload;
    } catch (_) {
      return null;
    }
  }

  function aplicarDadosMural(payload) {
    dataAtual = payload;
    expiraEm = dataAtual.expiresAtEpochSeconds || 0;
    renderTabs();
    renderConteudo();
    statusSessao();
  }

  function corDaCategoria(cat) {
    return coresCategoria[cat] || "#0a66d1";
  }

  function hexParaRgba(hex, alpha) {
    const h = (hex || "").replace("#", "");
    if (h.length !== 6) return `rgba(10, 102, 209, ${alpha})`;
    const r = parseInt(h.slice(0, 2), 16);
    const g = parseInt(h.slice(2, 4), 16);
    const b = parseInt(h.slice(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }

  function formatarDataHora(epochSeconds) {
    if (!epochSeconds) return "-";
    return new Date(epochSeconds * 1000).toLocaleString("pt-BR");
  }

  function formatarDataBR(valor) {
    if (!valor) return "-";
    if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
      const [y, m, d] = valor.split("-");
      return `${d}/${m}/${y}`;
    }
    const dt = new Date(valor);
    if (!isNaN(dt)) {
      return dt.toLocaleDateString("pt-BR");
    }
    return valor;
  }

  function statusSessao() {
    if (!expiraEm) return;
    const agora = Math.floor(Date.now() / 1000);
    const restante = Math.max(0, expiraEm - agora);
    const min = Math.floor(restante / 60);
    const sec = restante % 60;
    statusEl.textContent = `Acesso ativo ate ${formatarDataHora(expiraEm)} (${min}m ${sec}s)`;
  }

  function renderTabs() {
    tabsEl.innerHTML = "";
    categorias.forEach(cat => {
      const btn = document.createElement("button");
      btn.className = "tab-btn" + (cat.key === abaAtual ? " active" : "");
      btn.textContent = cat.label;
      if (cat.key === abaAtual) {
        const cor = corDaCategoria(cat.key);
        btn.style.backgroundColor = cor;
        btn.style.borderColor = cor;
      }
      btn.addEventListener("click", () => {
        abaAtual = cat.key;
        renderTabs();
        renderConteudo();
      });
      tabsEl.appendChild(btn);
    });
  }

  function abrirDetalhes(vaga) {
    sheetTitleEl.textContent = vaga.titulo || "Vaga";
    sheetBodyEl.innerHTML = `
      <div class="row"><div class="label">Codigo</div><div class="value">${vaga.codigo || "-"}</div></div>
      <div class="row"><div class="label">Categoria</div><div class="value">${vaga.categoria || "-"}</div></div>
      <div class="row"><div class="label">Salario</div><div class="value">R$ ${vaga.salario || "-"}</div></div>
      <div class="row"><div class="label">Local</div><div class="value">${vaga.local || "-"}</div></div>
      <div class="row"><div class="label">Experiencia</div><div class="value">${vaga.experiencia || "-"}</div></div>
      <div class="row"><div class="label">Beneficios</div><div class="value">${vaga.beneficios || "-"}</div></div>
      <div class="row"><div class="label">Atribuicoes</div><div class="value">${vaga.atribuicoes || "-"}</div></div>
      <div class="row"><div class="label">Validade da vaga</div><div class="value">${formatarDataBR(vaga.validade)}</div></div>
    `;
    sheetEl.classList.add("show");
  }

  function renderInfos() {
    const infos = dataAtual.infos || {};
    const cursos = Array.isArray(infos.cursos) ? infos.cursos : [];
    const processos = Array.isArray(infos.processos) ? infos.processos : [];
    const cronograma = Array.isArray(infos.cronograma) ? infos.cronograma : [];

    contentEl.innerHTML = `
      <section class="infos">
        <article class="info-card">
          <h3 class="info-title c1">Cursos e Qualificacoes</h3>
          <div class="info-body">
            ${cursos.length ? cursos.map(c => `<p><strong>${c.nome || ""}</strong><br>${c.detalhe || ""}</p>`).join("") : "<div class='empty'>Sem cursos cadastrados.</div>"}
          </div>
        </article>
        <article class="info-card">
          <h3 class="info-title c2">Processos na Agencia</h3>
          <div class="info-body">
            ${processos.length ? processos.map(p => `<p><strong>${p.empresa || ""}</strong><br>${p.data || ""}<br>${(p.texto || []).join("<br>")}</p>`).join("<hr>") : "<div class='empty'>Sem processos cadastrados.</div>"}
          </div>
        </article>
        <article class="info-card">
          <h3 class="info-title c3">Calendário</h3>
          <div class="info-body">
            ${cronograma.length ? cronograma.map(c => `
              <div class="cal-item">
                <div class="cal-data">${c.data || ""}</div>
                <div class="cal-evento">${c.evento || ""}</div>
              </div>
            `).join("") : "<div class='empty'>Sem eventos cadastrados.</div>"}
          </div>
        </article>
      </section>
    `;
  }

  function renderVagas(categoria) {
    const vagas = (dataAtual.vagas || []).filter(v => (v.categoria || "").toUpperCase() === categoria);
    const corCategoria = corDaCategoria(categoria);
    const badgeBg = hexParaRgba(corCategoria, 0.16);
    const badgeInk = corCategoria;
    if (!vagas.length) {
      contentEl.innerHTML = "<div class='empty'>Nenhuma vaga nesta categoria agora.</div>";
      return;
    }

    const html = vagas.map(v => `
      <article class="vaga-card" data-id="${v.id}" style="border-left-color:${corCategoria}">
        <div class="vaga-head">
          <span class="badge" style="background:${badgeBg};color:${badgeInk}">${v.codigo || "-"}</span>
          <span class="vaga-local">${v.local || ""}</span>
        </div>
        <h3 class="vaga-title">${v.titulo || "-"}</h3>
        <div class="vaga-meta">
          <span class="vaga-salario">R$ ${v.salario || "-"}</span>
          <span class="vaga-open" style="color:${corCategoria}">Abrir detalhes</span>
        </div>
      </article>
    `).join("");

    contentEl.innerHTML = `<section class="vagas-grid">${html}</section>`;
    contentEl.querySelectorAll(".vaga-card").forEach(el => {
      const id = Number(el.getAttribute("data-id"));
      const vaga = (dataAtual.vagas || []).find(v => v.id === id);
      el.addEventListener("click", () => abrirDetalhes(vaga || {}));
    });
  }

  function renderConteudo() {
    if (!dataAtual) return;
    if (abaAtual === "INFO") {
      renderInfos();
      return;
    }
    renderVagas(abaAtual);
  }

  async function carregarDados() {
    if (!painelPodeConsultar()) {
      const cache = lerCacheMural(sessionToken, Number.MAX_SAFE_INTEGER);
      if (cache) {
        aplicarDadosMural(cache);
        statusEl.textContent = "Fora do horario de consulta. Exibindo dados em cache.";
        return;
      }
      throw new Error("Consultas disponiveis de segunda a sexta, das 07:10 as 17:10.");
    }
    const res = await apiFetch(`/api/mural/data?t=${encodeURIComponent(sessionToken)}`);
    if (!res.ok) throw new Error("Acesso negado");
    const payload = await res.json();
    aplicarDadosMural(payload);
    salvarCacheMural(sessionToken, payload);
  }

  async function iniciarSessaoComQr(token) {
    if (!token) {
      throw new Error("Token ausente no link");
    }
    const res = await apiFetch(`/api/mural/session?t=${encodeURIComponent(token)}`);
    if (!res.ok) throw new Error("QR code invalido. Escaneie novamente na TV.");
    const s = await res.json();
    if (!s.token) {
      throw new Error("Falha ao iniciar sessao do mural.");
    }
    sessionToken = s.token;
    expiraEm = s.expiresAtEpochSeconds || 0;
    const novaUrl = `${window.location.pathname}?t=${encodeURIComponent(sessionToken)}`;
    window.history.replaceState({}, "", novaUrl);
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
    if (v.wrongNetwork) throw new Error("Este acesso so funciona dentro da agencia.");
    if (v.expired) return false;
    if (v.wrongType) return false;
    return false;
  }

  function renderErro(msg) {
    contentEl.innerHTML = `<div class="error">${msg}</div>`;
    statusEl.textContent = "Acesso indisponivel.";
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
      if (!sessaoValida) {
        await iniciarSessaoComQr(tokenAtual);
      }
      const cache = lerCacheMural(sessionToken);
      if (cache) {
        aplicarDadosMural(cache);
        statusEl.textContent = "Exibindo dados em cache. Atualizando...";
      }
      await carregarDados().catch((e) => {
        if (cache) {
          statusEl.textContent = "Sem conexao no momento. Exibindo dados recentes em cache.";
          return;
        }
        throw e;
      });
      setInterval(statusSessao, 1000);
      setInterval(async () => {
        try {
          await carregarDados();
        } catch (e) {
          const cacheAtual = lerCacheMural(sessionToken);
          if (cacheAtual) {
            aplicarDadosMural(cacheAtual);
            statusEl.textContent = "Falha temporaria de rede. Exibindo dados recentes em cache.";
            return;
          }
          renderErro("Sessao expirada ou fora da rede da agencia. Escaneie novamente o QR da TV.");
        }
      }, 60000);
    } catch (e) {
      renderErro(e.message || "Nao foi possivel carregar o mural.");
    }
  })();
</script>
