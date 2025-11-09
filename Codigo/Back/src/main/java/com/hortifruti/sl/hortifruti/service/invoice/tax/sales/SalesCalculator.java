package com.hortifruti.sl.hortifruti.service.invoice.tax.sales;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.SalesSummaryDetails;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceQuery;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SalesCalculator {
  private final InvoiceQuery invoiceQuery;
  private final CombinedScoreService combinedScoreService;

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
      return createSalesSummaryDetails(taxDetails);
    } catch (Exception e) {
      logProcessingError(combinedScore, e);
      return null;
    }
  }

  private SalesSummaryDetails createSalesSummaryDetails(InvoiceTaxDetails taxDetails) {
    return new SalesSummaryDetails(
        taxDetails.numero(),
        "55",
        taxDetails.dataEmissao().toLocalDate().toString(),
        taxDetails.dataEmissao().toLocalDate().toString(),
        "Cliente Indefinido",
        taxDetails.valorProdutos(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        taxDetails.valorTotal());
  }

  /**
   * Registra um erro ao processar um CombinedScore.
   *
   * @param combinedScore CombinedScore que causou o erro.
   * @param e Exceção lançada.
   */
  private void logProcessingError(CombinedScore combinedScore, Exception e) {
    System.err.println("Erro ao processar CombinedScore ID: " + combinedScore.getId());
    e.printStackTrace();
  }
}
