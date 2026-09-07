package br.org.cremic.farmaciaviva.unidademedida;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UnidadeMedidaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    UnidadeMedida toEntity(UnidadeMedidaRequest request);

    UnidadeMedidaResponse toResponse(UnidadeMedida entidade);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    void updateEntity(UnidadeMedidaUpdateRequest request, @MappingTarget UnidadeMedida entidade);
}
