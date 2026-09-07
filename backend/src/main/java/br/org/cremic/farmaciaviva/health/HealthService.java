package br.org.cremic.farmaciaviva.health;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private static final String STATUS_UP = "UP";

    private final String applicationName;
    private final Clock clock;

    public HealthService(
        @Value("${spring.application.name}") String applicationName,
        Clock clock
    ) {
        this.applicationName = applicationName;
        this.clock = clock;
    }

    public HealthResponse check() {
        return new HealthResponse(STATUS_UP, applicationName, OffsetDateTime.now(clock));
    }
}
