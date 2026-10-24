package br.gov.curitiba.mural.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Serviço responsável por validar e gerenciar a janela de horário de funcionamento
 * do mural de vagas público. Por padrão, restringe acessos a dias úteis em horário comercial,
 * permitindo forçar a abertura para testes locais em desenvolvimento.
 */
@Service
public class PanelAccessWindow {

    private final Clock clock;
    private final ZoneId zoneId;
    private final LocalTime start;
    private final LocalTime end;
    private final boolean forceOpen;

    @Autowired
    public PanelAccessWindow(
            @Value("${panel.access.zone:America/Sao_Paulo}") String zone,
            @Value("${panel.access.start:07:10}") String start,
            @Value("${panel.access.end:17:10}") String end,
            @Value("${panel.access.force-open:false}") boolean forceOpen
    ) {
        this(Clock.system(ZoneId.of(zone)), ZoneId.of(zone), LocalTime.parse(start), LocalTime.parse(end), forceOpen);
    }

    /**
     * Construtor público para injeção manual de Clock sem forçar abertura (padrão de testes).
     */
    public PanelAccessWindow(Clock clock, ZoneId zoneId, LocalTime start, LocalTime end) {
        this(clock, zoneId, start, end, false);
    }

    /**
     * Construtor completo permitindo configurar o modo forçado (forceOpen) para testes determinísticos.
     */
    public PanelAccessWindow(Clock clock, ZoneId zoneId, LocalTime start, LocalTime end, boolean forceOpen) {
        this.clock = clock;
        this.zoneId = zoneId;
        this.start = start;
        this.end = end;
        this.forceOpen = forceOpen;
    }

    /**
     * Verifica se o sistema está dentro da janela de operação permitida no momento atual.
     * Se forceOpen estiver ativo (modo de teste/desenvolvimento), sempre retorna true.
     * Retorna falso aos finais de semana ou fora do intervalo configurado.
     *
     * @return true se o painel estiver aberto, false caso contrário
     */
    public boolean isOpenNow() {
        if (forceOpen) {
            return true;
        }

        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(start) && time.isBefore(end);
    }

    /**
     * Exige que o painel esteja aberto. Lança HTTP 503 (Service Unavailable) se estiver fora do horário.
     */
    public void requireOpen() {
        if (!isOpenNow()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Painel fora do horário de consulta. As buscas ficam ativas de segunda a sexta, das 07:10 às 17:10."
            );
        }
    }
}
