package com.hortifruti.sl.hortifruti.controller.invoice;

import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceWithBilletResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.OpenInvoiceResponse;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceCancelService;
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
import org.springframework.security.access.prepost.PreAuthorize;
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

  /**
   * Corrige agrupamentos com {@code hasInvoice=true} indevido, deixados por notas que a Sefaz
   * rejeitou antes da checagem de status ter sido adicionada em {@link
   * com.hortifruti.sl.hortifruti.service.invoice.IssueInvoice}. Consulta o status atual na Focus
   * NFe e, se for erro/denegado/cancelado, libera o agrupamento para nova emissão.
   *
   * <p>Sem restrição de role (diferente de {@code /xml-storage}): é chamado automaticamente pelo
   * front sempre que "Ver NF" encontra uma nota rejeitada, então precisa estar disponível para
   * qualquer usuário autenticado, não só MANAGER.
   */
  @PostMapping("/{combinedScoreId}/reconciliar")
  public ResponseEntity<String> reconcileInvoice(@PathVariable Long combinedScoreId) {
    return ResponseEntity.ok(invoiceService.reconcileInvoiceStatus(combinedScoreId));
  }

  @GetMapping("/{ref}/danfe")
  public ResponseEntity<Resource> downloadDanfe(@PathVariable String ref) {
    return invoiceService.downloadDanfe(ref);
  }

  @GetMapping("/{ref}/xml/download")
  public ResponseEntity<Resource> downloadXml(@PathVariable String ref) {
    return invoiceService.downloadXml(ref);
  }

  /**
   * Cancela a NF-e pela referência. Funciona mesmo sem um CombinedScore local vinculado a essa ref
   * (ex: cancelamento manual/avulso) — se existir um agrupamento local, seu status também é
   * atualizado, mas isso não é obrigatório para a operação ter sucesso.
   *
   * <p>A justificativa não é mais informada pelo cliente: todo cancelamento disparado pela UI é
   * tratado como extemporâneo (fora do prazo normal de ~24h da SEFAZ, autorização excepcional já
   * obtida pelo operador junto à SEFAZ do estado) e usa sempre o texto fixo {@link
   * InvoiceCancelService#MANUAL_CANCEL_JUSTIFICATIVA}.
   */
  @DeleteMapping("/{ref}/cancel")
  public ResponseEntity<String> cancelInvoice(@PathVariable String ref) {
    try {
      String response =
          invoiceService.cancelInvoice(ref, InvoiceCancelService.MANUAL_CANCEL_JUSTIFICATIVA, true);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Erro ao cancelar a NF-e para ref {}", ref, e);
      return ResponseEntity.internalServerError()
          .body(e.getMessage() != null ? e.getMessage() : "Erro ao cancelar a NF-e.");
    }
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/xml-storage")
  public ResponseEntity<List<FiscalNoteXmlStorageResponse>> getFiscalNoteXmlsByPeriod(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    List<FiscalNoteXmlStorageResponse> result =
        invoiceService.findFiscalNoteXmlsByPeriod(startDate, endDate);
    return ResponseEntity.ok(result);
  }

  @PreAuthorize("hasRole('MANAGER')")
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
