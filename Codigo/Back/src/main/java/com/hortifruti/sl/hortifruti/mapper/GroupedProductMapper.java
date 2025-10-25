package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductResponse;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupedProductMapper {

  @Mapping(source = "code", target = "code")
  @Mapping(source = "name", target = "name")
  @Mapping(source = "price", target = "price")
  @Mapping(source = "quantity", target = "quantity")
  @Mapping(source = "totalValue", target = "totalValue")
  GroupedProductResponse toResponse(GroupedProduct groupedProduct);
}