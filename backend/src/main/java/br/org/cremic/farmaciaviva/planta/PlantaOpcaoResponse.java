package br.org.cremic.farmaciaviva.planta;

import java.util.UUID;

/** Opcao para selects de outros modulos. Apenas plantas ativas. */
public record PlantaOpcaoResponse(
    UUID id,
    String nomeCientifico,
    String nomePopularPrincipal
) {
}
