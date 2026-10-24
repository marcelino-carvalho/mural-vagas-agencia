/**
 * Lógica da TV do Painel de Mutirões
 * Agência do Trabalhador de Curitiba
 */
  const TEMPO_POR_TELA = 30000;
  const TEMPO_ATUALIZAR_QR = 60000;
  const TEMPO_ATUALIZAR_DADOS = 60000;
  const MODULO = "MUTIROES";
  let telaAtual = "1";
  let vagasCache = [];
  let telasDisponiveis = ["1"];
  let titulosMutirao = {
    "1": "PAINEL DE MUTIRÕES - TELA 1",
    "2": "PAINEL DE MUTIRÕES - TELA 2",
    "3": "PAINEL DE MUTIRÕES - TELA 3"
  };

  function hexToRgb(hex) {
    const value = (hex || '').replace('#', '');
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
    const toHex = (n) => n.toString(16).padStart(2, '0');
    return `#${toHex(mix(a.r, b.r))}${toHex(mix(a.g, b.g))}${toHex(mix(a.b, b.b))}`;
  }

  function aplicarTemaMutirao(cor) {
    const tema = /^#[0-9a-fA-F]{6}$/.test(cor || '') ? cor : '#1f4f8a';
    document.documentElement.style.setProperty('--brand', tema);
    document.documentElement.style.setProperty('--bg', mixColor(tema, '#ffffff', 0.92));
  }

  function aplicarCorRodape(cor) {
    const rodape = /^#[0-9a-fA-F]{6}$/.test(cor || '') ? cor : '#6889ec';
    document.documentElement.style.setProperty('--footer-bg', rodape);
  }

  const PAINEL_INICIO_MINUTOS = (7 * 60) + 10;
  const PAINEL_FIM_MINUTOS = (17 * 60) + 10;
  let sse = null;

  function painelPodeConsultar(agora = new Date()) {
    const dia = agora.getDay();
    const minutos = (agora.getHours() * 60) + agora.getMinutes();
//    return dia >= 1 && dia <= 5 && minutos >= PAINEL_INICIO_MINUTOS && minutos < PAINEL_FIM_MINUTOS;
    return dia >= 1 && dia <= 6 && minutos >= PAINEL_INICIO_MINUTOS && minutos < PAINEL_FIM_MINUTOS;
  }

  async function carregarTitulos() {
    if (!painelPodeConsultar()) return;
    try {
      const res = await fetch('/api/infos');
      const infos = await res.json();
      titulosMutirao = {
        "1": infos.mutiraoTituloTela1 || "PAINEL DE MUTIRÕES - TELA 1",
        "2": infos.mutiraoTituloTela2 || "PAINEL DE MUTIRÕES - TELA 2",
        "3": infos.mutiraoTituloTela3 || "PAINEL DE MUTIRÕES - TELA 3"
      };
      aplicarTemaMutirao(infos.mutiraoTemaCor);
      aplicarCorRodape(infos.mutiraoRodapeCor);
    } catch (_) {}
  }

  async function carregarVagas() {
    if (!painelPodeConsultar()) return;
    const res = await fetch(`/api/vagas/modulo?tipo=${MODULO}`);
    vagasCache = await res.json();
    const telasComVagas = Array.isArray(vagasCache)
      ? vagasCache.map(vaga => String(vaga.tela || '1'))
      : [];
    telasDisponiveis = ["1", "2", "3"].filter(tela => telasComVagas.includes(tela));
    if (!telasDisponiveis.length) {
      telasDisponiveis = ["1"];
    }
    if (!telasDisponiveis.includes(telaAtual)) {
      telaAtual = telasDisponiveis[0];
    }
    atualizarVisibilidadeNavegacao();
  }

  function renderizarTela() {
    const container = document.getElementById('lista-vagas');
    document.getElementById('titulo-painel').textContent = titulosMutirao[telaAtual] || `PAINEL DE MUTIRÕES - TELA ${telaAtual}`;
    container.innerHTML = '';

    const vagasDaTela = (Array.isArray(vagasCache) ? vagasCache : []).filter(vaga => (vaga.tela || '1') === telaAtual);

    if (!vagasDaTela.length) {
      container.innerHTML = `<div class="empty">Nenhum mutirão disponível na Tela ${telaAtual}.</div>`;
      return;
    }

    vagasDaTela.slice(0, 30).forEach(vaga => {
      const card = document.createElement('div');
      card.className = 'card-vaga';
      card.innerHTML = `
        <div class="card-top">
          <span class="codigo-badge">${vaga.empresa || '-'}</span>
          <span class="local-texto">${vaga.local ? `📍${vaga.local}` : ''}</span>
        </div>
        <div class="card-content">
          <div class="info-texto">${vaga.atribuicoes || '-'}</div>
        </div>
      `;
      container.appendChild(card);
    });
  }

  function animarBarra() {
    const barra = document.getElementById('barra-progresso');
    barra.style.transition = 'none';
    barra.style.width = '0%';
    setTimeout(() => {
      barra.style.transition = `width ${TEMPO_POR_TELA}ms linear`;
      barra.style.width = '100%';
    }, 50);
  }

  function alternarTela() {
    const indiceAtual = telasDisponiveis.indexOf(telaAtual);
    const proximoIndice = (indiceAtual + 1) % telasDisponiveis.length;
    telaAtual = telasDisponiveis[proximoIndice];
    renderizarTela();
    animarBarra();
  }

  function irParaTela(delta) {
    if (telasDisponiveis.length <= 1) {
      return;
    }
    const indiceAtual = telasDisponiveis.indexOf(telaAtual);
    const novoIndice = (indiceAtual + delta + telasDisponiveis.length) % telasDisponiveis.length;
    telaAtual = telasDisponiveis[novoIndice];
    renderizarTela();
    animarBarra();
  }

  function atualizarVisibilidadeNavegacao() {
    const nav = document.getElementById('nav-buttons');
    if (!nav) return;
    nav.style.display = telasDisponiveis.length > 1 ? 'flex' : 'none';
  }

  let hideButtonsTimer;
  function mostrarBotoesTemporario() {
    if (telasDisponiveis.length <= 1) return;
    const nav = document.getElementById('nav-buttons');
    nav.classList.add('show');
    clearTimeout(hideButtonsTimer);
    hideButtonsTimer = setTimeout(() => nav.classList.remove('show'), 1200);
  }

  async function atualizarQrRodape() {
    if (!painelPodeConsultar()) return;
    try {
      const res = await fetch('/api/mural/token?m=MUTIROES');
      const dados = await res.json();
      document.getElementById('qr-mural').src = `${dados.qrUrl}&v=${Date.now()}`;
    } catch (e) {
      console.error('Falha ao atualizar QR do mural de mutirões:', e);
    }
  }

  async function atualizarTudo() {
    if (!painelPodeConsultar()) {
      renderizarTela();
      animarBarra();
      return;
    }
    try {
      await carregarTitulos();
      await carregarVagas();
      renderizarTela();
      animarBarra();
    } catch (e) {
      document.getElementById('lista-vagas').innerHTML = '<div class="empty">Erro de conexão com o servidor.</div>';
    }
  }

  function atualizarConexaoSse() {
    if (!painelPodeConsultar()) {
      if (sse) {
        sse.close();
        sse = null;
      }
      return;
    }
    if (sse) return;
    sse = new EventSource('/api/vagas/stream');
    sse.onmessage = () => atualizarTudo();
    sse.onerror = () => {};
  }

  document.getElementById('btn-prev').addEventListener('click', () => irParaTela(-1));
  document.getElementById('btn-next').addEventListener('click', () => irParaTela(1));
  window.addEventListener('mousemove', mostrarBotoesTemporario);
  window.addEventListener('touchstart', mostrarBotoesTemporario, { passive: true });

  atualizarTudo();
  atualizarQrRodape();
  setInterval(atualizarQrRodape, TEMPO_ATUALIZAR_QR);
  setInterval(atualizarTudo, TEMPO_ATUALIZAR_DADOS);
  setInterval(alternarTela, TEMPO_POR_TELA);
  atualizarConexaoSse();
  setInterval(atualizarConexaoSse, 60000);
