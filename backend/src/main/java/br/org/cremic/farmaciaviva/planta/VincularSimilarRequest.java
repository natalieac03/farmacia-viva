package br.org.cremic.farmaciaviva.planta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record VincularSimilarRequest(

    @NotNull(message = "plantaSimilarId e obrigatorio")
    UUID plantaSimilarId,

    @Size(max = 500, message = "observacao deve ter no maximo 500 caracteres")
    String observacao
) {
}
