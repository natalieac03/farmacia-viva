package br.org.cremic.farmaciaviva.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HealthServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-04T12:00:00Z");
    private static final String APPLICATION_NAME = "farmacia-viva-backend";

    private HealthService healthService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        healthService = new HealthService(APPLICATION_NAME, fixedClock);
    }

    @Test
    @DisplayName("check deve retornar status UP")
    void checkShouldReturnStatusUp() {
        HealthResponse response = healthService.check();

        assertThat(response.status()).isEqualTo("UP");
    }

    @Test
    @DisplayName("check deve retornar o nome da aplicacao")
    void checkShouldReturnApplicationName() {
        HealthResponse response = healthService.check();

        assertThat(response.application()).isEqualTo(APPLICATION_NAME);
    }

    @Test
    @DisplayName("check deve retornar o timestamp do clock configurado")
    void checkShouldReturnTimestampFromClock() {
        HealthResponse response = healthService.check();

        OffsetDateTime expected = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        assertThat(response.timestamp()).isEqualTo(expected);
    }
}
