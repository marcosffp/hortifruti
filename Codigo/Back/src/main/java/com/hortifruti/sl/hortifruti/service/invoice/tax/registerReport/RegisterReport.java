package com.hortifruti.sl.hortifruti.service.invoice.tax.registerReport;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceSummaryDetails;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RegisterReport {
  private final RegisterCalculator registerReportCalculator;
  private final RegisterPdfGenerator registerReportPdfGenerator;

  public byte[] createRegisterReportPdf(LocalDate startDate, LocalDate endDate) throws IOException {
    List<InvoiceSummaryDetails> invoiceSummaries =
        registerReportCalculator.generateInvoiceSummaryDetails(startDate, endDate);
    return registerReportPdfGenerator.generateRegisterReportPdf(
        invoiceSummaries, startDate, endDate);
  }
}
