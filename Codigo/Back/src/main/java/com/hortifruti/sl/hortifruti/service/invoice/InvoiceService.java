package com.hortifruti.sl.hortifruti.service.invoice;

import com.hortifruti.sl.hortifruti.dto.invoice.FiscalNoteXmlStorageResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.dto.invoice.OpenInvoiceResponse;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class InvoiceService {

  private final IssueInvoice issueInvoiceService;
  private final InvoiceQuery invoiceQueryService;
  private final DanfeXmlService danfeXmlService;
  private final InvoiceCancelService invoiceCancelService;
  private final FiscalNoteXmlStorageService fiscalNoteXmlStorageService;
  private final CombinedScoreService combinedScoreService;

  @Transactional
  public InvoiceResponse issueInvoice(Long combinedScoreId, String dadosAdicionais) {
    return issueInvoiceService.issueInvoice(combinedScoreId, dadosAdicionais);
  }

  @Transactional
  public InvoiceResponseGet consultInvoice(String ref) {
    return invoiceQueryService.consultInvoice(ref);
  }

  @Transactional
  public String reconcileInvoiceStatus(Long combinedScoreId) {
    return invoiceQueryService.reconcileInvoiceStatus(combinedScoreId);
  }

  @Transactional
  public ResponseEntity<Resource> downloadDanfe(String ref) {
    return danfeXmlService.downloadDanfe(ref);
  }

  @Transactional
  public ResponseEntity<Resource> downloadXml(String ref) {
    return danfeXmlService.downloadXml(ref);
  }

  @Transactional
  public String cancelInvoice(String ref, String justificativa) {
    return invoiceCancelService.cancelInvoice(ref, justificativa);
  }

  /**
   * Cancela a NF-e por referência, sem exigir um CombinedScore local vinculado — usado tanto pelo
   * fluxo normal quanto pelo cancelamento manual/avulso (com suporte a cancelamento extemporâneo).
   */
  @Transactional
  public String cancelInvoice(String ref, String justificativa, boolean extemporaneo) {
    return invoiceCancelService.cancelInvoice(ref, justificativa, extemporaneo);
  }

  @Transactional
  public List<FiscalNoteXmlStorageResponse> findFiscalNoteXmlsByPeriod(
      LocalDate startDate, LocalDate endDate) {
    return fiscalNoteXmlStorageService.findByPeriod(startDate, endDate);
  }

  public byte[] getStoredXmlContent(String ref) {
    return fiscalNoteXmlStorageService.getXmlContent(ref);
  }

  @Transactional
  public List<OpenInvoiceResponse> listOpenInvoiceOnlyScores() {
    return combinedScoreService.listOpenInvoiceOnlyScores();
  }
}
