package com.hortifruti.sl.hortifruti.service.invoice.tax.registerReport;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceSummaryDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemTaxDetails;
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
public class RegisterCalculator {
  private final InvoiceQuery invoiceQuery;
  private final CombinedScoreService combinedScoreService;

  public List<InvoiceSummaryDetails> generateInvoiceSummaryDetails(
      LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);

    return combinedScores.stream()
        .map(this::processCombinedScore)
        .filter(summary -> summary != null)
        .collect(Collectors.toList());
  }

  private InvoiceSummaryDetails processCombinedScore(CombinedScore combinedScore) {
    try {
      InvoiceTaxDetails taxDetails =
          invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());
      return createInvoiceSummaryDetails(taxDetails);
    } catch (Exception e) {
      logProcessingError(combinedScore, e);
      return null;
    }
  }

  private InvoiceSummaryDetails createInvoiceSummaryDetails(InvoiceTaxDetails taxDetails) {
    String especie = "NF-e";
    String serie = "1";
    String dia = String.valueOf(taxDetails.dataEmissao().toLocalDate().getDayOfMonth());
    String uf = "MG";
    BigDecimal valor = taxDetails.valorTotal();

    String predominante = determinePredominantCfop(taxDetails.tributables());
    BigDecimal aliquota = determineAliquota(predominante);

    return new InvoiceSummaryDetails(especie, serie, dia, uf, valor, predominante, aliquota);
  }

  private String determinePredominantCfop(List<ItemTaxDetails> items) {
    Map<String, Long> cfopCounts =
        items.stream().collect(Collectors.groupingBy(ItemTaxDetails::cfop, Collectors.counting()));

    return cfopCounts.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .filter(entry -> entry.getValue() >= items.size() * 0.8)
        .map(Map.Entry::getKey)
        .orElse("Indefinido");
  }

  private BigDecimal determineAliquota(String cfop) {
    return switch (cfop) {
      case "5102" -> BigDecimal.valueOf(18.00);
      case "5405" -> BigDecimal.ZERO;
      default -> BigDecimal.ZERO;
    };
  }

  private List<CombinedScore> fetchCombinedScores(LocalDate startDate, LocalDate endDate) {
    return combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate);
  }

  private void logProcessingError(CombinedScore combinedScore, Exception e) {
    System.err.println("Erro ao processar CombinedScore ID: " + combinedScore.getId());
    e.printStackTrace();
  }
}
