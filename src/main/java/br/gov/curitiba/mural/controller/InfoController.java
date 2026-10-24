package br.gov.curitiba.mural.controller;

import br.gov.curitiba.mural.dto.InfoDTO;
import br.gov.curitiba.mural.service.InfoService;
import br.gov.curitiba.mural.service.PanelAccessWindow;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para manutenção das informações institucionais, cursos,
 * agendamentos e configurações visuais dos murais.
 */
@RestController
@RequestMapping("/api/infos")
public class InfoController {

    private final InfoService infoService;
    private final PanelAccessWindow accessWindow;

    public InfoController(InfoService infoService, PanelAccessWindow accessWindow) {
        this.infoService = infoService;
        this.accessWindow = accessWindow;
    }

    /**
     * Recupera as informações institucionais e cursos ativos para exibição.
     * Sujeito ao horário de funcionamento da agência.
     */
    @GetMapping
    public InfoDTO getInfos() {
        accessWindow.requireOpen();
        return infoService.getInfos();
    }

    /**
     * Atualiza as informações institucionais e cursos a partir do painel administrativo.
     *
     * @param novosDados Dados estruturados dos comunicados e cursos
     */
    @PostMapping
    public void atualizarInfos(@RequestBody InfoDTO novosDados) {
        infoService.atualizarInfos(novosDados);
    }
}
