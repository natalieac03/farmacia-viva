package br.org.cremic.farmaciaviva.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.org.cremic.farmaciaviva.shared.exception.ApiError;
import br.org.cremic.farmaciaviva.unidademedida.Dimensao;
import br.org.cremic.farmaciaviva.unidademedida.UnidadeMedidaRepository;
import br.org.cremic.farmaciaviva.unidademedida.UnidadeMedidaRequest;
import br.org.cremic.farmaciaviva.unidademedida.UnidadeMedidaResponse;
import br.org.cremic.farmaciaviva.unidademedida.UnidadeMedidaUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class UnidadeMedidaIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/unidades-medida";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UnidadeMedidaRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    private UnidadeMedidaRequest request(String codigo, String nome) {
        return new UnidadeMedidaRequest(
            codigo, nome, Dimensao.MASSA, new BigDecimal("1.000000"), true, 3);
    }

    private UnidadeMedidaResponse criar(String codigo, String nome) {
        ResponseEntity<UnidadeMedidaResponse> resposta = restTemplate.postForEntity(
            BASE_URL, request(codigo, nome), UnidadeMedidaResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).isNotNull();
        return resposta.getBody();
    }

    private HttpEntity<Void> corpoVazio() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    @Test
    @DisplayName("POST deve criar a unidade com codigo em maiusculas e retornar 201 com Location")
    void postDeveCriarComCodigoEmMaiusculas() {
        ResponseEntity<UnidadeMedidaResponse> resposta = restTemplate.postForEntity(
            BASE_URL, request("mg", "Miligrama"), UnidadeMedidaResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getHeaders().getLocation()).isNotNull();
        UnidadeMedidaResponse corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        assertThat(corpo.id()).isNotNull();
        assertThat(corpo.codigo()).isEqualTo("MG");
        assertThat(corpo.ativo()).isTrue();
        assertThat(corpo.criadoEm()).isNotNull();
        assertThat(corpo.atualizadoEm()).isNotNull();
        assertThat(resposta.getHeaders().getLocation().getPath())
            .isEqualTo(BASE_URL + "/" + corpo.id());
    }

    @Test
    @DisplayName("POST com codigo duplicado ignorando maiusculas deve retornar 409")
    void postDuplicadoIgnorandoCaseDeveRetornar409() {
        criar("MG", "Miligrama");

        ResponseEntity<ApiError> resposta = restTemplate.postForEntity(
            BASE_URL, request("mg", "Miligrama duplicada"), ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().message()).contains("MG");
    }

    @Test
    @DisplayName("POST invalido deve retornar 400 com fieldErrors")
    void postInvalidoDeveRetornar400() {
        UnidadeMedidaRequest invalido = new UnidadeMedidaRequest(
            "", "", null, new BigDecimal("-1"), null, 9);

        ResponseEntity<ApiError> resposta =
            restTemplate.postForEntity(BASE_URL, invalido, ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().fieldErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("GET por id deve retornar a unidade criada")
    void getPorIdDeveRetornarUnidade() {
        UnidadeMedidaResponse criada = criar("G", "Grama");

        ResponseEntity<UnidadeMedidaResponse> resposta = restTemplate.getForEntity(
            BASE_URL + "/" + criada.id(), UnidadeMedidaResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().id()).isEqualTo(criada.id());
        assertThat(resposta.getBody().codigo()).isEqualTo("G");
    }

    @Test
    @DisplayName("GET por id inexistente deve retornar 404")
    void getPorIdInexistenteDeveRetornar404() {
        ResponseEntity<ApiError> resposta = restTemplate.getForEntity(
            BASE_URL + "/" + UUID.randomUUID(), ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT deve atualizar os dados e rejeitar codigo de outra unidade")
    void putDeveAtualizarERejeitarCodigoDuplicado() {
        UnidadeMedidaResponse grama = criar("G", "Grama");
        criar("MG", "Miligrama");

        UnidadeMedidaUpdateRequest atualizacao = new UnidadeMedidaUpdateRequest(
            "kg", "Quilograma", Dimensao.MASSA, new BigDecimal("1000.000000"), false, 3);

        ResponseEntity<UnidadeMedidaResponse> ok = restTemplate.exchange(
            BASE_URL + "/" + grama.id(), HttpMethod.PUT,
            new HttpEntity<>(atualizacao), UnidadeMedidaResponse.class);

        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody()).isNotNull();
        assertThat(ok.getBody().codigo()).isEqualTo("KG");
        assertThat(ok.getBody().nome()).isEqualTo("Quilograma");
        assertThat(ok.getBody().fatorParaBase()).isEqualByComparingTo("1000");

        UnidadeMedidaUpdateRequest duplicada = new UnidadeMedidaUpdateRequest(
            "mg", "Conflito", Dimensao.MASSA, new BigDecimal("0.001000"), false, 6);

        ResponseEntity<ApiError> conflito = restTemplate.exchange(
            BASE_URL + "/" + grama.id(), HttpMethod.PUT,
            new HttpEntity<>(duplicada), ApiError.class);

        assertThat(conflito.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("PATCH arquivar deve desativar sem excluir e manter a unidade visivel")
    void patchArquivarDeveDesativarSemExcluir() {
        UnidadeMedidaResponse criada = criar("UN", "Unidade");

        ResponseEntity<UnidadeMedidaResponse> arquivada = restTemplate.exchange(
            BASE_URL + "/" + criada.id() + "/arquivar", HttpMethod.PATCH,
            corpoVazio(), UnidadeMedidaResponse.class);

        assertThat(arquivada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(arquivada.getBody()).isNotNull();
        assertThat(arquivada.getBody().ativo()).isFalse();

        ResponseEntity<UnidadeMedidaResponse> aindaVisivel = restTemplate.getForEntity(
            BASE_URL + "/" + criada.id(), UnidadeMedidaResponse.class);

        assertThat(aindaVisivel.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aindaVisivel.getBody()).isNotNull();
        assertThat(aindaVisivel.getBody().ativo()).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("PATCH ativar deve reativar uma unidade arquivada")
    void patchAtivarDeveReativar() {
        UnidadeMedidaResponse criada = criar("CP", "Comprimido");

        restTemplate.exchange(
            BASE_URL + "/" + criada.id() + "/arquivar", HttpMethod.PATCH,
            corpoVazio(), UnidadeMedidaResponse.class);

        ResponseEntity<UnidadeMedidaResponse> reativada = restTemplate.exchange(
            BASE_URL + "/" + criada.id() + "/ativar", HttpMethod.PATCH,
            corpoVazio(), UnidadeMedidaResponse.class);

        assertThat(reativada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reativada.getBody()).isNotNull();
        assertThat(reativada.getBody().ativo()).isTrue();
    }

    @Test
    @DisplayName("GET deve filtrar por ativo e paginar os resultados")
    void getDeveFiltrarPorAtivoEPaginar() {
        UnidadeMedidaResponse grama = criar("G", "Grama");
        criar("MG", "Miligrama");
        criar("KG", "Quilograma");

        restTemplate.exchange(
            BASE_URL + "/" + grama.id() + "/arquivar", HttpMethod.PATCH,
            corpoVazio(), UnidadeMedidaResponse.class);

        ResponseEntity<JsonNode> ativas = restTemplate.getForEntity(
            BASE_URL + "?ativo=true", JsonNode.class);

        assertThat(ativas.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ativas.getBody()).isNotNull();
        assertThat(ativas.getBody().get("totalElements").asLong()).isEqualTo(2);

        ResponseEntity<JsonNode> arquivadas = restTemplate.getForEntity(
            BASE_URL + "?ativo=false", JsonNode.class);

        assertThat(arquivadas.getBody()).isNotNull();
        assertThat(arquivadas.getBody().get("totalElements").asLong()).isEqualTo(1);
        assertThat(arquivadas.getBody().get("content").get(0).get("codigo").asText())
            .isEqualTo("G");

        ResponseEntity<JsonNode> paginado = restTemplate.getForEntity(
            BASE_URL + "?page=0&size=2&sort=codigo,asc", JsonNode.class);

        assertThat(paginado.getBody()).isNotNull();
        assertThat(paginado.getBody().get("totalElements").asLong()).isEqualTo(3);
        assertThat(paginado.getBody().get("totalPages").asInt()).isEqualTo(2);
        assertThat(paginado.getBody().get("content")).hasSize(2);
        assertThat(paginado.getBody().get("content").get(0).get("codigo").asText())
            .isEqualTo("G");
        assertThat(paginado.getBody().get("content").get(1).get("codigo").asText())
            .isEqualTo("KG");
    }

    @Test
    @DisplayName("PUT deve atualizar atualizadoEm e preservar criadoEm na propria resposta")
    void putDeveAtualizarDataDeAtualizacaoNaResposta() {
        UnidadeMedidaResponse criada = criar("L", "Litro");

        UnidadeMedidaUpdateRequest atualizacao = new UnidadeMedidaUpdateRequest(
            "L", "Litro (base)", Dimensao.VOLUME, new BigDecimal("1.000000"), true, 3);

        ResponseEntity<UnidadeMedidaResponse> resposta = restTemplate.exchange(
            BASE_URL + "/" + criada.id(), HttpMethod.PUT,
            new HttpEntity<>(atualizacao), UnidadeMedidaResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        UnidadeMedidaResponse corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        // Tolerancia de precisao: o valor em memoria pode ter resolucao maior do que
        // a armazenada pelo PostgreSQL (microssegundos).
        assertThat(corpo.criadoEm())
            .isCloseTo(criada.criadoEm(), within(1, ChronoUnit.SECONDS));
        assertThat(corpo.atualizadoEm()).isAfterOrEqualTo(corpo.criadoEm());

        ResponseEntity<UnidadeMedidaResponse> relido = restTemplate.getForEntity(
            BASE_URL + "/" + criada.id(), UnidadeMedidaResponse.class);

        assertThat(relido.getBody()).isNotNull();
        assertThat(relido.getBody().atualizadoEm())
            .isCloseTo(corpo.atualizadoEm(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("O fator deve ser persistido na escala da coluna e nao sofrer truncamento")
    void fatorDeveSerPersistidoNaEscalaDaColuna() {
        UnidadeMedidaRequest request = new UnidadeMedidaRequest(
            "MCG", "Micrograma", Dimensao.MASSA, new BigDecimal("0.000001"), false, 6);

        ResponseEntity<UnidadeMedidaResponse> criacao =
            restTemplate.postForEntity(BASE_URL, request, UnidadeMedidaResponse.class);

        assertThat(criacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criacao.getBody()).isNotNull();
        assertThat(criacao.getBody().fatorParaBase()).isEqualByComparingTo("0.000001");

        ResponseEntity<UnidadeMedidaResponse> relido = restTemplate.getForEntity(
            BASE_URL + "/" + criacao.getBody().id(), UnidadeMedidaResponse.class);

        assertThat(relido.getBody()).isNotNull();
        assertThat(relido.getBody().fatorParaBase())
            .isEqualByComparingTo(criacao.getBody().fatorParaBase());
        assertThat(relido.getBody().fatorParaBase().scale())
            .isEqualTo(criacao.getBody().fatorParaBase().scale());
    }

    @Test
    @DisplayName("Ordenacao por campo inexistente deve retornar 400 e nao 500")
    void ordenacaoInvalidaDeveRetornar400() {
        criar("G", "Grama");

        ResponseEntity<ApiError> resposta = restTemplate.getForEntity(
            BASE_URL + "?sort=campoInexistente,asc", ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().message()).contains("campoInexistente");
    }

    @Test
    @DisplayName("Tamanho de pagina acima do limite deve ser reduzido ao maximo configurado")
    void tamanhoDePaginaDeveRespeitarLimiteMaximo() {
        criar("G", "Grama");

        ResponseEntity<JsonNode> resposta = restTemplate.getForEntity(
            BASE_URL + "?size=100000", JsonNode.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().get("size").asInt()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Filtro ativo com valor invalido deve retornar 400")
    void filtroAtivoInvalidoDeveRetornar400() {
        ResponseEntity<ApiError> resposta = restTemplate.getForEntity(
            BASE_URL + "?ativo=talvez", ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Metodo nao suportado no recurso deve retornar 405 e nao 500")
    void metodoNaoSuportadoDeveRetornar405() {
        UnidadeMedidaResponse criada = criar("G", "Grama");

        ResponseEntity<ApiError> resposta = restTemplate.exchange(
            BASE_URL + "/" + criada.id(), HttpMethod.DELETE,
            corpoVazio(), ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Arquivar duas vezes deve ser idempotente e manter o registro")
    void arquivarDuasVezesDeveSerIdempotente() {
        UnidadeMedidaResponse criada = criar("G", "Grama");

        restTemplate.exchange(BASE_URL + "/" + criada.id() + "/arquivar",
            HttpMethod.PATCH, corpoVazio(), UnidadeMedidaResponse.class);
        ResponseEntity<UnidadeMedidaResponse> segunda = restTemplate.exchange(
            BASE_URL + "/" + criada.id() + "/arquivar",
            HttpMethod.PATCH, corpoVazio(), UnidadeMedidaResponse.class);

        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(segunda.getBody()).isNotNull();
        assertThat(segunda.getBody().ativo()).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Constraint unica do banco deve impedir duplicidade mesmo em corrida")
    void constraintUnicaDoBancoDeveImpedirDuplicidade() {
        criar("AMP", "Ampola");

        ResponseEntity<ApiError> resposta = restTemplate.postForEntity(
            BASE_URL, request("amp", "Ampola duplicada"), ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repository.count()).isEqualTo(1);
    }
}
