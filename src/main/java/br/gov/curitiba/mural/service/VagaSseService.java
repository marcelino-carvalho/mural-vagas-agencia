package br.gov.curitiba.mural.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serviço responsável por gerenciar conexões Server-Sent Events (SSE).
 * Permite que telas de TV e painéis conectados recebam notificações em tempo real
 * quando uma vaga ou informação institucional for criada, atualizada ou excluída.
 */
@Service
public class VagaSseService {

    /** Lista concorrente e segura contra race conditions para os clientes conectados */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Registra um novo cliente interessado em atualizações de dados.
     * Define callbacks para limpeza automática de conexões encerradas ou com timeout.
     *
     * @return emissor SSE para o cliente
     */
    public SseEmitter subscribe() {
        // Timeout de 0 indica conexão contínua até o cliente fechar
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    /**
     * Notifica todos os clientes conectados (painéis e TVs) de que houve alteração nos dados.
     * Dispara o evento SSE "vagas" com o payload "update".
     */
    public void notifyChange() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("vagas").data("update"));
            } catch (IOException e) {
                // Em caso de falha de escrita (cliente desconectou), remove o emissor
                emitters.remove(emitter);
            }
        }
    }
}
