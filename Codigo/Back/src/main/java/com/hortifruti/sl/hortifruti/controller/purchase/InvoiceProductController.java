package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.UpdateInvoiceProduct;
import com.hortifruti.sl.hortifruti.service.purchase.InvoiceProductService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoice-products")
@AllArgsConstructor
public class InvoiceProductController {

  private final InvoiceProductService service;

  @PreAuthorize("hasRole('MANAGER')")
  @PutMapping("/{id}")
  public ResponseEntity<InvoiceProductResponse> updateInvoiceProduct(
      @PathVariable Long id, @RequestBody UpdateInvoiceProduct dto) {
    InvoiceProductResponse updated = service.updateInvoiceProduct(id, dto);
    return ResponseEntity.ok(updated);
  }

  @PreAuthorize("hasRole('MANAGER')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteInvoiceProduct(@PathVariable Long id) {
    service.deleteInvoiceProduct(id);
    return ResponseEntity.noContent().build();
  }
}
