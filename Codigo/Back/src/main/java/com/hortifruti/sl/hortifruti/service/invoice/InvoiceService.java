package com.hortifruti.sl.hortifruti.service.invoice;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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
        try {
            return danfeXmlService.downloadDanfe(ref);
        } catch (Exception e) {
            throw new InvoiceException("Erro ao fazer download do DANFE", e);
        }
    }

    @Transactional
    public ResponseEntity<Resource> downloadXml(String ref) {
        try {
            return danfeXmlService.downloadXml(ref);
        } catch (Exception e) {
            throw new InvoiceException("Erro ao fazer download do XML", e);
        }
    }

    @Transactional
    public String cancelInvoice(String ref, String justificativa) {
        return invoiceCancelService.cancelInvoice(ref, justificativa);
    }
}