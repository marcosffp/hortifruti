package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.freight.FreightConfigDTO;
import com.hortifruti.sl.hortifruti.model.FreightConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FreightConfigMapper {
  FreightConfigDTO toDTO(FreightConfig entity);

  @Mapping(target = "id", ignore = true)
  void updateEntityFromDTO(@MappingTarget FreightConfig entity, FreightConfigDTO dto);
}
