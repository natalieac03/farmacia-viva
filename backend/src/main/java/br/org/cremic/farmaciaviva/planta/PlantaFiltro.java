package br.org.cremic.farmaciaviva.planta;

/** Filtros da listagem de plantas. Todos opcionais. */
public record PlantaFiltro(
    String busca,
    String familiaBotanica,
    Boolean ativo
) {
}
