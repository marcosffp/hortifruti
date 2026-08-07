package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.ManualPurchaseItemRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.ManualPurchaseRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseResponse;
import com.hortifruti.sl.hortifruti.model.purchase.Purchase;
import com.hortifruti.sl.hortifruti.service.purchase.PurchaseService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/purchases")
@AllArgsConstructor
public class PurchaseController {

  private final PurchaseService purchaseService;

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, String>> processPurchase(
      @RequestParam("file") MultipartFile file) throws IOException {
    purchaseService.processPurchaseFile(file);
    return ResponseEntity.ok(Map.of("message", "Compra processada com sucesso"));
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/manual", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PurchaseResponse> createManualPurchase(
      @Valid @RequestBody ManualPurchaseRequest request) {
    Purchase purchase = purchaseService.createManualPurchase(request);
    return ResponseEntity.ok(
        new PurchaseResponse(
            purchase.getId(),
            purchase.getPurchaseDate(),
            purchase.getTotal(),
            purchase.getUpdatedAt()));
  }

  @PreAuthorize("hasRole('MANAGER')")
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
    Page<PurchaseResponse> purchases =
        purchaseService.getPurchasesByClientOrdered(clientId, pageable);
    return ResponseEntity.ok(purchases);
  }

  @GetMapping("/{id}/products")
  public ResponseEntity<List<InvoiceProductResponse>> getInvoiceProductsByPurchaseId(
      @PathVariable Long id) {
    List<InvoiceProductResponse> products = purchaseService.getInvoiceProductsByPurchaseId(id);
    return ResponseEntity.ok(products);
  }

  /** Foto de comprovante da compra (ver {@code Client#requiresPurchaseProof}). */
  @GetMapping("/{id}/imagem")
  public ResponseEntity<byte[]> getImagem(@PathVariable Long id) {
    PurchaseService.ImagemCompra imagem = purchaseService.buscarImagem(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(imagem.contentType()))
        .body(imagem.bytes());
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping(value = "/{id}/products", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InvoiceProductResponse> addInvoiceProduct(
      @PathVariable Long id, @Valid @RequestBody ManualPurchaseItemRequest item) {
    InvoiceProductResponse created = purchaseService.addInvoiceProduct(id, item);
    return ResponseEntity.ok(created);
  }

  @GetMapping("/date-range")
  public ResponseEntity<Page<PurchaseResponse>> getPurchasesByDateRange(
      @RequestParam("startDate") String startDate,
      @RequestParam("endDate") String endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Page<PurchaseResponse> purchases =
        purchaseService.getPurchasesByDateRange(startDate, endDate, page, size);
    return ResponseEntity.ok(purchases);
  }
}
