package com.hortifruti.sl.hortifruti.service.invoice;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hortifruti.sl.hortifruti.dto.invoice.IcmsSalesReport;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceTaxDetails;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemTaxDetails;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;

import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Service
@AllArgsConstructor
public class ReportTaxService {
    private final InvoiceQuery invoiceQuery;
    private final CombinedScoreService combinedScoreService;

    public IcmsSalesReport generateIcmsSalesReport(LocalDate startDate, LocalDate endDate) {
        // Obter todos os CombinedScores com hasInvoice = true no período especificado
        List<CombinedScore> combinedScores = combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate);

        BigDecimal totalContabil = BigDecimal.ZERO;
        BigDecimal totalBaseCalculo = BigDecimal.ZERO;
        BigDecimal totalImpostoDebitado = BigDecimal.ZERO;
        BigDecimal totalIsentasOuNaoTributadas = BigDecimal.ZERO;
        BigDecimal totalOutras = BigDecimal.ZERO;

        Map<String, BigDecimal> valoresPorCfop = new HashMap<>();

        // Iterar sobre os CombinedScores e extrair os detalhes fiscais
        for (CombinedScore combinedScore : combinedScores) {
            try {
                InvoiceTaxDetails taxDetails = invoiceQuery.extractInvoiceTaxDetails(combinedScore.getInvoiceRef());

                // Atualizar totais
                totalContabil = totalContabil.add(taxDetails.valorTotal());
                totalBaseCalculo = totalBaseCalculo.add(taxDetails.icmsBaseCalculo());
                totalImpostoDebitado = totalImpostoDebitado.add(taxDetails.icmsValorTotal());

                // Agrupar valores por CFOP
                for (ItemTaxDetails item : taxDetails.tributables()) {
                    valoresPorCfop.merge(item.cfop(), item.valorBruto(), BigDecimal::add);
                }

                // Atualizar totais de isentas ou não tributadas e outras
                totalIsentasOuNaoTributadas = totalIsentasOuNaoTributadas.add(BigDecimal.ZERO); // Ajuste conforme necessário
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
            valoresPorCfop
        );
    }
}
