package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseResponse;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseMapper {

  @Mapping(source = "id", target = "id")
  @Mapping(source = "purchaseDate", target = "purchaseDate")
  @Mapping(source = "total", target = "total")
  @Mapping(source = "updatedAt", target = "updatedAt")
  PurchaseResponse toResponse(Purchase purchase);
}
