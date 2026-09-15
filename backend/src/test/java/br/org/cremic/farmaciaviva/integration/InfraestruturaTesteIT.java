package br.org.cremic.farmaciaviva.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.org.cremic.farmaciaviva.unidademedida.Dimensao;
import br.org.cremic.farmaciaviva.unidademedida.UnidadeMedidaRequest;
import br.org.cremic.farmaciaviva.unidademedida.UnidadeMedidaResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Teste descartavel de aceite da infraestrutura de testes de integracao.
 *
 * Existe para provar tres coisas de uma vez:
 *   1. o failsafe executa as classes *IT nas fases integration-test e verify;
 *   2. o Testcontainers sobe um PostgreSQL real e a aplicacao fala com ele;
 *   3. uma requisicao HTTP completa atravessa controller, service, JPA e banco.
 *
 * Pode ser removido assim que a infraestrutura estiver estabelecida; os testes
 * de comportamento do modulo ficam em UnidadeMedidaIT.
 */
class InfraestruturaTesteIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/unidades-medida";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("O banco do teste deve ser um PostgreSQL real com as migrations aplicadas")
    void deveEstarConectadoAUmPostgresRealComMigrations() {
        String versao = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(versao).contains("PostgreSQL 16");

        Integer migrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
            Integer.class);
        assertThat(migrations).isGreaterThanOrEqualTo(2);

        // A tabela so existe se a V2 tiver sido realmente aplicada neste banco.
        Integer tabela = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'unidade_medida'",
            Integer.class);
        assertThat(tabela).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/v1/unidades-medida deve retornar 200 com o dado gravado no banco")
    void deveListarUnidadeGravadaNoBancoReal() {
        ResponseEntity<UnidadeMedidaResponse> criada = restTemplate.postForEntity(
            BASE_URL,
            new UnidadeMedidaRequest(
                "KG-IT", "Quilograma (aceite)", Dimensao.MASSA,
                new BigDecimal("1.000000"), true, 3),
            UnidadeMedidaResponse.class);

        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criada.getBody()).isNotNull();
        assertThat(criada.getBody().id()).isNotNull();

        // Confirma que o POST chegou mesmo ao banco, e nao apenas ao contexto.
        Integer gravadas = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM unidade_medida WHERE codigo = 'KG-IT'",
            Integer.class);
        assertThat(gravadas).isEqualTo(1);

        ResponseEntity<JsonNode> listagem =
            restTemplate.getForEntity(BASE_URL, JsonNode.class);

        assertThat(listagem.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listagem.getBody()).isNotNull();

        JsonNode corpo = listagem.getBody();
        assertThat(corpo.get("content")).isNotNull();
        assertThat(corpo.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(corpo.get("content").findValuesAsText("codigo")).contains("KG-IT");
    }
}
