package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceProductMapper {

  InvoiceProductResponse toResponse(InvoiceProduct invoiceProduct);
}
