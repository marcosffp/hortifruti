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

  @Mapping(target = "id", ignore = true) // ID será gerado automaticamente
  @Mapping(target = "confirmedAt", ignore = true) // Gerenciado pelo @PrePersist
  @Mapping(target = "totalValue", ignore = true) // Calculado automaticamente
  CombinedScore toEntity(CombinedScoreRequest request);

  @Mapping(target = "id", source = "id")
  @Mapping(target = "clientId", source = "clientId")
  @Mapping(target = "totalValue", source = "totalValue")
  @Mapping(target = "dueDate", source = "dueDate")
  @Mapping(target = "confirmedAt", source = "confirmedAt")
  @Mapping(target = "status", source = "status")
  @Mapping(target = "hasBillet", source = "hasBillet")
  @Mapping(target = "hasInvoice", source = "hasInvoice")
  @Mapping(target = "yourNumber", source = "yourNumber")
  CombinedScoreResponse toResponse(CombinedScore combinedScore);

  @Mapping(target = "id", ignore = true) // ID não deve ser alterado
  @Mapping(target = "confirmedAt", ignore = true) // Não deve ser alterado
  @Mapping(target = "totalValue", ignore = true) // Calculado automaticamente
  void updateEntityFromRequest(CombinedScoreRequest request, @MappingTarget CombinedScore entity);
}
