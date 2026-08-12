package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.UpdateInvoiceProduct;
import com.hortifruti.sl.hortifruti.exception.purchase.PurchaseException;
import com.hortifruti.sl.hortifruti.mapper.InvoiceProductMapper;
import com.hortifruti.sl.hortifruti.model.purchase.InvoiceProduct;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.repository.purchase.InvoiceProductRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class InvoiceProductService {

  private final InvoiceProductRepository repository;
  private final InvoiceProductMapper mapper;
  private final PurchaseService purchaseService;

  @Transactional
  public InvoiceProductResponse updateInvoiceProduct(Long id, UpdateInvoiceProduct dto) {
    InvoiceProduct invoiceProduct =
        repository.findById(id).orElseThrow(() -> new PurchaseException("Produto não encontrado"));

    invoiceProduct.setCode(dto.code());
    invoiceProduct.setName(dto.name());
    invoiceProduct.setPrice(dto.price());
    invoiceProduct.setQuantity(dto.quantity());
    invoiceProduct.setUnitType(dto.unitType());

    InvoiceProduct updated = repository.save(invoiceProduct);
    purchaseService.recalculateTotal(updated.getPurchase().getId());

    return mapper.toResponse(updated);
  }

  @Transactional
  public void deleteInvoiceProduct(Long id) {
    InvoiceProduct invoiceProduct =
        repository.findById(id).orElseThrow(() -> new PurchaseException("Produto não encontrado"));
    Purchase purchase = invoiceProduct.getPurchase();
    repository.deleteById(id);
    purchaseService.recalculateTotal(purchase.getId());
  }

  @Transactional(readOnly = true)
  public List<InvoiceProduct> findAll() {
    return repository.findAll();
  }

  @Transactional
  public void deleteAllByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
    repository.deleteByCreatedAtBetween(startDate, endDate);
  }
}
