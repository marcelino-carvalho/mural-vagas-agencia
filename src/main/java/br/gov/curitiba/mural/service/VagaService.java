package br.gov.curitiba.mural.service;

import br.gov.curitiba.mural.domain.Vaga;
import br.gov.curitiba.mural.repository.VagaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * Serviço responsável pelas regras de negócio e operações do ciclo de vida das vagas de emprego.
 */
@Service
public class VagaService {

    private final VagaRepository repository;
    private final VagaSseService sseService;

    public VagaService(VagaRepository repository, VagaSseService sseService) {
        this.repository = repository;
        this.sseService = sseService;
    }

    /**
     * Cadastra uma nova vaga, aplicando normalizações e notificando os clientes SSE.
     *
     * @param vaga Dados da vaga a ser persistida
     * @return Vaga persistida com o ID gerado
     */
    @Transactional
    public Vaga cadastrar(Vaga vaga) {
        normalizarVaga(vaga);
        Vaga salva = repository.save(vaga);
        sseService.notifyChange();
        return salva;
    }

    /**
     * Retorna todas as vagas pertencentes ao módulo padrão de vagas.
     */
    @Transactional(readOnly = true)
    public List<Vaga> listarTodas() {
        return repository.findByModuloOrderByValidadeAsc(Vaga.MODULO_VAGAS);
    }

    /**
     * Retorna as vagas pertencentes a um módulo específico (VAGAS ou MUTIROES).
     * Caso seja o módulo de mutirões, aplica a ordenação por número de tela e empresa.
     *
     * @param tipo Nome do módulo
     * @return Lista de vagas do módulo ordenada
     */
    @Transactional(readOnly = true)
    public List<Vaga> listarPorModulo(String tipo) {
        String modulo = normalizarModulo(tipo);
        List<Vaga> vagas = repository.findByModuloOrderByValidadeAsc(modulo);
        return ordenarPorModulo(vagas, modulo);
    }

    /**
     * Retorna as vagas filtradas por categoria (ex: CONVENCIONAL, SUPERIOR, PCD).
     *
     * @param categoria Categoria informada
     * @return Lista de vagas da categoria ordenadas por validade
     */
    @Transactional(readOnly = true)
    public List<Vaga> listarPorCategoria(String categoria) {
        return repository.findByCategoriaOrderByValidadeAsc(categoria);
    }

    /**
     * Atualiza as informações de uma vaga existente.
     *
     * @param id Identificador da vaga
     * @param payload Novos dados da vaga
     * @return Vaga atualizada
     */
    @Transactional
    public Vaga atualizar(Long id, Vaga payload) {
        Vaga vaga = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada com o ID: " + id));

        vaga.setCodigo(payload.getCodigo());
        vaga.setEmpresa(payload.getEmpresa());
        vaga.setTitulo(payload.getTitulo());
        vaga.setSalario(payload.getSalario());
        vaga.setEscolaridade(payload.getEscolaridade());
        vaga.setLocal(payload.getLocal());
        vaga.setTela(normalizarTela(payload.getTela()));
        vaga.setBeneficios(payload.getBeneficios());
        vaga.setAtribuicoes(payload.getAtribuicoes());
        vaga.setValidade(payload.getValidade());
        vaga.setCategoria(payload.getCategoria() == null ? null : payload.getCategoria().toUpperCase());
        vaga.setModulo(normalizarModulo(payload.getModulo()));
        vaga.setExperiencia(payload.getExperiencia());
        vaga.setDescricao(payload.getDescricao());

        Vaga salva = repository.save(vaga);
        sseService.notifyChange();
        return salva;
    }

    /**
     * Remove uma vaga pelo seu identificador e notifica as TVs conectadas via SSE.
     *
     * @param id Identificador da vaga a ser excluída
     */
    @Transactional
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada com o ID: " + id);
        }
        repository.deleteById(id);
        sseService.notifyChange();
    }

    /**
     * Normaliza os valores de categoria, módulo e número da tela da vaga.
     */
    public void normalizarVaga(Vaga vaga) {
        if (vaga == null) return;
        vaga.setCategoria(vaga.getCategoria() == null ? null : vaga.getCategoria().toUpperCase());
        vaga.setModulo(normalizarModulo(vaga.getModulo()));
        vaga.setTela(normalizarTela(vaga.getTela()));
    }

    /**
     * Normaliza o nome do módulo informado para garantir maiúsculas e valor padrão seguro.
     */
    public String normalizarModulo(String modulo) {
        if (modulo == null || modulo.isBlank()) {
            return Vaga.MODULO_VAGAS;
        }
        String upper = modulo.trim().toUpperCase();
        if (Vaga.MODULO_MUTIROES.equals(upper)) {
            return Vaga.MODULO_MUTIROES;
        }
        return Vaga.MODULO_VAGAS;
    }

    /**
     * Ordena a lista de vagas. Quando se trata de mutirões, ordena pelo número da tela e depois pelo nome da empresa.
     */
    public List<Vaga> ordenarPorModulo(List<Vaga> vagas, String modulo) {
        if (!Vaga.MODULO_MUTIROES.equals(modulo)) {
            return vagas;
        }
        return vagas.stream()
                .sorted(Comparator
                        .comparing((Vaga v) -> normalizarTela(v.getTela()))
                        .thenComparing(v -> normalizarTextoOrdenacao(v.getEmpresa())))
                .toList();
    }

    private static String normalizarTextoOrdenacao(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    private static String normalizarTela(String valor) {
        if (valor == null) {
            return "1";
        }
        String tela = valor.trim();
        return switch (tela) {
            case "1", "2", "3" -> tela;
            default -> "1";
        };
    }
}
