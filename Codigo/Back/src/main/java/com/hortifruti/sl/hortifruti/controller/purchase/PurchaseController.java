package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.InvoiceProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseResponse;
import com.hortifruti.sl.hortifruti.service.purchase.PurchaseService;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/purchases")
@AllArgsConstructor
public class PurchaseController {

  private static final Logger logger = LoggerFactory.getLogger(PurchaseController.class);

  private final PurchaseService purchaseService;

  @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> processPurchase(@RequestParam("file") MultipartFile file) {
    try {
      purchaseService.processPurchaseFile(file);
      return ResponseEntity.ok(Map.of("message", "Compra processada com sucesso"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Falha ao processar a compra"));
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deletePurchase(@PathVariable Long id) {
    try {
      purchaseService.deletePurchaseById(id);
      return ResponseEntity.ok(Map.of("message", "Compra deletada com sucesso"));
    } catch (Exception e) {
      logger.error("Erro ao deletar compra: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Falha ao deletar a compra"));
    }
  }

  @GetMapping("/client/{clientId}/ordered")
  public ResponseEntity<Page<PurchaseResponse>> getPurchasesByClientOrdered(
      @PathVariable Long clientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size);
      Page<PurchaseResponse> purchases =
          purchaseService.getPurchasesByClientOrdered(clientId, pageable);
      return ResponseEntity.ok(purchases);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/{id}/products")
  public ResponseEntity<List<InvoiceProductResponse>> getInvoiceProductsByPurchaseId(
      @PathVariable Long id) {
    try {
      List<InvoiceProductResponse> products = purchaseService.getInvoiceProductsByPurchaseId(id);
      return ResponseEntity.ok(products);
    } catch (Exception e) {
      logger.error("Erro ao buscar produtos da compra: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
