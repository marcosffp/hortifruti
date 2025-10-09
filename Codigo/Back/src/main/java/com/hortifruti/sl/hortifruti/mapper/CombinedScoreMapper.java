package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.model.CombinedScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CombinedScoreMapper {

  CombinedScoreResponse toResponse(CombinedScore entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "confirmedAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "dueDate", ignore = true)
  void updateEntityFromRequest(@MappingTarget CombinedScore entity, CombinedScoreRequest dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "confirmedAt", expression = "java(java.time.LocalDateTime.now())")
  @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
  @Mapping(target = "dueDate", expression = "java(java.time.LocalDateTime.now().plusDays(20))")
  CombinedScore toEntity(CombinedScoreRequest dto);
}
