package br.org.cremic.farmaciaviva.planta;

import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlantaMapper {

    @Mapping(target = "id", source = "planta.id")
    @Mapping(target = "similares", source = "similares")
    PlantaResponse toResponse(Planta planta, List<PlantaResponse.SimilarResponse> similares);

    PlantaResponse.NomePopularResponse toNomePopularResponse(PlantaNomePopular nomePopular);

    PlantaResponse.ReferenciaResponse toReferenciaResponse(PlantaReferencia referencia);

    @Mapping(target = "id", source = "plantaSimilar.id")
    @Mapping(target = "nomeCientifico", source = "plantaSimilar.nomeCientifico")
    @Mapping(target = "ativo", source = "plantaSimilar.ativo")
    @Mapping(target = "nomePopularPrincipal",
        expression = "java(nomePopularPrincipal(vinculo.getPlantaSimilar()))")
    PlantaResponse.SimilarResponse toSimilarResponse(PlantaSimilar vinculo);

    @Mapping(target = "id", source = "planta.id")
    @Mapping(target = "nomesPopulares", expression = "java(nomesPopularesPrincipalPrimeiro(planta))")
    @Mapping(target = "quantidadeSimilares", source = "quantidadeSimilares")
    PlantaResumoResponse toResumo(Planta planta, Long quantidadeSimilares);

    @Mapping(target = "nomePopularPrincipal", expression = "java(nomePopularPrincipal(planta))")
    PlantaOpcaoResponse toOpcao(Planta planta);

    /**
     * O nome popular principal, ou o primeiro da lista quando nenhum foi
     * marcado, ou null quando a planta nao tem nomes populares.
     */
    default String nomePopularPrincipal(Planta planta) {
        List<PlantaNomePopular> nomes = planta.getNomesPopulares();
        return nomes.stream()
            .filter(PlantaNomePopular::isPrincipal)
            .findFirst()
            .or(() -> nomes.stream().findFirst())
            .map(PlantaNomePopular::getNome)
            .orElse(null);
    }

    default List<String> nomesPopularesPrincipalPrimeiro(Planta planta) {
        return planta.getNomesPopulares().stream()
            .sorted(Comparator
                .comparing(PlantaNomePopular::isPrincipal).reversed()
                .thenComparingInt(PlantaNomePopular::getOrdem))
            .map(PlantaNomePopular::getNome)
            .toList();
    }
}
