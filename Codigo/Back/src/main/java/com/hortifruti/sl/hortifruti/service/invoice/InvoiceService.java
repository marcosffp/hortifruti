package com.hortifruti.sl.hortifruti.service.invoice;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import jakarta.transaction.Transactional;
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

  /**
   * Lista todas as referências de notas fiscais para um CPF/CNPJ
   * 
   * @param cpfCnpj CPF ou CNPJ do cliente (apenas números)
   * @return Lista de referências das notas fiscais autorizadas
   */
  @Transactional
  public List<String> listInvoiceRefsByDocument(String cpfCnpj) {
    return invoiceQueryService.listInvoiceRefsByDocument(cpfCnpj);
  }
}
