package br.org.cremic.farmaciaviva.unidademedida;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UnidadeMedidaResponse(
    UUID id,
    String codigo,
    String nome,
    Dimensao dimensao,
    BigDecimal fatorParaBase,
    Boolean unidadeBase,
    Integer casasDecimais,
    Boolean ativo,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm
) {
}
