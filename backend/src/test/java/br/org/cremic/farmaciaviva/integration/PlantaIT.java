package br.org.cremic.farmaciaviva.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.org.cremic.farmaciaviva.planta.PlantaRepository;
import br.org.cremic.farmaciaviva.planta.PlantaRequest;
import br.org.cremic.farmaciaviva.planta.PlantaRequest.NomePopularRequest;
import br.org.cremic.farmaciaviva.planta.PlantaRequest.ReferenciaRequest;
import br.org.cremic.farmaciaviva.planta.PlantaResponse;
import br.org.cremic.farmaciaviva.planta.PlantaSimilarRepository;
import br.org.cremic.farmaciaviva.planta.TipoReferencia;
import br.org.cremic.farmaciaviva.planta.VincularSimilarRequest;
import br.org.cremic.farmaciaviva.shared.exception.ApiError;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
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
import org.springframework.jdbc.core.JdbcTemplate;

class PlantaIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/plantas";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlantaRepository repository;

    @Autowired
    private PlantaSimilarRepository similarRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        similarRepository.deleteAll();
        repository.deleteAll();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private PlantaRequest request(String nomeCientifico, String familia, String... nomesPopulares) {
        List<NomePopularRequest> nomes = java.util.stream.IntStream.range(0, nomesPopulares.length)
            .mapToObj(i -> new NomePopularRequest(nomesPopulares[i], i == 0))
            .toList();
        return new PlantaRequest(nomeCientifico, familia, null, null, null, nomes, List.of());
    }

    private PlantaResponse criar(PlantaRequest request) {
        ResponseEntity<PlantaResponse> resposta =
            restTemplate.postForEntity(BASE_URL, request, PlantaResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).isNotNull();
        return resposta.getBody();
    }

    private PlantaResponse criar(String nomeCientifico, String familia, String... nomesPopulares) {
        return criar(request(nomeCientifico, familia, nomesPopulares));
    }

    private HttpEntity<Void> corpoVazio() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    private ResponseEntity<PlantaResponse> vincular(UUID id, UUID similarId, String observacao) {
        return restTemplate.postForEntity(
            BASE_URL + "/" + id + "/similares",
            new VincularSimilarRequest(similarId, observacao),
            PlantaResponse.class);
    }

    private PlantaResponse ficha(UUID id) {
        ResponseEntity<PlantaResponse> resposta =
            restTemplate.getForEntity(BASE_URL + "/" + id, PlantaResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotNull();
        return resposta.getBody();
    }

    private ResponseEntity<PlantaResponse> arquivar(UUID id) {
        return restTemplate.exchange(
            BASE_URL + "/" + id + "/arquivar", HttpMethod.PATCH, corpoVazio(), PlantaResponse.class);
    }

    private Integer contar(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    // ---------------------------------------------------------------
    // Criacao
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST deve criar a ficha completa com nomes populares e referencias")
    void postDeveCriarFichaCompleta() {
        PlantaRequest request = new PlantaRequest(
            "Mikania glomerata Spreng.",
            "Asteraceae",
            "Trepadeira perene propagada por estacas.",
            "Expectorante.",
            "Especie de referencia.",
            List.of(
                new NomePopularRequest("Guaco", true),
                new NomePopularRequest("Guaco-liso", false)),
            List.of(
                new ReferenciaRequest(TipoReferencia.LEGISLACAO, "Ministerio da Saude",
                    "RENISUS", 2009, null, null),
                new ReferenciaRequest(TipoReferencia.OUTRO, null, null, null, null,
                    "Anotacao de campo do PET-Saude")));

        ResponseEntity<PlantaResponse> resposta =
            restTemplate.postForEntity(BASE_URL, request, PlantaResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getHeaders().getLocation()).isNotNull();
        PlantaResponse corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        assertThat(corpo.id()).isNotNull();
        assertThat(resposta.getHeaders().getLocation().getPath())
            .isEqualTo(BASE_URL + "/" + corpo.id());
        assertThat(corpo.nomeCientifico()).isEqualTo("Mikania glomerata Spreng.");
        assertThat(corpo.familiaBotanica()).isEqualTo("Asteraceae");
        assertThat(corpo.ativo()).isTrue();
        assertThat(corpo.versao()).isZero();
        assertThat(corpo.criadoEm()).isNotNull();
        assertThat(corpo.nomesPopulares()).extracting("nome").containsExactly("Guaco", "Guaco-liso");
        assertThat(corpo.nomesPopulares().get(0).principal()).isTrue();
        assertThat(corpo.referencias()).hasSize(2);
        assertThat(corpo.referencias().get(0).tipo()).isEqualTo(TipoReferencia.LEGISLACAO);
        assertThat(corpo.referencias().get(1).textoLivre()).isEqualTo("Anotacao de campo do PET-Saude");
        assertThat(corpo.similares()).isEmpty();

        // Os filhos chegaram ao banco, nao so ao contexto.
        assertThat(contar("SELECT COUNT(*) FROM planta_nome_popular")).isEqualTo(2);
        assertThat(contar("SELECT COUNT(*) FROM planta_referencia")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT nome_cientifico_normalizado FROM planta", String.class))
            .isEqualTo("MIKANIA GLOMERATA SPRENG");
    }

    @Test
    @DisplayName("POST deve recusar nome cientifico duplicado com 409, mesmo com acento e espacamento diferentes")
    void postDuplicadoDeveRetornar409() {
        criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco");

        ResponseEntity<ApiError> resposta = restTemplate.postForEntity(
            BASE_URL, request("  MIKANIA   GLOMERATA SPRENG ", "Asteraceae"), ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().message()).contains("Mikania glomerata Spreng.");
        assertThat(repository.count()).isEqualTo(1);

        // Acentuacao diferente tambem colide.
        ResponseEntity<ApiError> acentuada = restTemplate.postForEntity(
            BASE_URL, request("Mikania glomeráta Spreng", "Asteraceae"), ApiError.class);
        assertThat(acentuada.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("PUT deve recusar nome cientifico de outra planta com 409")
    void putDuplicadoDeveRetornar409() {
        PlantaResponse guaco = criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco");
        criar("Maytenus ilicifolia Mart. ex Reissek", "Celastraceae", "Espinheira-santa");

        ResponseEntity<ApiError> resposta = restTemplate.exchange(
            BASE_URL + "/" + guaco.id(), HttpMethod.PUT,
            new HttpEntity<>(request("maytenus ilicifolia mart ex reissek", "Celastraceae")),
            ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().message()).contains("Maytenus ilicifolia Mart. ex Reissek");
    }

    @Test
    @DisplayName("POST invalido deve retornar 400 com fieldErrors")
    void postInvalidoDeveRetornar400() {
        PlantaRequest invalido = new PlantaRequest(
            "", "x".repeat(121), null, null, null,
            List.of(new NomePopularRequest("", null)),
            List.of(new ReferenciaRequest(null, null, "t", -1, null, null)));

        ResponseEntity<ApiError> resposta =
            restTemplate.postForEntity(BASE_URL, invalido, ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().fieldErrors()).extracting("field")
            .contains("nomeCientifico", "familiaBotanica", "nomesPopulares[0].nome",
                "referencias[0].tipo", "referencias[0].ano");
    }

    @Test
    @DisplayName("Regras de negocio devem responder 422: referencia sem titulo nem texto, nomes populares repetidos, dois principais")
    void regrasDeNegocioDevemRetornar422() {
        ResponseEntity<ApiError> semTitulo = restTemplate.postForEntity(BASE_URL,
            new PlantaRequest("Lippia alba", null, null, null, null, List.of(),
                List.of(new ReferenciaRequest(TipoReferencia.LIVRO, "Autor", " ", 2020, null, null))),
            ApiError.class);
        assertThat(semTitulo.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(semTitulo.getBody()).isNotNull();
        assertThat(semTitulo.getBody().message()).contains("titulo ou texto livre");

        ResponseEntity<ApiError> repetido = restTemplate.postForEntity(BASE_URL,
            new PlantaRequest("Lippia alba", null, null, null, null,
                List.of(new NomePopularRequest("Erva-cidreira", true),
                    new NomePopularRequest("ERVA CIDREIRA", false)),
                List.of()),
            ApiError.class);
        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        ResponseEntity<ApiError> doisPrincipais = restTemplate.postForEntity(BASE_URL,
            new PlantaRequest("Lippia alba", null, null, null, null,
                List.of(new NomePopularRequest("Erva-cidreira", true),
                    new NomePopularRequest("Falsa-melissa", true)),
                List.of()),
            ApiError.class);
        assertThat(doisPrincipais.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        assertThat(repository.count()).isZero();
    }

    // ---------------------------------------------------------------
    // Atualizacao
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PUT deve substituir as listas mantendo um nome popular igual sem violar o indice unico")
    void putDeveSubstituirListasSemViolarUnicidade() {
        PlantaResponse criada = criar("Lippia alba", "Verbenaceae", "Erva-cidreira", "Falsa-melissa");

        PlantaRequest atualizacao = new PlantaRequest(
            "Lippia alba (Mill.) N.E.Br.", "Verbenaceae", "Pleno sol.", null, null,
            List.of(
                new NomePopularRequest("Erva-cidreira", true),     // mantido
                new NomePopularRequest("Salva-limao", false)),      // novo; Falsa-melissa sai
            List.of(new ReferenciaRequest(TipoReferencia.SITE, null, "Anvisa", null,
                "https://www.gov.br/anvisa", null)));

        ResponseEntity<PlantaResponse> resposta = restTemplate.exchange(
            BASE_URL + "/" + criada.id(), HttpMethod.PUT,
            new HttpEntity<>(atualizacao), PlantaResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        PlantaResponse corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        assertThat(corpo.nomeCientifico()).isEqualTo("Lippia alba (Mill.) N.E.Br.");
        assertThat(corpo.cultivo()).isEqualTo("Pleno sol.");
        assertThat(corpo.nomesPopulares()).extracting("nome")
            .containsExactly("Erva-cidreira", "Salva-limao");
        assertThat(corpo.referencias()).hasSize(1);
        assertThat(corpo.versao()).isGreaterThan(criada.versao());

        assertThat(contar("SELECT COUNT(*) FROM planta_nome_popular")).isEqualTo(2);
        assertThat(contar("SELECT COUNT(*) FROM planta_referencia")).isEqualTo(1);
    }

    // ---------------------------------------------------------------
    // Busca e listagem
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET com busca deve encontrar por nome popular e por nome cientifico, ignorando acento e caixa")
    void buscaDeveEncontrarPorNomePopularECientifico() {
        criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco", "Cipó-caatinga");
        criar("Maytenus ilicifolia Mart. ex Reissek", "Celastraceae", "Espinheira-santa");
        criar("Lippia alba", "Verbenaceae", "Erva-cidreira");

        ResponseEntity<JsonNode> porPopular = restTemplate.getForEntity(
            BASE_URL + "?busca=guaco", JsonNode.class);
        assertThat(porPopular.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(porPopular.getBody()).isNotNull();
        assertThat(porPopular.getBody().get("totalElements").asLong()).isEqualTo(1);
        assertThat(porPopular.getBody().get("content").get(0).get("nomeCientifico").asText())
            .isEqualTo("Mikania glomerata Spreng.");

        ResponseEntity<JsonNode> porCientifico = restTemplate.getForEntity(
            BASE_URL + "?busca=MAYTENUS", JsonNode.class);
        assertThat(porCientifico.getBody()).isNotNull();
        assertThat(porCientifico.getBody().get("totalElements").asLong()).isEqualTo(1);

        ResponseEntity<JsonNode> comAcento = restTemplate.getForEntity(
            BASE_URL + "?busca=cipo caatinga", JsonNode.class);
        assertThat(comAcento.getBody()).isNotNull();
        assertThat(comAcento.getBody().get("totalElements").asLong()).isEqualTo(1);

        ResponseEntity<JsonNode> semResultado = restTemplate.getForEntity(
            BASE_URL + "?busca=inexistente", JsonNode.class);
        assertThat(semResultado.getBody()).isNotNull();
        assertThat(semResultado.getBody().get("totalElements").asLong()).isZero();
    }

    @Test
    @DisplayName("GET deve trazer nomes populares (principal primeiro) e quantidade de similares na listagem")
    void listagemDeveTrazerNomesPopularesEQuantidadeDeSimilares() {
        PlantaResponse a = criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco", "Guaco-liso");
        PlantaResponse b = criar("Mikania laevigata Sch.Bip. ex Baker", "Asteraceae", "Guaco-cheiroso");
        criar("Lippia alba", "Verbenaceae");
        vincular(a.id(), b.id(), null);

        ResponseEntity<JsonNode> resposta = restTemplate.getForEntity(
            BASE_URL + "?sort=nomeCientifico,asc", JsonNode.class);

        assertThat(resposta.getBody()).isNotNull();
        JsonNode conteudo = resposta.getBody().get("content");
        assertThat(conteudo).hasSize(3);
        assertThat(conteudo.get(0).get("nomesPopulares").findValuesAsText(""))
            .isEmpty(); // Lippia alba, sem nomes populares
        assertThat(conteudo.get(1).get("nomeCientifico").asText()).isEqualTo("Mikania glomerata Spreng.");
        assertThat(conteudo.get(1).get("nomesPopulares").get(0).asText()).isEqualTo("Guaco");
        assertThat(conteudo.get(1).get("quantidadeSimilares").asLong()).isEqualTo(1);
        assertThat(conteudo.get(0).get("quantidadeSimilares").asLong()).isZero();
    }

    @Test
    @DisplayName("GET deve filtrar por familia botanica e por situacao")
    void listagemDeveFiltrarPorFamiliaEAtivo() {
        criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco");
        PlantaResponse lippia = criar("Lippia alba", "Verbenaceae", "Erva-cidreira");
        arquivar(lippia.id());

        ResponseEntity<JsonNode> porFamilia = restTemplate.getForEntity(
            BASE_URL + "?familiaBotanica=astera", JsonNode.class);
        assertThat(porFamilia.getBody()).isNotNull();
        assertThat(porFamilia.getBody().get("totalElements").asLong()).isEqualTo(1);

        ResponseEntity<JsonNode> ativas = restTemplate.getForEntity(
            BASE_URL + "?ativo=true", JsonNode.class);
        assertThat(ativas.getBody()).isNotNull();
        assertThat(ativas.getBody().get("totalElements").asLong()).isEqualTo(1);

        ResponseEntity<JsonNode> arquivadas = restTemplate.getForEntity(
            BASE_URL + "?ativo=false", JsonNode.class);
        assertThat(arquivadas.getBody()).isNotNull();
        assertThat(arquivadas.getBody().get("content").get(0).get("nomeCientifico").asText())
            .isEqualTo("Lippia alba");
    }

    @Test
    @DisplayName("GET deve paginar e ordenar; campo de ordenacao inexistente responde 400")
    void listagemDevePaginarEOrdenar() {
        criar("Mikania glomerata Spreng.", "Asteraceae");
        criar("Lippia alba", "Verbenaceae");
        criar("Maytenus ilicifolia Mart. ex Reissek", "Celastraceae");

        ResponseEntity<JsonNode> paginado = restTemplate.getForEntity(
            BASE_URL + "?page=0&size=2&sort=nomeCientifico,asc", JsonNode.class);
        assertThat(paginado.getBody()).isNotNull();
        assertThat(paginado.getBody().get("totalElements").asLong()).isEqualTo(3);
        assertThat(paginado.getBody().get("totalPages").asInt()).isEqualTo(2);
        assertThat(paginado.getBody().get("content")).hasSize(2);
        assertThat(paginado.getBody().get("content").get(0).get("nomeCientifico").asText())
            .isEqualTo("Lippia alba");
        assertThat(paginado.getBody().get("content").get(1).get("nomeCientifico").asText())
            .isEqualTo("Maytenus ilicifolia Mart. ex Reissek");

        ResponseEntity<JsonNode> desc = restTemplate.getForEntity(
            BASE_URL + "?sort=nomeCientifico,desc", JsonNode.class);
        assertThat(desc.getBody()).isNotNull();
        assertThat(desc.getBody().get("content").get(0).get("nomeCientifico").asText())
            .isEqualTo("Mikania glomerata Spreng.");

        ResponseEntity<ApiError> invalido = restTemplate.getForEntity(
            BASE_URL + "?sort=campoInexistente,asc", ApiError.class);
        assertThat(invalido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /opcoes deve listar apenas plantas ativas com o nome popular principal")
    void opcoesDeveListarApenasAtivas() {
        criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco", "Guaco-liso");
        PlantaResponse lippia = criar("Lippia alba", "Verbenaceae", "Erva-cidreira");
        arquivar(lippia.id());

        ResponseEntity<JsonNode> resposta = restTemplate.getForEntity(
            BASE_URL + "/opcoes", JsonNode.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).get("nomeCientifico").asText())
            .isEqualTo("Mikania glomerata Spreng.");
        assertThat(resposta.getBody().get(0).get("nomePopularPrincipal").asText()).isEqualTo("Guaco");
    }

    @Test
    @DisplayName("GET por id inexistente deve retornar 404")
    void getPorIdInexistenteDeveRetornar404() {
        ResponseEntity<ApiError> resposta = restTemplate.getForEntity(
            BASE_URL + "/" + UUID.randomUUID(), ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // Similaridade
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Vincular similares deve criar o reciproco e ambas as fichas devem mostrar o vinculo")
    void vincularDeveCriarReciproco() {
        PlantaResponse a = criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco");
        PlantaResponse b = criar("Mikania laevigata Sch.Bip. ex Baker", "Asteraceae", "Guaco-cheiroso");

        ResponseEntity<PlantaResponse> resposta = vincular(a.id(), b.id(), "Ambas chamadas de guaco");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().similares()).hasSize(1);
        PlantaResponse.SimilarResponse similar = resposta.getBody().similares().get(0);
        assertThat(similar.id()).isEqualTo(b.id());
        assertThat(similar.nomeCientifico()).isEqualTo("Mikania laevigata Sch.Bip. ex Baker");
        assertThat(similar.nomePopularPrincipal()).isEqualTo("Guaco-cheiroso");
        assertThat(similar.ativo()).isTrue();
        assertThat(similar.observacao()).isEqualTo("Ambas chamadas de guaco");

        // O reciproco existe no banco e aparece na ficha da outra planta.
        assertThat(contar("SELECT COUNT(*) FROM planta_similar")).isEqualTo(2);
        PlantaResponse fichaB = ficha(b.id());
        assertThat(fichaB.similares()).hasSize(1);
        assertThat(fichaB.similares().get(0).id()).isEqualTo(a.id());
        assertThat(fichaB.similares().get(0).observacao()).isEqualTo("Ambas chamadas de guaco");
    }

    @Test
    @DisplayName("Desvincular deve remover os dois sentidos")
    void desvincularDeveRemoverOsDoisSentidos() {
        PlantaResponse a = criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco");
        PlantaResponse b = criar("Mikania laevigata Sch.Bip. ex Baker", "Asteraceae");
        vincular(a.id(), b.id(), null);

        ResponseEntity<Void> resposta = restTemplate.exchange(
            BASE_URL + "/" + b.id() + "/similares/" + a.id(),
            HttpMethod.DELETE, corpoVazio(), Void.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(contar("SELECT COUNT(*) FROM planta_similar")).isZero();
        assertThat(ficha(a.id()).similares()).isEmpty();
        assertThat(ficha(b.id()).similares()).isEmpty();

        ResponseEntity<ApiError> deNovo = restTemplate.exchange(
            BASE_URL + "/" + a.id() + "/similares/" + b.id(),
            HttpMethod.DELETE, corpoVazio(), ApiError.class);
        assertThat(deNovo.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Arquivar uma planta deve preservar o vinculo de similaridade, marcado como arquivada")
    void arquivarDevePreservarVinculo() {
        PlantaResponse a = criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco");
        PlantaResponse b = criar("Mikania laevigata Sch.Bip. ex Baker", "Asteraceae");
        vincular(a.id(), b.id(), null);

        ResponseEntity<PlantaResponse> arquivada = arquivar(b.id());

        assertThat(arquivada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(arquivada.getBody()).isNotNull();
        assertThat(arquivada.getBody().ativo()).isFalse();
        assertThat(arquivada.getBody().similares()).hasSize(1);

        assertThat(contar("SELECT COUNT(*) FROM planta_similar")).isEqualTo(2);
        PlantaResponse fichaA = ficha(a.id());
        assertThat(fichaA.similares()).hasSize(1);
        assertThat(fichaA.similares().get(0).id()).isEqualTo(b.id());
        assertThat(fichaA.similares().get(0).ativo()).isFalse();

        // Some da listagem ativa, mas a ficha continua acessivel.
        ResponseEntity<JsonNode> ativas = restTemplate.getForEntity(
            BASE_URL + "?ativo=true", JsonNode.class);
        assertThat(ativas.getBody()).isNotNull();
        assertThat(ativas.getBody().get("totalElements").asLong()).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Vincular deve recusar auto-vinculo, planta arquivada e vinculo duplicado")
    void vincularDeveRecusarCasosInvalidos() {
        PlantaResponse a = criar("Mikania glomerata Spreng.", "Asteraceae", "Guaco");
        PlantaResponse b = criar("Mikania laevigata Sch.Bip. ex Baker", "Asteraceae");
        PlantaResponse c = criar("Lippia alba", "Verbenaceae");
        arquivar(c.id());

        ResponseEntity<ApiError> autoVinculo = restTemplate.postForEntity(
            BASE_URL + "/" + a.id() + "/similares",
            new VincularSimilarRequest(a.id(), null), ApiError.class);
        assertThat(autoVinculo.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        ResponseEntity<ApiError> arquivada = restTemplate.postForEntity(
            BASE_URL + "/" + a.id() + "/similares",
            new VincularSimilarRequest(c.id(), null), ApiError.class);
        assertThat(arquivada.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(arquivada.getBody()).isNotNull();
        assertThat(arquivada.getBody().message()).contains("arquivada");

        assertThat(vincular(a.id(), b.id(), null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<ApiError> duplicado = restTemplate.postForEntity(
            BASE_URL + "/" + b.id() + "/similares",
            new VincularSimilarRequest(a.id(), null), ApiError.class);
        assertThat(duplicado.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<ApiError> inexistente = restTemplate.postForEntity(
            BASE_URL + "/" + a.id() + "/similares",
            new VincularSimilarRequest(UUID.randomUUID(), null), ApiError.class);
        assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(contar("SELECT COUNT(*) FROM planta_similar")).isEqualTo(2);
    }

    @Test
    @DisplayName("Nao ha exclusao fisica: DELETE na planta responde 405")
    void deleteDePlantaDeveRetornar405() {
        PlantaResponse a = criar("Mikania glomerata Spreng.", "Asteraceae");

        ResponseEntity<ApiError> resposta = restTemplate.exchange(
            BASE_URL + "/" + a.id(), HttpMethod.DELETE, corpoVazio(), ApiError.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(repository.count()).isEqualTo(1);
    }
}
