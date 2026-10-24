package br.gov.curitiba.mural.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptador global de exceções para a API REST (/api/*).
 * Garante que respostas de erro da API sejam padronizadas em formato JSON (application/json)
 * e evita interferir em requisições de arquivos estáticos canceladas pelo navegador.
 */
@RestControllerAdvice(basePackages = "br.gov.curitiba.mural.controller")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata exceções do tipo {@link ResponseStatusException} geradas pela API (ex: 404, 503).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getReason());

        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /**
     * Ignora exceções de desconexão do cliente (ex: usuário deu F5 ou fechou a aba durante o carregamento).
     */
    @ExceptionHandler(IOException.class)
    public void handleClientAbort(IOException ex) {
        // Conexão encerrada pelo cliente/navegador; nenhum envio de resposta é necessário ou possível.
        log.debug("Conexão cancelada pelo cliente: {}", ex.getMessage());
    }

    /**
     * Trata quaisquer outras exceções não mapeadas nas rotas da API REST.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Se a causa raiz for desconexão de rede do cliente, ignora silenciosamente
        if (ex.getCause() instanceof IOException || ex.getClass().getSimpleName().contains("ClientAbort")) {
            log.debug("Requisição abortada pelo cliente: {}", ex.getMessage());
            return null;
        }

        log.error("Erro interno não tratado na API: {}", ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Ocorreu um erro interno no servidor ao processar a solicitação.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
