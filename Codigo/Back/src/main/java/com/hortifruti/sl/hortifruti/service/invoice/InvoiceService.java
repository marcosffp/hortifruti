package com.hortifruti.sl.hortifruti.service.invoice;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import jakarta.transaction.Transactional;
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

  @Transactional
  public InvoiceResponse issueInvoice(Long combinedScoreId) {
    return issueInvoiceService.issueInvoice(combinedScoreId);
  }

  @Transactional
  public InvoiceResponseGet consultInvoice(String ref) {
    return invoiceQueryService.consultInvoice(ref);
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

  @Transactional
  public InvoiceTaxDetails printRawInvoiceJson(String ref) {
    return invoiceQueryService.printRawInvoiceJson(ref);
  }

}
