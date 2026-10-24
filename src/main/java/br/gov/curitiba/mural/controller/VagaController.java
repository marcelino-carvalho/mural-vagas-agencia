package br.gov.curitiba.mural.controller;

import br.gov.curitiba.mural.domain.Vaga;
import br.gov.curitiba.mural.service.PanelAccessWindow;
import br.gov.curitiba.mural.service.PdfReportService;
import br.gov.curitiba.mural.service.VagaService;
import br.gov.curitiba.mural.service.VagaSseService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Controlador REST para o gerenciamento de vagas de emprego.
 * Expõe endpoints para cadastro, consulta, atualização, exclusão,
 * transmissão de eventos em tempo real (SSE) e exportação de relatórios em PDF.
 */
@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    private final VagaService vagaService;
    private final VagaSseService sseService;
    private final PanelAccessWindow accessWindow;
    private final PdfReportService pdfReportService;

    public VagaController(
            VagaService vagaService,
            VagaSseService sseService,
            PanelAccessWindow accessWindow,
            PdfReportService pdfReportService
    ) {
        this.vagaService = vagaService;
        this.sseService = sseService;
        this.accessWindow = accessWindow;
        this.pdfReportService = pdfReportService;
    }

    /**
     * Cadastra uma nova vaga de emprego no sistema.
     *
     * @param vaga Dados da vaga a ser cadastrada
     * @return Vaga cadastrada com ID atribuído
     */
    @PostMapping
    public Vaga cadastrar(@RequestBody Vaga vaga) {
        return vagaService.cadastrar(vaga);
    }

    /**
     * Lista todas as vagas disponíveis no módulo padrão VAGAS.
     * Sujeito ao horário de funcionamento da agência.
     */
    @GetMapping
    public List<Vaga> listarTodas() {
        accessWindow.requireOpen();
        return vagaService.listarTodas();
    }

    /**
     * Lista vagas filtradas por módulo (ex: VAGAS ou MUTIROES).
     *
     * @param tipo Nome do módulo
     */
    @GetMapping("/modulo")
    public List<Vaga> listarPorModulo(@RequestParam String tipo) {
        accessWindow.requireOpen();
        return vagaService.listarPorModulo(tipo);
    }

    /**
     * Lista vagas filtradas por categoria (ex: CONVENCIONAL, SUPERIOR, PCD).
     *
     * @param tipo Nome da categoria
     */
    @GetMapping("/filtro")
    public List<Vaga> listarPorCategoria(@RequestParam String tipo) {
        accessWindow.requireOpen();
        return vagaService.listarPorCategoria(tipo);
    }

    /**
     * Remove uma vaga existente através do seu identificador.
     *
     * @param id ID da vaga
     */
    @DeleteMapping("/{id}")
    public void deletarVaga(@PathVariable Long id) {
        vagaService.deletar(id);
    }

    /**
     * Atualiza as informações de uma vaga existente.
     *
     * @param id ID da vaga a ser alterada
     * @param payload Novos dados da vaga
     * @return Vaga com os dados atualizados
     */
    @PutMapping("/{id}")
    public Vaga atualizar(@PathVariable Long id, @RequestBody Vaga payload) {
        return vagaService.atualizar(id, payload);
    }

    /**
     * Abre uma conexão de stream Server-Sent Events (SSE) para receber atualizações instantâneas.
     */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return sseService.subscribe();
    }

    /**
     * Gera e realiza o download do relatório completo de vagas ativas em formato PDF.
     */
    @GetMapping("/relatorio")
    public ResponseEntity<byte[]> gerarRelatorio() {
        accessWindow.requireOpen();
        List<Vaga> todasVagas = vagaService.listarTodas();
        byte[] pdfBytes = pdfReportService.generatePdfReport(todasVagas);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio_vagas.pdf")
                .body(pdfBytes);
    }
}
