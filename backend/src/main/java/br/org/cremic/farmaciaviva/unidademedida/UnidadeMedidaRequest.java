package br.org.cremic.farmaciaviva.unidademedida;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UnidadeMedidaRequest(

    @NotBlank(message = "codigo e obrigatorio")
    @Size(max = 20, message = "codigo deve ter no maximo 20 caracteres")
    String codigo,

    @NotBlank(message = "nome e obrigatorio")
    @Size(max = 100, message = "nome deve ter no maximo 100 caracteres")
    String nome,

    @NotNull(message = "dimensao e obrigatoria")
    Dimensao dimensao,

    @NotNull(message = "fatorParaBase e obrigatorio")
    @Positive(message = "fatorParaBase deve ser maior que zero")
    @Digits(integer = 13, fraction = 6,
        message = "fatorParaBase deve ter no maximo 13 digitos inteiros e 6 decimais")
    BigDecimal fatorParaBase,

    @NotNull(message = "unidadeBase e obrigatoria")
    Boolean unidadeBase,

    @NotNull(message = "casasDecimais e obrigatoria")
    @Min(value = 0, message = "casasDecimais deve ser no minimo 0")
    @Max(value = 6, message = "casasDecimais deve ser no maximo 6")
    Integer casasDecimais
) {
}
