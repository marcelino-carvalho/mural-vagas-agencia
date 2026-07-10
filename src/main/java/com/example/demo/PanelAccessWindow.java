package com.example.demo;

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

@Service
public class PanelAccessWindow {

    private final Clock clock;
    private final ZoneId zoneId;
    private final LocalTime start;
    private final LocalTime end;

    @Autowired
    public PanelAccessWindow(
            @Value("${panel.access.zone:America/Sao_Paulo}") String zone,
            @Value("${panel.access.start:07:10}") String start,
            @Value("${panel.access.end:17:10}") String end
    ) {
        this(Clock.system(ZoneId.of(zone)), ZoneId.of(zone), LocalTime.parse(start), LocalTime.parse(end));
    }

    PanelAccessWindow(Clock clock, ZoneId zoneId, LocalTime start, LocalTime end) {
        this.clock = clock;
        this.zoneId = zoneId;
        this.start = start;
        this.end = end;
    }

    public boolean isOpenNow() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(start) && time.isBefore(end);
    }

    public void requireOpen() {
        if (!isOpenNow()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Painel fora do horario de consulta. As buscas ficam ativas de segunda a sexta, das 07:10 as 17:10."
            );
        }
    }
}
