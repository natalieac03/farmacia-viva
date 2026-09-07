package br.org.cremic.farmaciaviva.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.org.cremic.farmaciaviva.health.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("GET /api/v1/health deve retornar 200 com status UP")
    void healthEndpointShouldReturnUp() {
        ResponseEntity<HealthResponse> response =
            restTemplate.getForEntity("/api/v1/health", HealthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("UP");
        assertThat(response.getBody().application()).isEqualTo("farmacia-viva-backend");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Flyway deve executar as migrations no banco do Testcontainers")
    void flywayShouldHaveAppliedMigrations() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
            Integer.class);

        assertThat(appliedMigrations).isNotNull();
        assertThat(appliedMigrations).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Rotas nao liberadas devem exigir autenticacao")
    void protectedRoutesShouldRequireAuthentication() {
        ResponseEntity<String> response =
            restTemplate.getForEntity("/api/v1/itens", String.class);

        assertThat(response.getStatusCode()).isIn(
            HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
