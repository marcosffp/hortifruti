package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CombinedScoreMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "confirmedAt", ignore = true)
  @Mapping(target = "totalValue", ignore = true)
  CombinedScore toEntity(CombinedScoreRequest request);

  CombinedScoreResponse toResponse(CombinedScore combinedScore);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "confirmedAt", ignore = true)
  @Mapping(target = "totalValue", ignore = true)
  void updateEntityFromRequest(CombinedScoreRequest request, @MappingTarget CombinedScore entity);
}
