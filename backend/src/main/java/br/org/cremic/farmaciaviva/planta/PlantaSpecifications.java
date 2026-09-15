package br.org.cremic.farmaciaviva.planta;

import br.org.cremic.farmaciaviva.shared.texto.NormalizadorTexto;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros da listagem como Specification.
 *
 * A Specification e usada com findAll(spec, pageable) de proposito: a
 * paginacao e a ordenacao ficam no SQL, e um campo de ordenacao inexistente
 * vira PropertyReferenceException (400 pelo GlobalExceptionHandler), como no
 * modulo de unidade de medida.
 */
final class PlantaSpecifications {

    private PlantaSpecifications() {
    }

    static Specification<Planta> comFiltro(PlantaFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (filtro.ativo() != null) {
                predicados.add(cb.equal(root.get("ativo"), filtro.ativo()));
            }

            String familia = NormalizadorTexto.emBrancoParaNulo(filtro.familiaBotanica());
            if (familia != null) {
                predicados.add(cb.like(
                    cb.upper(root.get("familiaBotanica")),
                    "%" + familia.toUpperCase(Locale.ROOT) + "%"));
            }

            String busca = NormalizadorTexto.emBrancoParaNulo(filtro.busca());
            if (busca != null) {
                predicados.add(porNomeCientificoOuPopular(root, query, cb, busca));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    /**
     * Quem digita "guaco" precisa encontrar Mikania glomerata sem saber o nome
     * cientifico: o termo e normalizado com a MESMA regra das colunas
     * "*_normalizado" e procurado no nome cientifico OU em qualquer nome
     * popular. O nome popular entra por EXISTS, e nao por join, para que a
     * consulta principal nao multiplique linhas de planta e a paginacao
     * continue correta.
     */
    private static Predicate porNomeCientificoOuPopular(
        Root<Planta> root,
        CriteriaQuery<?> query,
        CriteriaBuilder cb,
        String busca
    ) {
        String termo = "%" + NormalizadorTexto.normalizar(busca) + "%";

        Predicate porNomeCientifico = cb.like(root.get("nomeCientificoNormalizado"), termo);

        Subquery<UUID> nomesPopulares = query.subquery(UUID.class);
        Root<PlantaNomePopular> nome = nomesPopulares.from(PlantaNomePopular.class);
        nomesPopulares.select(nome.get("id")).where(
            cb.equal(nome.get("planta"), root),
            cb.like(nome.get("nomeNormalizado"), termo));

        return cb.or(porNomeCientifico, cb.exists(nomesPopulares));
    }
}
