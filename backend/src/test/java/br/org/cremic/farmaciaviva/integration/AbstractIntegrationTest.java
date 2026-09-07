package br.org.cremic.farmaciaviva.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Padrao singleton de container: o PostgreSQL e iniciado uma unica vez por JVM
    // e compartilhado por todas as classes de teste de integracao. Isso evita que o
    // container seja parado entre classes enquanto o contexto Spring em cache ainda
    // aponta para a porta antiga.
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("farmacia_viva_test")
            .withUsername("farmacia_test")
            .withPassword("farmacia_test");

    static {
        POSTGRES.start();
    }
}
