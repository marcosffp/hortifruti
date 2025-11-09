package com.hortifruti.sl.hortifruti.service.invoice.tax.nfSales;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class NfSalesReport {
  private final NfSalesCalculator nfSalesCalculator;
  private final NfSalesZipGenerator nfSalesZipGenerator;

  public String createNfSalesZip(LocalDate startDate, LocalDate endDate) throws IOException {
    List<File> xmlFiles = nfSalesCalculator.generateXmlFileList(startDate, endDate);

    return nfSalesZipGenerator.generateZipFromXmlFiles(xmlFiles, startDate, endDate);
  }
}
