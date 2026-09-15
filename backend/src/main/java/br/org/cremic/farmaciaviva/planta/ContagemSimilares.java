package br.org.cremic.farmaciaviva.planta;

import java.util.UUID;

/** Projecao de consulta: quantidade de vinculos de similaridade por planta. */
public record ContagemSimilares(UUID plantaId, long quantidade) {
}
