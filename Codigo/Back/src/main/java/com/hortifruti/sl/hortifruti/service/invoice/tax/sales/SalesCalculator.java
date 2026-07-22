package com.hortifruti.sl.hortifruti.service.invoice.tax.sales;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.SalesSummaryDetails;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceQuery;
import com.hortifruti.sl.hortifruti.service.purchase.ClientService;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SalesCalculator {
  private static final String CLIENTE_INDEFINIDO = "Cliente Indefinido";

  private final InvoiceQuery invoiceQuery;
  private final CombinedScoreService combinedScoreService;
  private final ClientService clientService;

  public List<SalesSummaryDetails> generateSalesSummaryDetails(
      LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);
    return combinedScores.stream()
        .map(this::processCombinedScore)
        .filter(summary -> summary != null)
        .collect(Collectors.toList());
  }

  private List<CombinedScore> fetchCombinedScores(LocalDate startDate, LocalDate endDate) {
    return combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate);
  }

  private SalesSummaryDetails processCombinedScore(CombinedScore combinedScore) {
    try {
      InvoiceTaxDetails taxDetails =
          invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());
      String clientName = resolveClientName(combinedScore.getClientId());
      return createSalesSummaryDetails(taxDetails, clientName);
    } catch (Exception e) {
      logProcessingError(combinedScore, e);
      return null;
    }
  }

  private String resolveClientName(Long clientId) {
    if (clientId == null) {
      return CLIENTE_INDEFINIDO;
    }
    return clientService.findClientName(clientId).orElse(CLIENTE_INDEFINIDO);
  }

  private SalesSummaryDetails createSalesSummaryDetails(
      InvoiceTaxDetails taxDetails, String clientName) {
    return new SalesSummaryDetails(
        taxDetails.numero(),
        "55",
        taxDetails.dataEmissao().toLocalDate().toString(),
        taxDetails.dataEmissao().toLocalDate().toString(),
        clientName,
        taxDetails.valorProdutos(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        taxDetails.valorTotal());
  }

  private void logProcessingError(CombinedScore combinedScore, Exception e) {
    log.error("Erro ao processar CombinedScore ID: {}", combinedScore.getId(), e);
  }
}
