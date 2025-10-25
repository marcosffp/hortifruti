package com.hortifruti.sl.hortifruti.service.invoice;

import org.springframework.stereotype.Service;

import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class InvoiceCancelService {

    private final FocusNfeApiClient focusNfeApiClient;

    private final CombinedScoreService combinedScoreService;

    @Transactional
    public String cancelInvoice(String ref, String justificativa) {
        try {
            String response = focusNfeApiClient.cancelInvoice(ref, justificativa);
            combinedScoreService.updateStatusAfterInvoiceCancellation(ref);
            return response;
        } catch (Exception e) {
            throw new InvoiceException("Erro ao cancelar a NF-e: " + e.getMessage(), e);
        }
    }
}