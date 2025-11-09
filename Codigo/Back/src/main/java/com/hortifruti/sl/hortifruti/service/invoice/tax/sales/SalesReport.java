package com.hortifruti.sl.hortifruti.service.invoice.tax.sales;

import com.hortifruti.sl.hortifruti.dto.invoice.SalesSummaryDetails;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SalesReport {
  private final SalesCalculator salesCalculator;
  private final SalesPdfGenerator salesPdfGenerator;

  public byte[] createSalesReportPdf(LocalDate startDate, LocalDate endDate) throws IOException {
    List<SalesSummaryDetails> salesSummaries =
        salesCalculator.generateSalesSummaryDetails(startDate, endDate);
    return salesPdfGenerator.generateSalesReportPdf(salesSummaries, startDate, endDate);
  }
}
