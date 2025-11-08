package com.hortifruti.sl.hortifruti.service.invoice.tax.icms;

import com.hortifruti.sl.hortifruti.dto.invoice.IcmsSalesReport;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class IcmsReport {
  private final ImcsReportCalculator icmsReportCalculator;
  private final IcmsPdfGenerator icmsPdfGenerator;

  public byte[] createIcmsReportPdf(
      String filePath, java.time.LocalDate startDate, java.time.LocalDate endDate)
      throws java.io.IOException {
    IcmsSalesReport report = icmsReportCalculator.generateIcmsSalesReport(startDate, endDate);
    return icmsPdfGenerator.generateIcmsReportPdf(report, startDate, endDate);
  }
}
