package com.hortifruti.sl.hortifruti.service.invoice.tax.registerReport;

import com.hortifruti.sl.hortifruti.dto.invoice.tax.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.tax.ItemTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.tax.registerReport.InvoiceSummaryDetails;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceQuery;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class RegisterCalculator {

  /** Venda de mercadoria adquirida ou recebida de terceiros — ICMS interno MG (18%). */
  private static final String CFOP_VENDA_MERCADORIA_TERCEIROS = "5102";

  private static final BigDecimal ALIQUOTA_ICMS_VENDA_MERCADORIA_TERCEIROS =
      BigDecimal.valueOf(18.00);

  /**
   * Venda de mercadoria sujeita a substituição tributária, na condição de contribuinte substituído
   * — o ICMS próprio já foi retido antes na cadeia, então a alíquota aqui é 0 por desenho, não um
   * caso não mapeado.
   */
  private static final String CFOP_SUBSTITUICAO_TRIBUTARIA = "5405";

  private static final BigDecimal ALIQUOTA_ICMS_SUBSTITUICAO_TRIBUTARIA = BigDecimal.ZERO;

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
      case CFOP_VENDA_MERCADORIA_TERCEIROS -> ALIQUOTA_ICMS_VENDA_MERCADORIA_TERCEIROS;
      case CFOP_SUBSTITUICAO_TRIBUTARIA -> ALIQUOTA_ICMS_SUBSTITUICAO_TRIBUTARIA;
      default -> {
        log.warn(
            "CFOP '{}' não mapeado em RegisterCalculator.determineAliquota — usando 0 como"
                + " fallback. Verifique se é um CFOP novo que precisa de alíquota própria.",
            cfop);
        yield BigDecimal.ZERO;
      }
    };
  }

  private List<CombinedScore> fetchCombinedScores(LocalDate startDate, LocalDate endDate) {
    return combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate);
  }

  private void logProcessingError(CombinedScore combinedScore, Exception e) {
    log.error("Erro ao processar CombinedScore ID: {}", combinedScore.getId(), e);
  }
}
