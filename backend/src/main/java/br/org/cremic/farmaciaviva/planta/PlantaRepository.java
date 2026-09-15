package br.org.cremic.farmaciaviva.planta;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantaRepository
    extends JpaRepository<Planta, UUID>, JpaSpecificationExecutor<Planta> {

    Optional<Planta> findByNomeCientificoNormalizado(String nomeCientificoNormalizado);

    Optional<Planta> findByNomeCientificoNormalizadoAndIdNot(
        String nomeCientificoNormalizado, UUID id);

    /**
     * Hidrata os nomes populares de um conjunto de plantas em UMA consulta.
     *
     * Usado pela listagem paginada. A pagina em si e obtida sem fetch join
     * (ver PlantaService.listar): fetch join de colecao combinado com
     * setMaxResults faz o Hibernate paginar EM MEMORIA (HHH90003004), o que
     * traria a tabela inteira a cada pagina. Aqui a paginacao ja aconteceu no
     * SQL; esta consulta apenas inicializa as colecoes das instancias que ja
     * estao gerenciadas no contexto de persistencia, evitando o N+1 de uma
     * consulta de nomes por linha da pagina.
     */
    @Query("select p from Planta p left join fetch p.nomesPopulares where p.id in :ids")
    List<Planta> carregarNomesPopulares(@Param("ids") Collection<UUID> ids);

    @Query("select p from Planta p left join fetch p.nomesPopulares where p.id = :id")
    Optional<Planta> buscarComNomesPopulares(@Param("id") UUID id);

    /**
     * Segunda consulta da ficha, separada da anterior de proposito: nomes
     * populares e referencias sao ambas List (bags), e o Hibernate nao permite
     * fetch join de duas bags na mesma consulta (MultipleBagFetchException).
     */
    @Query("select p from Planta p left join fetch p.referencias where p.id = :id")
    Optional<Planta> buscarComReferencias(@Param("id") UUID id);

    @Query("select p from Planta p left join fetch p.nomesPopulares "
        + "where p.ativo = true order by p.nomeCientifico")
    List<Planta> listarAtivasComNomesPopulares();
}
