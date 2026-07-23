package com.hortifruti.sl.hortifruti.controller.invoice;

import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceWithBilletResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.OpenInvoiceResponse;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceService;
import com.hortifruti.sl.hortifruti.service.invoice.IssueInvoiceWithBilletService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

  private final InvoiceService invoiceService;
  private final IssueInvoiceWithBilletService issueInvoiceWithBilletService;

  @PostMapping("/issue/{combinedScoreId}")
  public ResponseEntity<InvoiceResponse> issueInvoice(
      @PathVariable Long combinedScoreId,
      @RequestParam(value = "dadosAdicionais", required = false, defaultValue = "")
          String dadosAdicionais) {
    InvoiceResponse response = invoiceService.issueInvoice(combinedScoreId, dadosAdicionais);
    return ResponseEntity.ok(response);
  }

  /**
   * Emite a NF e, em seguida, o boleto vinculado a ela (usando o número da NF como identificador do
   * boleto). Se qualquer etapa após a emissão da NF falhar, a NF é cancelada automaticamente.
   * Retorna a NF (DANFE + XML) e o boleto, todos em base64.
   */
  @PostMapping("/issue-with-billet/{combinedScoreId}")
  public ResponseEntity<InvoiceWithBilletResponse> issueInvoiceWithBillet(
      @PathVariable Long combinedScoreId,
      @RequestParam(value = "dadosAdicionais", required = false, defaultValue = "")
          String dadosAdicionais,
      @RequestParam(required = false) String dueDate) {
    InvoiceWithBilletResponse response =
        issueInvoiceWithBilletService.issueInvoiceAndBillet(
            combinedScoreId, dadosAdicionais, dueDate);
    return ResponseEntity.ok(response);
  }

  /**
   * Lista agrupamentos que só têm nota fiscal emitida (sem boleto), ainda pendentes de confirmação
   * manual de pagamento — usado pela aba "NF sem Boleto" da tela de Boletos.
   */
  @GetMapping("/open")
  public ResponseEntity<List<OpenInvoiceResponse>> listOpenInvoiceOnly() {
    return ResponseEntity.ok(invoiceService.listOpenInvoiceOnlyScores());
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
      log.error("Erro ao cancelar a NF-e para ref {}", ref, e);
      return ResponseEntity.internalServerError().body("Erro ao cancelar a NF-e.");
    }
  }

  @GetMapping("/xml-storage")
  public ResponseEntity<List<FiscalNoteXmlStorageResponse>> getFiscalNoteXmlsByPeriod(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<FiscalNoteXmlStorageResponse> result =
        invoiceService.findFiscalNoteXmlsByPeriod(startDate, endDate);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/xml-storage/{ref}/download")
  public ResponseEntity<Resource> downloadStoredXml(@PathVariable String ref) {
    byte[] xmlBytes = invoiceService.getStoredXmlContent(ref);
    Resource resource = new ByteArrayResource(xmlBytes);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_XML)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nota-fiscal-" + ref + ".xml\"")
        .body(resource);
  }
}
