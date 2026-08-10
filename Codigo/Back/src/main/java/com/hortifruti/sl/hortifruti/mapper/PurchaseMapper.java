package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseResponse;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseMapper {

  PurchaseResponse toResponse(Purchase purchase);
}
