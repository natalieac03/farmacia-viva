package br.org.cremic.farmaciaviva.planta;

import java.util.List;
import java.util.UUID;

/** Linha da listagem paginada. Nomes populares vem com o principal primeiro. */
public record PlantaResumoResponse(
    UUID id,
    String nomeCientifico,
    String familiaBotanica,
    List<String> nomesPopulares,
    Long quantidadeSimilares,
    Boolean ativo
) {
}
