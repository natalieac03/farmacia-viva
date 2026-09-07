package br.org.cremic.farmaciaviva.health;

import java.time.OffsetDateTime;

public record HealthResponse(
    String status,
    String application,
    OffsetDateTime timestamp
) {
}
