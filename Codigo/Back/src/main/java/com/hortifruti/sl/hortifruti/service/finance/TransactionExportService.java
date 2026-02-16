package com.hortifruti.sl.hortifruti.service.finance;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionExportService {

  private final TransactionExcelExportService transactionExcelExportService;
  private final TransactionPdfExportService transactionPdfExportService;

  public Map<String, byte[]> exportTransactionsAsZip() throws IOException {
    // Gerar Excel
    Map<String, byte[]> excelData = transactionExcelExportService.exportTransactionsAsExcel();

    // Gerar PDF
    Map<String, byte[]> pdfData = transactionPdfExportService.exportTransactionsAsPdf();

    // Criar ZIP com ambos os arquivos
    String currentMonth =
        LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
    String zipFileName = "Relatorio-Hortifruti-Santa-Luzia-" + currentMonth + ".zip";

    File tempZipFile = File.createTempFile("tmp", ".zip");
    try {
      ZipFile zipFile = new ZipFile(tempZipFile);

      // Adicionar Excel ao ZIP
      String excelFileName = excelData.keySet().iterator().next();
      byte[] excelBytes = excelData.get(excelFileName);
      ZipParameters excelParams = new ZipParameters();
      excelParams.setFileNameInZip(excelFileName);
      zipFile.addStream(new ByteArrayInputStream(excelBytes), excelParams);

      // Adicionar PDF ao ZIP
      String pdfFileName = pdfData.keySet().iterator().next();
      byte[] pdfBytes = pdfData.get(pdfFileName);
      ZipParameters pdfParams = new ZipParameters();
      pdfParams.setFileNameInZip(pdfFileName);
      zipFile.addStream(new ByteArrayInputStream(pdfBytes), pdfParams);

      zipFile.close();

      byte[] zipBytes = Files.readAllBytes(tempZipFile.toPath());
      Map<String, byte[]> result = new HashMap<>();
      result.put(zipFileName, zipBytes);
      return result;
    } finally {
      tempZipFile.delete();
    }
  }
}
