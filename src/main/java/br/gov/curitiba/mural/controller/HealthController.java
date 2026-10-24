package br.gov.curitiba.mural.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de verificação de disponibilidade (Health Check).
 * Utilizado por balanceadores de carga, scripts de monitoramento e testes de conectividade da TV.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * Responde com o status operacional da aplicação.
     *
     * @return "ok" quando a aplicação está em execução
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String health() {
        return "ok";
    }
}
