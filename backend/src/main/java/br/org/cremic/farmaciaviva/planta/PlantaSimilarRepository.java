package br.org.cremic.farmaciaviva.planta;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantaSimilarRepository extends JpaRepository<PlantaSimilar, UUID> {

    boolean existsByPlantaIdAndPlantaSimilarId(UUID plantaId, UUID plantaSimilarId);

    long deleteByPlantaIdAndPlantaSimilarId(UUID plantaId, UUID plantaSimilarId);

    /**
     * Vinculos de uma planta com a outra ponta ja carregada, incluindo os
     * nomes populares dela, em UMA consulta: a ficha mostra o primeiro nome
     * popular de cada similar, e sem o fetch join isso seria um N+1.
     */
    @Query("select s from PlantaSimilar s "
        + "join fetch s.plantaSimilar ps "
        + "left join fetch ps.nomesPopulares "
        + "where s.planta.id = :plantaId "
        + "order by ps.nomeCientifico")
    List<PlantaSimilar> listarPorPlanta(@Param("plantaId") UUID plantaId);

    @Query("select new br.org.cremic.farmaciaviva.planta.ContagemSimilares(s.planta.id, count(s)) "
        + "from PlantaSimilar s where s.planta.id in :ids group by s.planta.id")
    List<ContagemSimilares> contarPorPlanta(@Param("ids") Collection<UUID> ids);
}
