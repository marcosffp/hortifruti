package com.hortifruti.sl.hortifruti.service.invoice.tax;

import com.hortifruti.sl.hortifruti.dto.invoice.IcmsSalesReport;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceSummaryDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.SalesSummaryDetails;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.DanfeXmlService;
import com.hortifruti.sl.hortifruti.service.invoice.InvoiceQuery;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ImcsReportGenerator {
  private final InvoiceQuery invoiceQuery;
  private final CombinedScoreService combinedScoreService;
  private final DanfeXmlService danfeXmlService;

  public List<String> generateXmlFileList(LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);
    List<String> invoiceRefs = extractInvoiceRefs(combinedScores);

    // Buscar os caminhos XML usando o DanfeXmlService
    return danfeXmlService.getXmlPathsForPeriod(invoiceRefs);
  }

  public IcmsSalesReport generateIcmsSalesReport(LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);

    BigDecimal totalContabil = BigDecimal.ZERO;
    BigDecimal totalBaseCalculo = BigDecimal.ZERO;
    BigDecimal totalImpostoDebitado = BigDecimal.ZERO;
    BigDecimal totalIsentasOuNaoTributadas = BigDecimal.ZERO;
    BigDecimal totalOutras = BigDecimal.ZERO;

    Map<String, BigDecimal> valoresPorCfop = new HashMap<>();

    // Iterar sobre os CombinedScores e extrair os detalhes fiscais
    for (CombinedScore combinedScore : combinedScores) {
      try {
        InvoiceTaxDetails taxDetails =
            invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());

        // Atualizar totais
        totalContabil = totalContabil.add(taxDetails.valorTotal());
        totalBaseCalculo = totalBaseCalculo.add(taxDetails.icmsBaseCalculo());
        totalImpostoDebitado = totalImpostoDebitado.add(taxDetails.icmsValorTotal());

        // Agrupar valores por CFOP
        for (ItemTaxDetails item : taxDetails.tributables()) {
          valoresPorCfop.merge(item.cfop(), item.valorBruto(), BigDecimal::add);
        }

        // Atualizar totais de isentas ou não tributadas e outras
        totalIsentasOuNaoTributadas =
            totalIsentasOuNaoTributadas.add(BigDecimal.ZERO); // Ajuste conforme necessário
        totalOutras = totalOutras.add(taxDetails.valorProdutos());

        // Ajuste conforme necessário
      } catch (Exception e) {
        System.err.println("Erro ao processar CombinedScore ID: " + combinedScore.getId());
        e.printStackTrace();
      }
    }

    // Retornar o record consolidado
    return new IcmsSalesReport(
        totalContabil,
        totalBaseCalculo,
        totalImpostoDebitado,
        totalIsentasOuNaoTributadas,
        totalOutras,
        valoresPorCfop);
  }

  public List<InvoiceSummaryDetails> generateInvoiceSummaryDetails(
      LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);

    // Mapear os CombinedScores para InvoiceSummaryDetails
    return combinedScores.stream()
        .map(
            combinedScore -> {
              try {
                InvoiceTaxDetails taxDetails =
                    invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());
                return createInvoiceSummaryDetails(taxDetails);
              } catch (Exception e) {
                System.err.println("Erro ao processar CombinedScore ID: " + combinedScore.getId());
                e.printStackTrace();
                return null; // Ignorar registros com erro
              }
            })
        .filter(summary -> summary != null) // Remover nulos
        .collect(Collectors.toList());
  }

  private InvoiceSummaryDetails createInvoiceSummaryDetails(InvoiceTaxDetails taxDetails) {
    String especie = "NF-e";
    String serie = "1";
    String dia = String.valueOf(taxDetails.dataEmissao().toLocalDate().getDayOfMonth());
    String uf = "MG";
    BigDecimal valor = taxDetails.valorTotal();

    // Determinar o CFOP predominante (80% ou mais dos itens)
    List<ItemTaxDetails> items = taxDetails.tributables();
    Map<String, Long> cfopCounts =
        items.stream().collect(Collectors.groupingBy(ItemTaxDetails::cfop, Collectors.counting()));

    String predominante =
        cfopCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(entry -> entry.getValue() >= items.size() * 0.8)
            .map(Map.Entry::getKey)
            .orElse("Indefinido");

    // Determinar a alíquota com base no CFOP predominante
    BigDecimal aliquota =
        switch (predominante) {
          case "5102" -> BigDecimal.valueOf(18.00);
          case "5405" -> BigDecimal.ZERO;
          default -> BigDecimal.ZERO;
        };

    return new InvoiceSummaryDetails(especie, serie, dia, uf, valor, predominante, aliquota);
  }

  private SalesSummaryDetails createSalesSummaryDetails(InvoiceTaxDetails taxDetails) {
    String numero = taxDetails.numero();
    String mod = "55"; // Sempre será "55"
    String data = taxDetails.dataEmissao().toLocalDate().toString();
    String envio = taxDetails.dataEmissao().toLocalDate().toString(); // Ajuste conforme necessário
    String cliente = "Cliente Indefinido"; // Substitua conforme necessário
    BigDecimal subtotal = taxDetails.valorProdutos();
    BigDecimal desconto = BigDecimal.ZERO; // Sempre será 0,00
    BigDecimal acrescimo = BigDecimal.ZERO; // Sempre será 0,00
    BigDecimal total = taxDetails.valorTotal();

    return new SalesSummaryDetails(
        numero, mod, data, envio, cliente, subtotal, desconto, acrescimo, total);
  }

  public List<SalesSummaryDetails> generateSalesSummaryDetails(
      LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);

    // Mapear os CombinedScores para SalesSummaryDetails
    return combinedScores.stream()
        .map(
            combinedScore -> {
              try {
                InvoiceTaxDetails taxDetails =
                    invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());
                return createSalesSummaryDetails(taxDetails);
              } catch (Exception e) {
                System.err.println("Erro ao processar CombinedScore ID: " + combinedScore.getId());
                e.printStackTrace();
                return null; // Ignorar registros com erro
              }
            })
        .filter(summary -> summary != null) // Remover nulos
        .collect(Collectors.toList());
  }

  public Map<String, BigDecimal> generateBankSettlementTotals(
      LocalDate startDate, LocalDate endDate) {
    String bankSettlement = "Liquidação Bancária"; // Substitua conforme necessário

    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);

    return combinedScores.stream()
        .map(
            combinedScore -> {
              try {
                InvoiceTaxDetails taxDetails =
                    invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());
                return taxDetails.valorTotal();
              } catch (Exception e) {
                System.err.println("Erro ao processar CombinedScore ID: " + combinedScore.getId());
                e.printStackTrace();
                return BigDecimal.ZERO;
              }
            })
        .filter(
            totalValue ->
                totalValue != null
                    && totalValue.compareTo(BigDecimal.ZERO) > 0) // Filtrar valores válidos
        .collect(Collectors.toMap(key -> bankSettlement, value -> value, BigDecimal::add));
  }

  private List<CombinedScore> fetchCombinedScores(LocalDate startDate, LocalDate endDate) {
    return combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate);
  }

  private List<String> extractInvoiceRefs(List<CombinedScore> combinedScores) {
    return combinedScores.stream().map(CombinedScore::getInvoiceRef).collect(Collectors.toList());
  }
}
