package br.org.cremic.farmaciaviva.planta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.org.cremic.farmaciaviva.planta.PlantaRequest.NomePopularRequest;
import br.org.cremic.farmaciaviva.planta.PlantaRequest.ReferenciaRequest;
import br.org.cremic.farmaciaviva.shared.exception.BusinessRuleException;
import br.org.cremic.farmaciaviva.shared.exception.DuplicateResourceException;
import br.org.cremic.farmaciaviva.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlantaServiceTest {

    @Mock
    private PlantaRepository repository;

    @Mock
    private PlantaSimilarRepository similarRepository;

    private PlantaService service;

    @BeforeEach
    void setUp() {
        PlantaMapper mapper = new PlantaMapperImpl();
        service = new PlantaService(repository, similarRepository, mapper);
    }

    private PlantaRequest request(
        String nomeCientifico,
        List<NomePopularRequest> nomesPopulares,
        List<ReferenciaRequest> referencias
    ) {
        return new PlantaRequest(
            nomeCientifico, "Asteraceae", null, null, null, nomesPopulares, referencias);
    }

    private Planta entidade(UUID id, String nomeCientifico, boolean ativo) {
        Planta planta = new Planta();
        planta.setId(id);
        planta.setNomeCientifico(nomeCientifico);
        planta.setNomeCientificoNormalizado(
            br.org.cremic.farmaciaviva.shared.texto.NormalizadorTexto.normalizar(nomeCientifico));
        planta.setAtivo(ativo);
        return planta;
    }

    private void stubSaveAndFlushDevolvendoArgumento() {
        when(repository.saveAndFlush(any(Planta.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------
    // Normalizacao do nome cientifico
    // ---------------------------------------------------------------

    @Test
    @DisplayName("criar deve aparar o nome cientifico e gravar a forma normalizada")
    void criarDeveNormalizarNomeCientifico() {
        when(repository.findByNomeCientificoNormalizado("MIKANIA GLOMERATA SPRENG"))
            .thenReturn(Optional.empty());
        stubSaveAndFlushDevolvendoArgumento();

        PlantaResponse response = service.criar(
            request("  Mikania glomerata  Spreng. ", List.of(), List.of()));

        ArgumentCaptor<Planta> captor = ArgumentCaptor.forClass(Planta.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNomeCientifico()).isEqualTo("Mikania glomerata  Spreng.");
        assertThat(captor.getValue().getNomeCientificoNormalizado())
            .isEqualTo("MIKANIA GLOMERATA SPRENG");
        assertThat(captor.getValue().isAtivo()).isTrue();
        assertThat(response.nomeCientifico()).isEqualTo("Mikania glomerata  Spreng.");
    }

    @Test
    @DisplayName("criar deve recusar nome cientifico ja usado, informando qual planta o usa")
    void criarDeveRecusarNomeCientificoDuplicado() {
        Planta existente = entidade(UUID.randomUUID(), "Mikania glomerata Spreng.", true);
        when(repository.findByNomeCientificoNormalizado("MIKANIA GLOMERATA SPRENG"))
            .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.criar(
            request("MIKANIA GLOMERATA SPRENG", List.of(), List.of())))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Mikania glomerata Spreng.")
            .hasMessageContaining(existente.getId().toString());

        verify(repository, never()).saveAndFlush(any());
    }

    // ---------------------------------------------------------------
    // Nomes populares
    // ---------------------------------------------------------------

    @Test
    @DisplayName("criar deve recusar nomes populares repetidos apos normalizacao")
    void criarDeveRecusarNomePopularRepetido() {
        when(repository.findByNomeCientificoNormalizado(any())).thenReturn(Optional.empty());

        PlantaRequest request = request("Lippia alba", List.of(
            new NomePopularRequest("Erva-cidreira", true),
            new NomePopularRequest("erva cidreira", false)), List.of());

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("repetido");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("criar deve recusar mais de um nome popular principal")
    void criarDeveRecusarDoisPrincipais() {
        when(repository.findByNomeCientificoNormalizado(any())).thenReturn(Optional.empty());

        PlantaRequest request = request("Lippia alba", List.of(
            new NomePopularRequest("Erva-cidreira", true),
            new NomePopularRequest("Falsa-melissa", true)), List.of());

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("principal");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("criar deve numerar a ordem dos nomes populares pela posicao na lista")
    void criarDeveNumerarOrdemDosNomesPopulares() {
        when(repository.findByNomeCientificoNormalizado(any())).thenReturn(Optional.empty());
        stubSaveAndFlushDevolvendoArgumento();

        PlantaResponse response = service.criar(request("Lippia alba", List.of(
            new NomePopularRequest("Erva-cidreira", true),
            new NomePopularRequest("Falsa-melissa", false)), List.of()));

        assertThat(response.nomesPopulares()).hasSize(2);
        assertThat(response.nomesPopulares().get(0).ordem()).isZero();
        assertThat(response.nomesPopulares().get(0).principal()).isTrue();
        assertThat(response.nomesPopulares().get(1).ordem()).isEqualTo(1);
    }

    // ---------------------------------------------------------------
    // Referencias
    // ---------------------------------------------------------------

    @Test
    @DisplayName("criar deve recusar referencia sem titulo e sem texto livre")
    void criarDeveRecusarReferenciaSemTituloNemTexto() {
        when(repository.findByNomeCientificoNormalizado(any())).thenReturn(Optional.empty());

        PlantaRequest request = request("Lippia alba", List.of(), List.of(
            new ReferenciaRequest(TipoReferencia.LIVRO, "Autor", "   ", 2020, null, "  ")));

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("titulo ou texto livre");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("criar deve aceitar referencia apenas com texto livre")
    void criarDeveAceitarReferenciaSoComTextoLivre() {
        when(repository.findByNomeCientificoNormalizado(any())).thenReturn(Optional.empty());
        stubSaveAndFlushDevolvendoArgumento();

        PlantaResponse response = service.criar(request("Lippia alba", List.of(), List.of(
            new ReferenciaRequest(TipoReferencia.OUTRO, null, null, null, null,
                "Anotacao de campo"))));

        assertThat(response.referencias()).hasSize(1);
        assertThat(response.referencias().get(0).titulo()).isNull();
        assertThat(response.referencias().get(0).textoLivre()).isEqualTo("Anotacao de campo");
    }

    // ---------------------------------------------------------------
    // Similaridade
    // ---------------------------------------------------------------

    @Test
    @DisplayName("vincular deve recusar planta vinculada a si mesma")
    void vincularDeveRecusarAutoVinculo() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.vincularSimilar(id, new VincularSimilarRequest(id, null)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("dela mesma");

        verify(similarRepository, never()).save(any());
    }

    @Test
    @DisplayName("vincular deve recusar quando a outra planta esta arquivada")
    void vincularDeveRecusarPlantaArquivada() {
        UUID id = UUID.randomUUID();
        UUID similarId = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entidade(id, "A", true)));
        when(repository.findById(similarId)).thenReturn(Optional.of(entidade(similarId, "B", false)));

        assertThatThrownBy(() -> service.vincularSimilar(id, new VincularSimilarRequest(similarId, null)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("arquivada");

        verify(similarRepository, never()).save(any());
    }

    @Test
    @DisplayName("vincular deve recusar vinculo ja existente")
    void vincularDeveRecusarDuplicado() {
        UUID id = UUID.randomUUID();
        UUID similarId = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entidade(id, "A", true)));
        when(repository.findById(similarId)).thenReturn(Optional.of(entidade(similarId, "B", true)));
        when(similarRepository.existsByPlantaIdAndPlantaSimilarId(id, similarId)).thenReturn(true);

        assertThatThrownBy(() -> service.vincularSimilar(id, new VincularSimilarRequest(similarId, null)))
            .isInstanceOf(DuplicateResourceException.class);

        verify(similarRepository, never()).save(any());
    }

    @Test
    @DisplayName("vincular deve gravar o vinculo nos dois sentidos com a mesma observacao")
    void vincularDeveGravarReciproco() {
        UUID id = UUID.randomUUID();
        UUID similarId = UUID.randomUUID();
        Planta a = entidade(id, "A", true);
        Planta b = entidade(similarId, "B", true);
        when(repository.findById(id)).thenReturn(Optional.of(a));
        when(repository.findById(similarId)).thenReturn(Optional.of(b));
        when(similarRepository.existsByPlantaIdAndPlantaSimilarId(id, similarId)).thenReturn(false);
        when(repository.buscarComNomesPopulares(id)).thenReturn(Optional.of(a));
        when(repository.buscarComReferencias(id)).thenReturn(Optional.of(a));
        when(similarRepository.listarPorPlanta(id)).thenReturn(List.of());

        service.vincularSimilar(id, new VincularSimilarRequest(similarId, "  mesmo uso  "));

        ArgumentCaptor<PlantaSimilar> captor = ArgumentCaptor.forClass(PlantaSimilar.class);
        verify(similarRepository, times(2)).save(captor.capture());
        List<PlantaSimilar> gravados = captor.getAllValues();
        assertThat(gravados.get(0).getPlanta()).isSameAs(a);
        assertThat(gravados.get(0).getPlantaSimilar()).isSameAs(b);
        assertThat(gravados.get(1).getPlanta()).isSameAs(b);
        assertThat(gravados.get(1).getPlantaSimilar()).isSameAs(a);
        assertThat(gravados).allSatisfy(v -> assertThat(v.getObservacao()).isEqualTo("mesmo uso"));
    }

    @Test
    @DisplayName("desvincular deve falhar com 404 quando o vinculo nao existe")
    void desvincularDeveFalharQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        UUID similarId = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entidade(id, "A", true)));
        when(similarRepository.deleteByPlantaIdAndPlantaSimilarId(id, similarId)).thenReturn(0L);
        when(similarRepository.deleteByPlantaIdAndPlantaSimilarId(similarId, id)).thenReturn(0L);

        assertThatThrownBy(() -> service.desvincularSimilar(id, similarId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorId deve falhar com ResourceNotFoundException quando nao existe")
    void buscarPorIdDeveFalharQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.buscarComNomesPopulares(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(id.toString());
    }
}
