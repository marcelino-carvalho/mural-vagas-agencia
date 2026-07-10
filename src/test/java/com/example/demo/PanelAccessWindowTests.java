package com.example.demo;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelAccessWindowTests {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Test
    void abreNaSegundaAsSeteEDez() {
        PanelAccessWindow window = windowAt("2026-07-06T10:10:00Z");

        assertTrue(window.isOpenNow());
    }

    @Test
    void fechaNaSegundaAsDezesseteEDez() {
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
