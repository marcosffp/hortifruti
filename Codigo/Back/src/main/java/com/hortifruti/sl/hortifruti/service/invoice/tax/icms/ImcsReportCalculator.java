package com.hortifruti.sl.hortifruti.service.invoice.tax.icms;

import com.hortifruti.sl.hortifruti.dto.invoice.IcmsSalesReport;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemTaxDetails;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceQuery;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ImcsReportCalculator {
  private final InvoiceQuery invoiceQuery;
  private final CombinedScoreService combinedScoreService;

  public IcmsSalesReport generateIcmsSalesReport(LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);

    BigDecimal totalContabil = BigDecimal.ZERO;
    BigDecimal totalBaseCalculo = BigDecimal.ZERO;
    BigDecimal totalImpostoDebitado = BigDecimal.ZERO;
    BigDecimal totalIsentasOuNaoTributadas = BigDecimal.ZERO;
    BigDecimal totalOutras = BigDecimal.ZERO;

    Map<String, BigDecimal> valoresPorCfop = new HashMap<>();

    for (CombinedScore combinedScore : combinedScores) {
      processCombinedScore(
          combinedScore,
          valoresPorCfop,
          totalContabil,
          totalBaseCalculo,
          totalImpostoDebitado,
          totalIsentasOuNaoTributadas,
          totalOutras);
    }

    return buildIcmsSalesReport(
        totalContabil,
        totalBaseCalculo,
        totalImpostoDebitado,
        totalIsentasOuNaoTributadas,
        totalOutras,
        valoresPorCfop);
  }

  private void processCombinedScore(
      CombinedScore combinedScore,
      Map<String, BigDecimal> valoresPorCfop,
      BigDecimal totalContabil,
      BigDecimal totalBaseCalculo,
      BigDecimal totalImpostoDebitado,
      BigDecimal totalIsentasOuNaoTributadas,
      BigDecimal totalOutras) {
    try {
      InvoiceTaxDetails taxDetails =
          invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());

      updateTotals(
          taxDetails,
          totalContabil,
          totalBaseCalculo,
          totalImpostoDebitado,
          totalIsentasOuNaoTributadas,
          totalOutras);

      groupValuesByCfop(taxDetails, valoresPorCfop);
    } catch (Exception e) {
      handleProcessingError(combinedScore, e);
    }
  }

  private void updateTotals(
      InvoiceTaxDetails taxDetails,
      BigDecimal totalContabil,
      BigDecimal totalBaseCalculo,
      BigDecimal totalImpostoDebitado,
      BigDecimal totalIsentasOuNaoTributadas,
      BigDecimal totalOutras) {
    totalContabil = totalContabil.add(taxDetails.valorTotal());
    totalBaseCalculo = totalBaseCalculo.add(taxDetails.icmsBaseCalculo());
    totalImpostoDebitado = totalImpostoDebitado.add(taxDetails.icmsValorTotal());
    totalIsentasOuNaoTributadas =
        totalIsentasOuNaoTributadas.add(BigDecimal.ZERO); // Ajuste conforme necessário
    totalOutras = totalOutras.add(taxDetails.valorProdutos());
  }

  private void groupValuesByCfop(
      InvoiceTaxDetails taxDetails, Map<String, BigDecimal> valoresPorCfop) {
    for (ItemTaxDetails item : taxDetails.tributables()) {
      valoresPorCfop.merge(item.cfop(), item.valorBruto(), BigDecimal::add);
    }
  }

  private void handleProcessingError(CombinedScore combinedScore, Exception e) {
    e.printStackTrace();
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
