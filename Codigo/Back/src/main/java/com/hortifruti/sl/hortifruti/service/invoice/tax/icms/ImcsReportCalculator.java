package com.hortifruti.sl.hortifruti.service.invoice.tax.icms;

import com.hortifruti.sl.hortifruti.dto.invoice.tax.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.tax.ItemTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.tax.icms.IcmsSalesReport;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceQuery;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ImcsReportCalculator {
  private final InvoiceQuery invoiceQuery;
  private final CombinedScoreService combinedScoreService;

  public IcmsSalesReport generateIcmsSalesReport(LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);

    Totals totals = new Totals();
    Map<String, BigDecimal> valoresPorCfop = new HashMap<>();

    for (CombinedScore combinedScore : combinedScores) {
      processCombinedScore(combinedScore, valoresPorCfop, totals);
    }

    return buildIcmsSalesReport(
        totals.contabil,
        totals.baseCalculo,
        totals.impostoDebitado,
        totals.isentasOuNaoTributadas,
        totals.outras,
        valoresPorCfop);
  }

  private void processCombinedScore(
      CombinedScore combinedScore, Map<String, BigDecimal> valoresPorCfop, Totals totals) {
    try {
      InvoiceTaxDetails taxDetails =
          invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());

      updateTotals(taxDetails, totals);

      groupValuesByCfop(taxDetails, valoresPorCfop);
    } catch (Exception e) {
      handleProcessingError(combinedScore, e);
    }
  }

  private void updateTotals(InvoiceTaxDetails taxDetails, Totals totals) {
    totals.contabil = totals.contabil.add(taxDetails.valorTotal());
    totals.baseCalculo = totals.baseCalculo.add(taxDetails.icmsBaseCalculo());
    totals.impostoDebitado = totals.impostoDebitado.add(taxDetails.icmsValorTotal());
    totals.isentasOuNaoTributadas = totals.isentasOuNaoTributadas.add(BigDecimal.ZERO);
    totals.outras = totals.outras.add(taxDetails.valorProdutos());
  }

  private static final class Totals {
    private BigDecimal contabil = BigDecimal.ZERO;
    private BigDecimal baseCalculo = BigDecimal.ZERO;
    private BigDecimal impostoDebitado = BigDecimal.ZERO;
    private BigDecimal isentasOuNaoTributadas = BigDecimal.ZERO;
    private BigDecimal outras = BigDecimal.ZERO;
  }

  private void groupValuesByCfop(
      InvoiceTaxDetails taxDetails, Map<String, BigDecimal> valoresPorCfop) {
    for (ItemTaxDetails item : taxDetails.tributables()) {
      valoresPorCfop.merge(item.cfop(), item.valorBruto(), BigDecimal::add);
    }
  }

  private void handleProcessingError(CombinedScore combinedScore, Exception e) {
    log.error("Erro ao processar CombinedScore ID: {}", combinedScore.getId(), e);
  }

  private IcmsSalesReport buildIcmsSalesReport(
      BigDecimal totalContabil,
      BigDecimal totalBaseCalculo,
      BigDecimal totalImpostoDebitado,
      BigDecimal totalIsentasOuNaoTributadas,
      BigDecimal totalOutras,
      Map<String, BigDecimal> valoresPorCfop) {
    return new IcmsSalesReport(
        totalContabil,
        totalBaseCalculo,
        totalImpostoDebitado,
        totalIsentasOuNaoTributadas,
        totalOutras,
        valoresPorCfop);
  }

  private List<CombinedScore> fetchCombinedScores(LocalDate startDate, LocalDate endDate) {
    return combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate);
  }
}
