package com.hortifruti.sl.hortifruti.service.invoice.tax.payment;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentReport {
  private final PaymentCalculator paymentCalculator;
  private final PaymentPdfGenerator paymentPdfGenerator;

  public byte[] createPaymentReportPdf(LocalDate startDate, LocalDate endDate) throws IOException {
    Map<String, BigDecimal> paymentSummary =
        paymentCalculator.generateBankSettlementTotals(startDate, endDate);
    return paymentPdfGenerator.generateSummaryByPaymentPdf(paymentSummary, startDate, endDate);
  }
}
