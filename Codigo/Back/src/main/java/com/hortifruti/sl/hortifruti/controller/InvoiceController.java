package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

  private final InvoiceService invoiceService;

  @PostMapping("/issue/{combinedScoreId}")
  public ResponseEntity<InvoiceResponse> issueInvoice(@PathVariable Long combinedScoreId) {
    InvoiceResponse response = invoiceService.issueInvoice(combinedScoreId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/consulta/{ref}")
  public ResponseEntity<InvoiceResponseGet> consultInvoice(@PathVariable String ref) {
    InvoiceResponseGet response = invoiceService.consultInvoice(ref);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{ref}/danfe")
  public ResponseEntity<Resource> downloadDanfe(@PathVariable String ref) {
    return invoiceService.downloadDanfe(ref);
  }

  @GetMapping("/{ref}/xml/download")
  public ResponseEntity<Resource> downloadXml(@PathVariable String ref) {
    return invoiceService.downloadXml(ref);
  }

  @DeleteMapping("/{ref}/cancel")
  public ResponseEntity<String> cancelInvoice(
      @PathVariable String ref, @RequestParam String justificativa) {
    try {
      String response = invoiceService.cancelInvoice(ref, justificativa);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Erro ao cancelar a NF-e para ref {}: {}", ref, e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body("Erro ao cancelar a NF-e: " + e.getMessage());
    }
  }
}
