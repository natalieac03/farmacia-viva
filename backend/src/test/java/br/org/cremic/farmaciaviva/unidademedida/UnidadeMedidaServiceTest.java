package br.org.cremic.farmaciaviva.unidademedida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.org.cremic.farmaciaviva.shared.dto.PageResponse;
import br.org.cremic.farmaciaviva.shared.exception.BusinessRuleException;
import br.org.cremic.farmaciaviva.shared.exception.DuplicateResourceException;
import br.org.cremic.farmaciaviva.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UnidadeMedidaServiceTest {

    @Mock
    private UnidadeMedidaRepository repository;

    private UnidadeMedidaService service;

    @BeforeEach
    void setUp() {
        UnidadeMedidaMapper mapper = new UnidadeMedidaMapperImpl();
        service = new UnidadeMedidaService(repository, mapper);
    }

    private UnidadeMedidaRequest requestValido(String codigo) {
        return new UnidadeMedidaRequest(
            codigo,
            "Grama",
            Dimensao.MASSA,
            new BigDecimal("1.000000"),
            true,
            3);
    }

    private UnidadeMedida entidade(UUID id, String codigo, boolean ativo) {
        UnidadeMedida unidade = new UnidadeMedida();
        unidade.setId(id);
        unidade.setCodigo(codigo);
        unidade.setNome("Grama");
        unidade.setDimensao(Dimensao.MASSA);
        unidade.setFatorParaBase(new BigDecimal("1.000000"));
        unidade.setUnidadeBase(true);
        unidade.setCasasDecimais(3);
        unidade.setAtivo(ativo);
        return unidade;
    }

    private void stubSaveAndFlushDevolvendoArgumento() {
        when(repository.saveAndFlush(any(UnidadeMedida.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("criar deve salvar o codigo normalizado em maiusculas")
    void criarDeveNormalizarCodigoParaMaiusculas() {
        when(repository.existsByCodigoIgnoreCase("G")).thenReturn(false);
        stubSaveAndFlushDevolvendoArgumento();

        UnidadeMedidaResponse response = service.criar(requestValido("  g "));

        ArgumentCaptor<UnidadeMedida> captor = ArgumentCaptor.forClass(UnidadeMedida.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("G");
        assertThat(captor.getValue().isAtivo()).isTrue();
        assertThat(response.codigo()).isEqualTo("G");
    }

    @Test
    @DisplayName("criar deve gravar o fator na escala da coluna, sem alterar o valor")
    void criarDeveNormalizarEscalaDoFator() {
        when(repository.existsByCodigoIgnoreCase("KG")).thenReturn(false);
        stubSaveAndFlushDevolvendoArgumento();

        UnidadeMedidaRequest request = new UnidadeMedidaRequest(
            "kg", "Quilograma", Dimensao.MASSA, new BigDecimal("1000.5"), false, 3);

        UnidadeMedidaResponse response = service.criar(request);

        assertThat(response.fatorParaBase().scale())
            .isEqualTo(UnidadeMedidaService.ESCALA_FATOR_PARA_BASE);
        assertThat(response.fatorParaBase()).isEqualByComparingTo("1000.5");
    }

    @Test
    @DisplayName("criar deve rejeitar fator com mais casas decimais do que a coluna suporta")
    void criarDeveRejeitarFatorComEscalaExcessiva() {
        UnidadeMedidaRequest request = new UnidadeMedidaRequest(
            "NG", "Nanograma", Dimensao.MASSA, new BigDecimal("0.0000001"), false, 6);

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("casas decimais");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("criar deve rejeitar fator menor ou igual a zero")
    void criarDeveRejeitarFatorNaoPositivo() {
        UnidadeMedidaRequest request = new UnidadeMedidaRequest(
            "X", "Invalida", Dimensao.MASSA, BigDecimal.ZERO, false, 0);

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("maior que zero");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("criar deve rejeitar casasDecimais fora do intervalo permitido")
    void criarDeveRejeitarCasasDecimaisForaDoIntervalo() {
        UnidadeMedidaRequest request = new UnidadeMedidaRequest(
            "X", "Invalida", Dimensao.MASSA, BigDecimal.ONE, false, 7);

        assertThatThrownBy(() -> service.criar(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("casasDecimais");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("criar deve falhar com DuplicateResourceException quando o codigo ja existe")
    void criarDeveFalharQuandoCodigoDuplicado() {
        when(repository.existsByCodigoIgnoreCase("MG")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(requestValido("mg")))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("MG");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("buscarPorId deve falhar com ResourceNotFoundException quando nao existe")
    void buscarPorIdDeveFalharQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("atualizar deve falhar quando o codigo pertence a outra unidade")
    void atualizarDeveFalharQuandoCodigoPertenceAOutraUnidade() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entidade(id, "G", true)));
        when(repository.existsByCodigoIgnoreCaseAndIdNot("MG", id)).thenReturn(true);

        UnidadeMedidaUpdateRequest update = new UnidadeMedidaUpdateRequest(
            "mg", "Miligrama", Dimensao.MASSA, new BigDecimal("0.001000"), false, 6);

        assertThatThrownBy(() -> service.atualizar(id, update))
            .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("atualizar deve aplicar os novos valores e normalizar o codigo")
    void atualizarDeveAplicarNovosValores() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entidade(id, "G", true)));
        when(repository.existsByCodigoIgnoreCaseAndIdNot("MG", id)).thenReturn(false);
        stubSaveAndFlushDevolvendoArgumento();

        UnidadeMedidaUpdateRequest update = new UnidadeMedidaUpdateRequest(
            "mg", "Miligrama", Dimensao.MASSA, new BigDecimal("0.001000"), false, 6);

        UnidadeMedidaResponse response = service.atualizar(id, update);

        assertThat(response.codigo()).isEqualTo("MG");
        assertThat(response.nome()).isEqualTo("Miligrama");
        assertThat(response.fatorParaBase()).isEqualByComparingTo("0.001");
        assertThat(response.unidadeBase()).isFalse();
        assertThat(response.casasDecimais()).isEqualTo(6);
    }

    @Test
    @DisplayName("atualizar nao deve alterar a situacao ativo da unidade")
    void atualizarNaoDeveAlterarSituacao() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entidade(id, "G", false)));
        when(repository.existsByCodigoIgnoreCaseAndIdNot("G", id)).thenReturn(false);
        stubSaveAndFlushDevolvendoArgumento();

        UnidadeMedidaUpdateRequest update = new UnidadeMedidaUpdateRequest(
            "G", "Grama", Dimensao.MASSA, BigDecimal.ONE, true, 3);

        UnidadeMedidaResponse response = service.atualizar(id, update);

        assertThat(response.ativo()).isFalse();
    }

    @Test
    @DisplayName("arquivar deve alterar ativo para false sem excluir o registro")
    void arquivarDeveDesativarSemExcluir() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entidade(id, "G", true)));
        stubSaveAndFlushDevolvendoArgumento();

        UnidadeMedidaResponse response = service.arquivar(id);

        assertThat(response.ativo()).isFalse();
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("ativar deve alterar ativo para true")
    void ativarDeveReativar() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entidade(id, "G", false)));
        stubSaveAndFlushDevolvendoArgumento();

        UnidadeMedidaResponse response = service.ativar(id);

        assertThat(response.ativo()).isTrue();
    }

    @Test
    @DisplayName("listar sem filtro deve usar findAll")
    void listarSemFiltroDeveUsarFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<UnidadeMedida> pagina =
            new PageImpl<>(List.of(entidade(UUID.randomUUID(), "G", true)), pageable, 1);
        when(repository.findAll(pageable)).thenReturn(pagina);

        PageResponse<UnidadeMedidaResponse> response = service.listar(null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(repository, never()).findByAtivo(any(Boolean.class), any(Pageable.class));
    }

    @Test
    @DisplayName("listar com filtro ativo deve usar findByAtivo")
    void listarComFiltroDeveUsarFindByAtivo() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<UnidadeMedida> pagina =
            new PageImpl<>(List.of(entidade(UUID.randomUUID(), "G", false)), pageable, 1);
        when(repository.findByAtivo(false, pageable)).thenReturn(pagina);

        PageResponse<UnidadeMedidaResponse> response = service.listar(false, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).ativo()).isFalse();
        verify(repository, never()).findAll(any(Pageable.class));
    }
}
