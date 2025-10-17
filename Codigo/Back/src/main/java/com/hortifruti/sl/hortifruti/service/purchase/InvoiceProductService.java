package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.UpdateInvoiceProduct;
import com.hortifruti.sl.hortifruti.exception.PurchaseException;
import com.hortifruti.sl.hortifruti.mapper.InvoiceProductMapper;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import com.hortifruti.sl.hortifruti.repository.purchase.InvoiceProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceProductService {

  private final InvoiceProductRepository repository;
  private final InvoiceProductMapper mapper;

  public InvoiceProductResponse updateInvoiceProduct(Long id, UpdateInvoiceProduct dto) {
    InvoiceProduct invoiceProduct =
        repository.findById(id).orElseThrow(() -> new PurchaseException("Produto não encontrado"));

    invoiceProduct.setCode(dto.code());
    invoiceProduct.setName(dto.name());
    invoiceProduct.setPrice(dto.price());
    invoiceProduct.setQuantity(dto.quantity());
    invoiceProduct.setUnitType(dto.unitType());

    InvoiceProduct updated = repository.save(invoiceProduct);

    return mapper.toResponse(updated);
  }

  public void deleteInvoiceProduct(Long id) {
    if (!repository.existsById(id)) {
      throw new PurchaseException("Produto não encontrado");
    }
    repository.deleteById(id);
  }
}
