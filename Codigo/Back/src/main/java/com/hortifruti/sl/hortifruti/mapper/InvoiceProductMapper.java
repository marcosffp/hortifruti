package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceProductMapper {

  @Mapping(target = "id", source = "id")
  @Mapping(target = "code", source = "code")
  @Mapping(target = "name", source = "name")
  @Mapping(target = "price", source = "price")
  @Mapping(target = "quantity", source = "quantity")
  @Mapping(target = "unitType", source = "unitType")
  InvoiceProductResponse toResponse(InvoiceProduct invoiceProduct);
}
