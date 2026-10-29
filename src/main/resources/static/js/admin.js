/**
 * Lógica Javascript do Painel Administrativo do Mural de Vagas
 * Agência do Trabalhador de Curitiba
 */
    const API_VAGAS = "/api/vagas";
    const API_INFOS = "/api/infos";
    const MODULOS = {
        VAGAS: {
            key: "VAGAS",
            label: "Painel de Vagas",
            badgeClass: "vagas",
            tvUrl: "/tv.html"
        },
        MUTIROES: {
            key: "MUTIROES",
            label: "Painel de Mutirões",
            badgeClass: "mutiroes",
            tvUrl: "/tv-mutiroes.html"
        }
    };

    let moduloAtual = null;
    let editId = null;
    let vagasCache = [];
    let infosCache = null;
    let infosDirty = false;
    let populandoInfos = false;
    let painelVagasView = 'vagas';
    let termoBusca = '';

    function abrirSeletor() {
        document.getElementById('module-picker').style.display = 'flex';
    }

    function fecharSeletor() {
        document.getElementById('module-picker').style.display = 'none';
    }

    function selecionarModulo(modulo, fechar = true) {
        moduloAtual = MODULOS[modulo] ? modulo : "VAGAS";
        const config = MODULOS[moduloAtual];
        const isMutirao = moduloAtual === 'MUTIROES';
        const switcher = document.getElementById('vagas-view-switch');
        const campoBusca = document.getElementById('busca-vagas');
        const statusBusca = document.getElementById('busca-status');

        document.getElementById('group-codigo').classList.toggle('d-none', isMutirao);
        document.getElementById('group-empresa').classList.toggle('d-none', !isMutirao);
        document.getElementById('group-tela-mutirao').style.display = isMutirao ? 'block' : 'none';
        document.getElementById('group-info-mutirao').style.display = isMutirao ? 'block' : 'none';
        document.getElementById('group-titulo').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('group-salario').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('group-escolaridade').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('group-beneficios').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('group-atribuicoes').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('group-validade').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('group-tipo').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('group-experiencia').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('group-expediente').style.display = isMutirao ? 'none' : 'block';
        document.getElementById('mutirao-titulos-shell').style.display = isMutirao ? 'block' : 'none';
        document.getElementById('form-title').textContent = isMutirao ? 'Novo Mutirão' : 'Nova Vaga';
        document.getElementById('form-subtitle').textContent = isMutirao
            ? 'Preencher aqui informações sobre a empresa e suas vagas.'
            : 'Preencha os dados para exibir no painel de vagas.';
        document.getElementById('table-title').textContent = config.label;
        document.getElementById('col-identificador').textContent = isMutirao ? 'Empresa' : 'Código';
        document.getElementById('col-conteudo').textContent = isMutirao ? 'Informações' : 'Título';
        document.getElementById('col-categoria').textContent = isMutirao ? 'Tela' : 'Categoria';
        document.getElementById('col-validade').textContent = isMutirao ? '-' : 'Validade';
        document.getElementById('col-imprimir').style.display = isMutirao ? '' : 'none';
        document.getElementById('titulo').required = !isMutirao;

        const badge = document.getElementById('table-badge');
        badge.className = `module-badge ${config.badgeClass}`;
        badge.innerHTML = isMutirao
            ? '<i class="bi bi-people-fill"></i> Mutirões'
            : '<i class="bi bi-briefcase-fill"></i> Vagas';

        document.getElementById('btn-tv').href = config.tvUrl;
        termoBusca = '';
        campoBusca.value = '';
        campoBusca.placeholder = isMutirao
            ? 'Buscar mutirão por empresa ou informações'
            : 'Buscar vaga por código ou título';
        statusBusca.textContent = 'Exibindo todos os registros.';
        switcher.style.display = isMutirao ? 'none' : 'flex';
        selecionarPainelVagas(isMutirao ? 'vagas' : painelVagasView);
        sairEdicao();
        carregarVagas();
        if (fechar) fecharSeletor();
    }

    function selecionarPainelVagas(view) {
        painelVagasView = view === 'infos' ? 'infos' : 'vagas';
        const isModuloVagas = moduloAtual === 'VAGAS';
        const mostrandoInfos = isModuloVagas && painelVagasView === 'infos';

        document.getElementById('infos-shell').style.display = mostrandoInfos ? 'block' : 'none';
        document.getElementById('vagas-management').style.display = mostrandoInfos ? 'none' : '';
        document.getElementById('btn-view-vagas').classList.toggle('active', !mostrandoInfos);
        document.getElementById('btn-view-infos').classList.toggle('active', mostrandoInfos);
    }

    function formatarDataBR(valor) {
        if (!valor) return "-";
        if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
            const [y, m, d] = valor.split("-");
            return `${d}/${m}/${y}`;
        }
        const dt = new Date(valor);
        return isNaN(dt) ? valor : dt.toLocaleDateString("pt-BR");
    }

    function normalizarDataInput(valor) {
        if (!valor) return "";
        if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) return valor;
        const dt = new Date(valor);
        return isNaN(dt) ? "" : dt.toISOString().slice(0, 10);
    }

    function endpointModulo() {
        return `${API_VAGAS}/modulo?tipo=${encodeURIComponent(moduloAtual)}`;
    }

    function aplicarNegritoInfo() {
        const editor = document.getElementById('infoMutirao');
        if (document.activeElement !== editor) {
            editor.focus();
        }
        document.execCommand('bold');
    }

    function escaparHtml(valor) {
        return (valor || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function normalizarHtmlInfoMutirao(html) {
        const root = document.createElement('div');
        root.innerHTML = html || '';

        function walk(node) {
            if (node.nodeType === Node.TEXT_NODE) {
                return escaparHtml(node.textContent || '');
            }

            if (node.nodeType !== Node.ELEMENT_NODE) {
                return '';
            }

            const tag = node.tagName.toLowerCase();
            const fontWeight = (node.style && node.style.fontWeight ? node.style.fontWeight : '').toLowerCase();
            const isBold = tag === 'b' || tag === 'strong' || fontWeight === 'bold' || Number(fontWeight) >= 600;
            const children = Array.from(node.childNodes).map(walk).join('');

            if (tag === 'br') {
                return '<br>';
            }

            if (tag === 'div' || tag === 'p') {
                return children ? `${children}<br>` : '<br>';
            }

            return isBold ? `<strong>${children}</strong>` : children;
        }

        return Array.from(root.childNodes)
            .map(walk)
            .join('')
            .replace(/(<br>\s*)+$/i, '');
    }

    function aplicarFontePadraoInfoMutirao() {
        const editor = document.getElementById('infoMutirao');
        editor.innerHTML = normalizarHtmlInfoMutirao(editor.innerHTML);
    }

    function normalizarBusca(valor) {
        return (valor || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .trim()
            .toLowerCase();
    }

    function obterVagasFiltradas() {
        const termoNormalizado = normalizarBusca(termoBusca);
        if (!termoNormalizado) {
            return vagasCache;
        }

        return vagasCache.filter(vaga => {
            const identificador = moduloAtual === 'MUTIROES' ? vaga.empresa : vaga.codigo;
            const conteudo = moduloAtual === 'MUTIROES' ? vaga.atribuicoes : vaga.titulo;
            return [identificador, conteudo].some(valor => normalizarBusca(valor).includes(termoNormalizado));
        });
    }

    function atualizarStatusBusca(totalFiltradas, totalGeral) {
        const status = document.getElementById('busca-status');
        if (!totalGeral) {
            status.textContent = 'Nenhum registro cadastrado neste módulo.';
            return;
        }

        if (!termoBusca.trim()) {
            status.textContent = `Exibindo ${totalGeral} registro(s).`;
            return;
        }

        if (!totalFiltradas) {
            status.textContent = 'Nenhum registro encontrado para esta busca.';
            return;
        }

        status.textContent = `Encontrado(s) ${totalFiltradas} de ${totalGeral} registro(s).`;
    }

    function renderizarTabelaVagas() {
        const tbody = document.getElementById('tabela-vagas');
        const vagasFiltradas = obterVagasFiltradas();
        const colspan = moduloAtual === 'MUTIROES' ? 6 : 5;
        tbody.innerHTML = '';

        atualizarStatusBusca(vagasFiltradas.length, vagasCache.length);

        if (!vagasCache.length) {
            tbody.innerHTML = `<tr><td colspan="${colspan}" class="text-center text-secondary py-4">Nenhum registro cadastrado neste módulo.</td></tr>`;
            return;
        }

        if (!vagasFiltradas.length) {
            tbody.innerHTML = `<tr><td colspan="${colspan}" class="text-center text-secondary py-4">Nenhum registro encontrado para o termo informado.</td></tr>`;
            return;
        }

        vagasFiltradas.forEach(v => {
            const identificador = moduloAtual === 'MUTIROES' ? (v.empresa || '-') : (v.codigo || '-');
            const titulo = moduloAtual === 'MUTIROES' ? (v.atribuicoes || '-') : (v.titulo || '-');
            const categoria = moduloAtual === 'MUTIROES' ? `Tela ${v.tela || '1'}` : (v.categoria || '-');
            const validade = moduloAtual === 'MUTIROES' ? '-' : formatarDataBR(v.validade);
            const imprimirCell = moduloAtual === 'MUTIROES'
                ? `<td><button onclick="imprimirCardMutirao(${v.id})" class="btn btn-outline-dark btn-sm"><i class="bi bi-printer"></i></button></td>`
                : '';
            tbody.innerHTML += `
                <tr>
                    <td>${identificador}</td>
                    <td><strong>${titulo}</strong></td>
                    <td>${categoria}</td>
                    <td>${validade}</td>
                    ${imprimirCell}
                    <td class="text-end">
                        <button onclick="editarVaga(${v.id})" class="btn btn-outline-secondary btn-sm me-1"><i class="bi bi-pencil"></i></button>
                        <button onclick="deletarVaga(${v.id})" class="btn btn-danger btn-sm">Excluir</button>
                    </td>
                </tr>`;
        });
    }

    function carregarVagas() {
        if (!moduloAtual) return;
        fetch(endpointModulo())
            .then(res => res.json())
            .then(vagas => {
                vagasCache = Array.isArray(vagas) ? vagas : [];
                renderizarTabelaVagas();
            });
    }

    document.getElementById('formVaga').addEventListener('submit', function(e) {
        e.preventDefault();

        const dados = {
            modulo: moduloAtual,
            codigo: document.getElementById('codigo').value,
            empresa: document.getElementById('empresa').value,
            tela: document.getElementById('telaMutirao').value,
            titulo: document.getElementById('titulo').value,
            salario: document.getElementById('salario').value,
            escolaridade: document.getElementById('escolaridade').value,
            local: document.getElementById('local').value,
            beneficios: document.getElementById('beneficios').value,
            atribuicoes: document.getElementById('atribuicoes').value,
            validade: document.getElementById('validade').value,
            categoria: document.getElementById('tipo').value,
            experiencia: document.getElementById('experiencia').value,
            expediente: document.getElementById('expediente').value,
        };

        if (moduloAtual === 'MUTIROES' && !dados.empresa.trim()) {
            alert('Informe o nome da empresa para o mutirão.');
            return;
        }

        if (moduloAtual === 'MUTIROES') {
            aplicarFontePadraoInfoMutirao();
            const info = document.getElementById('infoMutirao').innerHTML;
            dados.titulo = '';
            dados.salario = '';
            dados.escolaridade = '';
            dados.beneficios = '';
            dados.atribuicoes = info;
            dados.validade = '';
            dados.categoria = '';
            dados.experiencia = '';
            dados.expediente = '';
        }

        const url = editId ? `${API_VAGAS}/${editId}` : API_VAGAS;
        const method = editId ? 'PUT' : 'POST';

        fetch(url, {
            method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(dados)
        }).then(() => {
            carregarVagas();
            sairEdicao();
        });
    });

    function editarVaga(id) {
        const v = vagasCache.find(item => item.id === id);
        if (!v) return;

        editId = id;
        document.getElementById('codigo').value = v.codigo || '';
        document.getElementById('empresa').value = v.empresa || '';
        document.getElementById('telaMutirao').value = v.tela || '1';
        document.getElementById('titulo').value = v.titulo || '';
        document.getElementById('infoMutirao').innerHTML = normalizarHtmlInfoMutirao(v.atribuicoes || '');
        document.getElementById('salario').value = v.salario || '';
        document.getElementById('escolaridade').value = v.escolaridade || '';
        document.getElementById('local').value = v.local || '';
        document.getElementById('beneficios').value = v.beneficios || '';
        document.getElementById('atribuicoes').value = v.atribuicoes || '';
        document.getElementById('expediente').value = v.expediente || '';
        document.getElementById('validade').value = normalizarDataInput(v.validade);
        document.getElementById('tipo').value = v.categoria || 'CONVENCIONAL';
        document.getElementById('experiencia').value = v.experiencia || 'COM EXPERIÊNCIA';

        document.getElementById('btn-salvar-vaga').textContent = 'Salvar alterações';
        document.getElementById('btn-cancelar-edicao').style.display = 'block';
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function sairEdicao() {
        editId = null;
        document.getElementById('formVaga').reset();
        document.getElementById('infoMutirao').innerHTML = '';
        document.getElementById('btn-salvar-vaga').textContent = 'Cadastrar';
        document.getElementById('btn-cancelar-edicao').style.display = 'none';
    }

    function deletarVaga(id) {
        if (!confirm('Deseja realmente excluir?')) return;
        fetch(`${API_VAGAS}/${id}`, { method: 'DELETE' }).then(() => carregarVagas());
    }

    document.getElementById('btn-cancelar-edicao').addEventListener('click', sairEdicao);
    document.getElementById('busca-vagas').addEventListener('input', function(event) {
        termoBusca = event.target.value || '';
        renderizarTabelaVagas();
    });
    document.getElementById('btn-limpar-busca').addEventListener('click', function() {
        termoBusca = '';
        document.getElementById('busca-vagas').value = '';
        renderizarTabelaVagas();
    });

    const campoExpediente = document.getElementById('expediente');

    campoExpediente.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            // Se já existe 1 quebra de linha (\n), bloqueia um novo Enter
            if (this.value.includes('\n')) {
                event.preventDefault();
            }
        }
    });

    campoExpediente.addEventListener('input', function() {
        // Garante no máximo 1 quebra de linha (2 linhas) e 50 caracteres (mesmo ao colar texto)
        const partes = this.value.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
        let texto = partes.length > 2
            ? partes[0] + '\n' + partes.slice(1).join(' ')
            : partes.join('\n');
        if (texto.length > 50) {
            texto = texto.slice(0, 50);
        }
        if (this.value !== texto) {
            this.value = texto;
        }
    });


    document.getElementById('infoMutirao').addEventListener('paste', function(event) {
        event.preventDefault();
        const clipboard = event.clipboardData || window.clipboardData;
        const html = clipboard.getData('text/html');
        const text = clipboard.getData('text/plain');
        const conteudoNormalizado = html
            ? normalizarHtmlInfoMutirao(html)
            : escaparHtml(text).replace(/\r?\n/g, '<br>');
        document.execCommand('insertHTML', false, conteudoNormalizado);
    });

    document.getElementById('infoMutirao').addEventListener('keydown', function(event) {
        if (event.key !== 'Enter') return;
        event.preventDefault();
        document.execCommand('insertHTML', false, '<br>');
    });

    document.getElementById('infoMutirao').addEventListener('blur', aplicarFontePadraoInfoMutirao);

    function montarCardsImpressao() {
        const grid = document.getElementById('print-grid');
        const heading = document.getElementById('print-heading');
        const kicker = document.getElementById('print-kicker');
        const description = document.getElementById('print-description');
        const config = MODULOS[moduloAtual];

        kicker.textContent = config.label;
        heading.textContent = moduloAtual === 'MUTIROES' ? 'Cards de Mutirões para Mural Físico' : 'Cards de Vagas para Mural Físico';
        description.textContent = 'A impressão preserva a estrutura visual do painel digital, pronta para exposição no mural da agência.';
        grid.innerHTML = '';

        if (!vagasCache.length) {
            grid.innerHTML = '<div class="text-secondary">Nenhum card disponível para impressão.</div>';
            return;
        }

        vagasCache.forEach(vaga => {
            const card = document.createElement('article');
            card.className = `print-vaga-card ${moduloAtual === 'MUTIROES' ? 'mutiroes' : ''}`;
            const identificador = moduloAtual === 'MUTIROES' ? (vaga.empresa || '-') : (vaga.codigo || '-');
            if (moduloAtual === 'MUTIROES') {
                card.innerHTML = `
                    <div class="print-card-top">
                        <span class="print-code">${identificador}</span>
                        <span class="small text-uppercase text-secondary">${vaga.local || ''}</span>
                    </div>
                    <div class="print-detail">${vaga.atribuicoes || '-'}</div>
                `;
            } else {
                card.innerHTML = `
                    <div class="print-card-top">
                        <span class="print-code">${identificador}</span>
                        <span class="small text-uppercase text-secondary">${vaga.local || ''}</span>
                    </div>
                    <h2 class="print-title">${vaga.titulo || '-'}</h2>
                    <div class="print-salary">R$ ${vaga.salario || '-'}</div>
                    <div class="print-meta"><strong>Categoria:</strong> ${vaga.categoria || '-'}</div>
                    <div class="print-meta"><strong>Escolaridade:</strong> ${vaga.escolaridade || '-'}</div>
                    <div class="print-meta"><strong>Experiência:</strong> ${vaga.experiencia || '-'}</div>
                    <div class="print-detail"><strong>Benefícios:</strong> ${vaga.beneficios || '-'}</div>
                    <div class="print-detail"><strong>Atribuições:</strong> ${vaga.atribuicoes || '-'}</div>
                    <div class="print-meta mt-auto"><strong>Validade:</strong> ${formatarDataBR(vaga.validade)}</div>
                `;
            }
            grid.appendChild(card);
        });
    }

    function imprimirCards() {
        document.body.classList.remove('print-mutirao-single');
        montarCardsImpressao();
        window.print();
    }

    function ajustarTextoImpressaoMutirao(card) {
        const detail = card.querySelector('.print-detail');
        const wrap = card.querySelector('.print-detail-wrap');
        if (!detail || !wrap) return;

        const measureStyleId = 'mutirao-print-measure-style';
        let measureStyle = document.getElementById(measureStyleId);
        if (!measureStyle) {
            measureStyle = document.createElement('style');
            measureStyle.id = measureStyleId;
            document.head.appendChild(measureStyle);
        }

        measureStyle.textContent = `
            body.print-mutirao-single #print-area {
                display: block !important;
                width: 285mm !important;
                height: 198mm !important;
                padding: 0 !important;
                margin: 0 !important;
                overflow: hidden !important;
            }
            body.print-mutirao-single #print-area,
            body.print-mutirao-single #print-area * {
                font-family: Arial, sans-serif !important;
                font-size: 25pt !important;
            }
            body.print-mutirao-single #print-kicker,
            body.print-mutirao-single #print-heading,
            body.print-mutirao-single #print-description {
                display: none !important;
            }
            body.print-mutirao-single .print-preview-grid {
                display: grid !important;
                grid-template-columns: 1fr !important;
                gap: 0 !important;
            }
            body.print-mutirao-single .print-vaga-card {
                width: 100% !important;
                height: 198mm !important;
                min-height: 198mm !important;
                padding: 10mm 10mm 10mm 12mm !important;
                box-sizing: border-box !important;
                display: grid !important;
                grid-template-rows: auto 1fr !important;
                overflow: hidden !important;
            }
            body.print-mutirao-single .print-card-top {
                display: flex !important;
                justify-content: space-between !important;
                gap: 10px !important;
                align-items: flex-start !important;
                margin-bottom: 8mm !important;
            }
            body.print-mutirao-single #print-area .print-code {
                font-size: 40pt !important;
                font-weight: 900 !important;
                padding: 0 !important;
                background: transparent !important;
                color: #000 !important;
            }
            body.print-mutirao-single .print-location {
                font-size: 24pt !important;
                font-weight: 900 !important;
                color: #000 !important;
                letter-spacing: 0.03em !important;
            }
            body.print-mutirao-single .print-detail-wrap {
                height: 100% !important;
                min-height: 0 !important;
                display: flex !important;
                align-items: flex-start !important;
                overflow: hidden !important;
            }
            body.print-mutirao-single #print-area .print-detail,
            body.print-mutirao-single #print-area .print-detail * {
                font-size: var(--mutirao-print-detail-size, 28pt) !important;
                line-height: 1.32 !important;
                white-space: pre-wrap !important;
                margin: 0 !important;
            }
        `;

        const maxSize = 28;
        const minSize = 12;
        const safetyGap = 24;
        let selectedSize = maxSize;
        document.documentElement.style.setProperty('--mutirao-print-detail-size', `${maxSize}pt`);

        for (let size = maxSize; size >= minSize; size -= 1) {
            document.documentElement.style.setProperty('--mutirao-print-detail-size', `${size}pt`);
            detail.offsetHeight;
            const detailRect = detail.getBoundingClientRect();
            const wrapRect = wrap.getBoundingClientRect();
            const cardRect = card.getBoundingClientRect();
            const detailFits = detail.scrollHeight <= Math.max(0, wrap.clientHeight - safetyGap);
            const rectFits = detailRect.bottom <= Math.min(wrapRect.bottom, cardRect.bottom) - safetyGap;
            const widthFits = detail.scrollWidth <= wrap.clientWidth;
            if (detailFits && rectFits && widthFits) {
                selectedSize = size;
                break;
            }
            selectedSize = size;
        }

        document.documentElement.style.setProperty('--mutirao-print-detail-size', `${selectedSize}pt`);
    }

    function imprimirCardMutirao(id) {
        const vaga = vagasCache.find(item => item.id === id);
        if (!vaga || moduloAtual !== 'MUTIROES') return;

        const grid = document.getElementById('print-grid');
        const heading = document.getElementById('print-heading');
        const kicker = document.getElementById('print-kicker');
        const description = document.getElementById('print-description');

        document.body.classList.add('print-mutirao-single');
        kicker.textContent = `Painel de Mutirões - Tela ${vaga.tela || '1'}`;
        heading.textContent = vaga.empresa || 'Mutirão';
        description.textContent = vaga.local || '';
        grid.innerHTML = '';

        const card = document.createElement('article');
        card.className = 'print-vaga-card mutiroes';
        card.innerHTML = `
            <div class="print-card-top">
                <span class="print-code">${vaga.empresa || '-'}</span>
                <span class="small text-uppercase text-secondary print-location">${vaga.local || ''}</span>
            </div>
            <div class="print-detail-wrap">
                <div class="print-detail">${vaga.atribuicoes || '-'}</div>
            </div>
        `;
        grid.appendChild(card);
        ajustarTextoImpressaoMutirao(card);

        const styleId = 'mutirao-print-page-style';
        let style = document.getElementById(styleId);
        if (!style) {
            style = document.createElement('style');
            style.id = styleId;
            document.head.appendChild(style);
        }
        style.textContent = '@media print { @page { size: A4 landscape; margin: 6mm; } }';

        const limpar = () => {
            document.body.classList.remove('print-mutirao-single');
            document.documentElement.style.removeProperty('--mutirao-print-detail-size');
            style.textContent = '';
            const measureStyle = document.getElementById('mutirao-print-measure-style');
            if (measureStyle) measureStyle.textContent = '';
            window.removeEventListener('afterprint', limpar);
        };
        window.addEventListener('afterprint', limpar);
        window.print();
    }

    function criarInput(placeholder, className, value) {
        const input = document.createElement('input');
        input.type = 'text';
        input.className = `form-control ${className}`;
        input.placeholder = placeholder;
        input.value = value || '';
        return input;
    }

    function criarTextarea(placeholder, className, value, rows = 3) {
        const textarea = document.createElement('textarea');
        textarea.className = `form-control ${className}`;
        textarea.placeholder = placeholder;
        textarea.rows = rows;
        textarea.value = value || '';
        return textarea;
    }

    function marcarInfosAlteradas() {
        if (!populandoInfos) {
            infosDirty = true;
        }
    }

    function criarBotaoRemover() {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-outline-danger btn-sm';
        btn.innerHTML = '<i class="bi bi-x-lg"></i>';
        btn.addEventListener('click', () => {
            btn.parentElement.remove();
            marcarInfosAlteradas();
        });
        return btn;
    }

    function addCurso(nome = '', detalhe = '', marcarAlteracao = true) {
        const wrap = document.createElement('div');
        wrap.className = 'curso-item';

        const nomeInput = criarInput('Nome do curso', 'curso-nome', nome);
        const detalheArea = criarTextarea('Informações do curso (uma linha por item)', 'curso-detalhe', detalhe, 4);
        const btnRemover = criarBotaoRemover();
        btnRemover.className = 'btn btn-outline-danger btn-sm w-100';

        wrap.append(nomeInput, detalheArea, btnRemover);
        document.getElementById('cursos-list').appendChild(wrap);
        if (marcarAlteracao) marcarInfosAlteradas();
    }

    function addProcesso(empresa = '', data = '', texto = '', marcarAlteracao = true) {
        const wrap = document.createElement('div');
        wrap.className = 'processo-item';

        const empresaInput = criarInput('Empresa', 'processo-empresa', empresa);
        const dataInput = criarInput('Data e horário', 'processo-data', data);
        const textoArea = document.createElement('textarea');
        textoArea.className = 'form-control processo-texto';
        textoArea.rows = 4;
        textoArea.placeholder = 'Texto do processo (uma linha por item)';
        textoArea.value = texto || '';

        const btnRemover = criarBotaoRemover();
        btnRemover.className = 'btn btn-outline-danger btn-sm w-100';

        wrap.append(empresaInput, dataInput, textoArea, btnRemover);
        document.getElementById('processos-list').appendChild(wrap);
        if (marcarAlteracao) marcarInfosAlteradas();
    }

    function addCronograma(data = '', evento = '', marcarAlteracao = true) {
        const row = document.createElement('div');
        row.className = 'info-row';
        row.append(
            criarInput('Data', 'cronograma-data', data),
            criarInput('Evento', 'cronograma-evento', evento),
            criarBotaoRemover()
        );
        document.getElementById('cronograma-list').appendChild(row);
        if (marcarAlteracao) marcarInfosAlteradas();
    }

    function carregarInfos() {
        fetch(API_INFOS, { cache: 'no-store' })
            .then(res => res.json())
            .then(d => {
                const dadosRecebidos = d || {};
                infosCache = dadosRecebidos;
                if (infosDirty) {
                    return;
                }
                const cursos = Array.isArray(dadosRecebidos.cursos) ? dadosRecebidos.cursos : [];
                const processos = Array.isArray(dadosRecebidos.processos) ? dadosRecebidos.processos : [];
                const cronograma = Array.isArray(dadosRecebidos.cronograma) ? dadosRecebidos.cronograma : [];
                populandoInfos = true;
                try {
                    document.getElementById('mutiraoTituloTela1').value = dadosRecebidos.mutiraoTituloTela1 || '';
                    document.getElementById('mutiraoTituloTela2').value = dadosRecebidos.mutiraoTituloTela2 || '';
                    document.getElementById('mutiraoTituloTela3').value = dadosRecebidos.mutiraoTituloTela3 || '';
                    document.getElementById('mutiraoTemaCor').value = dadosRecebidos.mutiraoTemaCor || '#1f4f8a';
                    document.getElementById('mutiraoRodapeCor').value = dadosRecebidos.mutiraoRodapeCor || '#6889ec';

                    document.getElementById('cursos-list').innerHTML = '';
                    document.getElementById('processos-list').innerHTML = '';
                    document.getElementById('cronograma-list').innerHTML = '';

                    (cursos.length ? cursos : [{}]).forEach(c => addCurso(c.nome || '', c.detalhe || '', false));
                    (processos.length ? processos : [{}]).forEach(p => addProcesso(p.empresa || '', p.data || '', (p.texto || []).join('\n'), false));
                    (cronograma.length ? cronograma : [{}]).forEach(c => addCronograma(c.data || '', c.evento || '', false));
                } finally {
                    populandoInfos = false;
                }
            });
    }

    function salvarInfos() {
        const dados = {
            cursos: [],
            processos: [],
            cronograma: [],
            mutiraoTituloTela1: document.getElementById('mutiraoTituloTela1').value.trim(),
            mutiraoTituloTela2: document.getElementById('mutiraoTituloTela2').value.trim(),
            mutiraoTituloTela3: document.getElementById('mutiraoTituloTela3').value.trim(),
            mutiraoTemaCor: document.getElementById('mutiraoTemaCor').value || '#1f4f8a',
            mutiraoRodapeCor: document.getElementById('mutiraoRodapeCor').value || '#6889ec'
        };

        document.querySelectorAll('#cursos-list .curso-item').forEach(row => {
            const nome = row.querySelector('.curso-nome').value.trim();
            const detalhe = row.querySelector('.curso-detalhe').value.trim();
            if (nome || detalhe) dados.cursos.push({ nome, detalhe });
        });

        document.querySelectorAll('#processos-list .processo-item').forEach(item => {
            const empresa = item.querySelector('.processo-empresa').value.trim();
            const data = item.querySelector('.processo-data').value.trim();
            const texto = item.querySelector('.processo-texto').value.split('\n').map(l => l.trim()).filter(Boolean);
            if (empresa || data || texto.length) dados.processos.push({ empresa, data, texto });
        });

        document.querySelectorAll('#cronograma-list .info-row').forEach(row => {
            const data = row.querySelector('.cronograma-data').value.trim();
            const evento = row.querySelector('.cronograma-evento').value.trim();
            if (data || evento) dados.cronograma.push({ data, evento });
        });

        if (dados.processos.length) {
            dados.empresaNome = dados.processos[0].empresa || '';
            dados.empresaData = dados.processos[0].data || '';
            dados.processoVagas = dados.processos[0].texto || [];
        } else {
            dados.empresaNome = '';
            dados.empresaData = '';
            dados.processoVagas = [];
        }

        fetch(API_INFOS, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(dados)
        }).then(res => {
            if (!res.ok) throw new Error('Falha ao salvar informacoes');
            infosCache = dados;
            infosDirty = false;
            alert('Informações da TV atualizadas.');
        }).catch(() => {
            alert('Não foi possível salvar as informações. Tente novamente.');
        });
    }

    function salvarTitulosMutiroes() {
        const base = infosCache || {
            cursos: [],
            processos: [],
            cronograma: [],
            empresaNome: '',
            empresaData: '',
            processoVagas: []
        };

        const dados = {
            ...base,
            cursos: Array.isArray(base.cursos) ? base.cursos : [],
            processos: Array.isArray(base.processos) ? base.processos : [],
            cronograma: Array.isArray(base.cronograma) ? base.cronograma : [],
            processoVagas: Array.isArray(base.processoVagas) ? base.processoVagas : [],
            mutiraoTituloTela1: document.getElementById('mutiraoTituloTela1').value.trim(),
            mutiraoTituloTela2: document.getElementById('mutiraoTituloTela2').value.trim(),
            mutiraoTituloTela3: document.getElementById('mutiraoTituloTela3').value.trim(),
            mutiraoTemaCor: document.getElementById('mutiraoTemaCor').value || '#1f4f8a',
            mutiraoRodapeCor: document.getElementById('mutiraoRodapeCor').value || '#6889ec'
        };

        fetch(API_INFOS, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(dados)
        }).then(res => {
            if (!res.ok) throw new Error('Falha ao salvar titulos');
            infosCache = dados;
            alert('Títulos dos mutirões atualizados.');
        }).catch(() => {
            alert('Não foi possível salvar os títulos dos mutirões. Tente novamente.');
        });
    }

    document.getElementById('formInfos').addEventListener('input', marcarInfosAlteradas);

    carregarInfos();
    selecionarModulo('VAGAS', false);
