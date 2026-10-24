package br.gov.curitiba.mural;

import br.gov.curitiba.mural.service.PanelAccessWindow;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários para validar as regras da janela de horário de funcionamento do painel.
 */
class PanelAccessWindowTests {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Test
    void abreNaSegundaAsSeteEDez() {
        // 2026-07-06 07:10:00 UTC-3 corresponde a 10:10:00 UTC
        PanelAccessWindow window = windowAt("2026-07-06T10:10:00Z");

        assertTrue(window.isOpenNow());
    }

    @Test
    void fechaNaSegundaAsDezesseteEDez() {
        // 2026-07-06 17:10:00 UTC-3 corresponde a 20:10:00 UTC
        PanelAccessWindow window = windowAt("2026-07-06T20:10:00Z");

        assertFalse(window.isOpenNow());
    }

    @Test
    void fechaNoSabado() {
        PanelAccessWindow window = windowAt("2026-07-11T13:00:00Z");

        assertFalse(window.isOpenNow());
    }

    private PanelAccessWindow windowAt(String instant) {
        return new PanelAccessWindow(
                Clock.fixed(Instant.parse(instant), ZONE),
                ZONE,
                LocalTime.of(7, 10),
                LocalTime.of(17, 10)
        );
    }
}
