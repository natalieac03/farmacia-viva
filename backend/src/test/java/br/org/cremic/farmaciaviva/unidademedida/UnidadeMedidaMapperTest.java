package br.org.cremic.farmaciaviva.unidademedida;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnidadeMedidaMapperTest {

    private final UnidadeMedidaMapper mapper = new UnidadeMedidaMapperImpl();

    @Test
    @DisplayName("toEntity deve copiar os campos do request e manter ativo como true")
    void toEntityDeveCopiarCampos() {
        UnidadeMedidaRequest request = new UnidadeMedidaRequest(
            "ML", "Mililitro", Dimensao.VOLUME, new BigDecimal("0.001000"), false, 2);

        UnidadeMedida entidade = mapper.toEntity(request);

        assertThat(entidade.getId()).isNull();
        assertThat(entidade.getCodigo()).isEqualTo("ML");
        assertThat(entidade.getNome()).isEqualTo("Mililitro");
        assertThat(entidade.getDimensao()).isEqualTo(Dimensao.VOLUME);
        assertThat(entidade.getFatorParaBase()).isEqualByComparingTo("0.001");
        assertThat(entidade.isUnidadeBase()).isFalse();
        assertThat(entidade.getCasasDecimais()).isEqualTo(2);
        assertThat(entidade.isAtivo()).isTrue();
        assertThat(entidade.getCriadoEm()).isNull();
        assertThat(entidade.getAtualizadoEm()).isNull();
    }

    @Test
    @DisplayName("toResponse deve copiar todos os campos da entidade")
    void toResponseDeveCopiarCampos() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        UnidadeMedida entidade = new UnidadeMedida();
        entidade.setId(UUID.randomUUID());
        entidade.setCodigo("UN");
        entidade.setNome("Unidade");
        entidade.setDimensao(Dimensao.CONTAGEM);
        entidade.setFatorParaBase(BigDecimal.ONE);
        entidade.setUnidadeBase(true);
        entidade.setCasasDecimais(0);
        entidade.setAtivo(false);
        entidade.setCriadoEm(agora);
        entidade.setAtualizadoEm(agora);

        UnidadeMedidaResponse response = mapper.toResponse(entidade);

        assertThat(response.id()).isEqualTo(entidade.getId());
        assertThat(response.codigo()).isEqualTo("UN");
        assertThat(response.nome()).isEqualTo("Unidade");
        assertThat(response.dimensao()).isEqualTo(Dimensao.CONTAGEM);
        assertThat(response.fatorParaBase()).isEqualByComparingTo("1");
        assertThat(response.unidadeBase()).isTrue();
        assertThat(response.casasDecimais()).isZero();
        assertThat(response.ativo()).isFalse();
        assertThat(response.criadoEm()).isEqualTo(agora);
        assertThat(response.atualizadoEm()).isEqualTo(agora);
    }

    @Test
    @DisplayName("updateEntity nao deve alterar id, ativo e datas de auditoria")
    void updateEntityDevePreservarCamposDeControle() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        UUID id = UUID.randomUUID();
        UnidadeMedida entidade = new UnidadeMedida();
        entidade.setId(id);
        entidade.setCodigo("G");
        entidade.setNome("Grama");
        entidade.setDimensao(Dimensao.MASSA);
        entidade.setFatorParaBase(BigDecimal.ONE);
        entidade.setUnidadeBase(true);
        entidade.setCasasDecimais(3);
        entidade.setAtivo(false);
        entidade.setCriadoEm(agora);
        entidade.setAtualizadoEm(agora);

        UnidadeMedidaUpdateRequest update = new UnidadeMedidaUpdateRequest(
            "KG", "Quilograma", Dimensao.MASSA, new BigDecimal("1000.000000"), false, 3);

        mapper.updateEntity(update, entidade);

        assertThat(entidade.getId()).isEqualTo(id);
        assertThat(entidade.isAtivo()).isFalse();
        assertThat(entidade.getCriadoEm()).isEqualTo(agora);
        assertThat(entidade.getAtualizadoEm()).isEqualTo(agora);
        assertThat(entidade.getCodigo()).isEqualTo("KG");
        assertThat(entidade.getNome()).isEqualTo("Quilograma");
        assertThat(entidade.getFatorParaBase()).isEqualByComparingTo("1000");
    }
}
