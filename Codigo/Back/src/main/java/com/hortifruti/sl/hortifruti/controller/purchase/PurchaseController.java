package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseResponse;
import com.hortifruti.sl.hortifruti.service.purchase.PurchaseService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/purchases")
@AllArgsConstructor
public class PurchaseController {

  private final PurchaseService purchaseService;

  @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> processPurchase(@RequestParam("file") MultipartFile file) {
    try {
      purchaseService.processPurchaseFile(file);
      return ResponseEntity.ok(Map.of("message", "Compra processada com sucesso"));
    } catch (IOException e) {
      return ResponseEntity.status(500).body(Map.of("error", "Erro ao processar o arquivo: " + e.getMessage()));
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deletePurchase(@PathVariable Long id) {
    purchaseService.deletePurchaseById(id);
    return ResponseEntity.ok(Map.of("message", "Compra deletada com sucesso"));
  }

  @GetMapping("/client/{clientId}/ordered")
  public ResponseEntity<Page<PurchaseResponse>> getPurchasesByClientOrdered(
      @PathVariable Long clientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<PurchaseResponse> purchases = purchaseService.getPurchasesByClientOrdered(clientId, pageable);
    return ResponseEntity.ok(purchases);
  }

  @GetMapping("/{id}/products")
  public ResponseEntity<List<InvoiceProductResponse>> getInvoiceProductsByPurchaseId(
      @PathVariable Long id) {
    List<InvoiceProductResponse> products = purchaseService.getInvoiceProductsByPurchaseId(id);
    return ResponseEntity.ok(products);
  }

  @GetMapping("/date-range")
  public ResponseEntity<Page<PurchaseResponse>> getPurchasesByDateRange(
      @RequestParam("startDate") String startDate,
      @RequestParam("endDate") String endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Page<PurchaseResponse> purchases = purchaseService.getPurchasesByDateRange(startDate, endDate, page, size);
    return ResponseEntity.ok(purchases);
  }
}
