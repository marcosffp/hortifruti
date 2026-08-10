package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductResponse;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupedProductMapper {

  GroupedProductResponse toResponse(GroupedProduct groupedProduct);
}
