package com.hortifruti.sl.hortifruti.service.invoice.tax.payment;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceQuery;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentCalculator {
  private final InvoiceQuery invoiceQuery;
  private final CombinedScoreService combinedScoreService;

  public Map<String, BigDecimal> generateBankSettlementTotals(
      LocalDate startDate, LocalDate endDate) {
    String bankSettlement = "Liquidação Bancária";
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);

    return combinedScores.stream()
        .map(this::extractInvoiceTotal)
        .filter(this::isValidTotal)
        .collect(Collectors.toMap(key -> bankSettlement, value -> value, BigDecimal::add));
  }

  private List<CombinedScore> fetchCombinedScores(LocalDate startDate, LocalDate endDate) {
    return combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate);
  }

  private BigDecimal extractInvoiceTotal(CombinedScore combinedScore) {
    try {
      InvoiceTaxDetails taxDetails =
          invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());
      return taxDetails.valorTotal();
    } catch (Exception e) {
      logProcessingError(combinedScore, e);
      return BigDecimal.ZERO;
    }
  }

  private boolean isValidTotal(BigDecimal totalValue) {
    return totalValue != null && totalValue.compareTo(BigDecimal.ZERO) > 0;
  }

  private void logProcessingError(CombinedScore combinedScore, Exception e) {
    System.err.println("Erro ao processar CombinedScore ID: " + combinedScore.getId());
    e.printStackTrace();
  }
}
