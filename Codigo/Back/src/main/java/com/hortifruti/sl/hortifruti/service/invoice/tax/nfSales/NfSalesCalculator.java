package com.hortifruti.sl.hortifruti.service.invoice.tax.nfSales;

import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.invoice.DanfeXmlService;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class NfSalesCalculator {
  private final CombinedScoreService combinedScoreService;
  private final DanfeXmlService danfeXmlService;

  public List<File> generateXmlFileList(LocalDate startDate, LocalDate endDate) {
    List<CombinedScore> combinedScores = fetchCombinedScores(startDate, endDate);
    List<String> invoiceRefs = extractInvoiceRefs(combinedScores);

    return danfeXmlService.downloadXmlFilesForPeriod(invoiceRefs);
  }

  private List<CombinedScore> fetchCombinedScores(LocalDate startDate, LocalDate endDate) {
    return combinedScoreService.getCombinedScoresWithInvoice(startDate, endDate);
  }

  private List<String> extractInvoiceRefs(List<CombinedScore> combinedScores) {
    return combinedScores.stream().map(CombinedScore::getInvoiceRef).collect(Collectors.toList());
  }
}
