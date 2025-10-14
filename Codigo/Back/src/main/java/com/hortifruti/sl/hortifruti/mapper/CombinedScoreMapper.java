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

  @Mapping(target = "id", ignore = true) // ID será gerado automaticamente
  @Mapping(target = "confirmedAt", ignore = true) // Gerenciado pelo @PrePersist
  @Mapping(target = "totalValue", ignore = true) // Calculado automaticamente
  CombinedScore toEntity(CombinedScoreRequest request);

  CombinedScoreResponse toResponse(CombinedScore combinedScore);

  @Mapping(target = "id", ignore = true) // ID não deve ser alterado
  @Mapping(target = "confirmedAt", ignore = true) // Não deve ser alterado
  @Mapping(target = "totalValue", ignore = true) // Calculado automaticamente
  void updateEntityFromRequest(CombinedScoreRequest request, @MappingTarget CombinedScore entity);
}
