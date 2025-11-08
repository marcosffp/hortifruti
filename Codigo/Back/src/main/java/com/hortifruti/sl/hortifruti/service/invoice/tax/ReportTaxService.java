package com.hortifruti.sl.hortifruti.service.invoice.tax;

import com.hortifruti.sl.hortifruti.service.invoice.tax.icms.IcmsReport;
import com.hortifruti.sl.hortifruti.service.invoice.tax.nfSales.NfSalesReport;
import com.hortifruti.sl.hortifruti.service.invoice.tax.payment.PaymentReport;
import com.hortifruti.sl.hortifruti.service.invoice.tax.registerReport.RegisterReport;
import com.hortifruti.sl.hortifruti.service.invoice.tax.sales.SalesReport;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ReportTaxService {
  private final PaymentReport paymentReport;
  private final RegisterReport registerReport;
  private final SalesReport salesReport;
  private final NfSalesReport nfSalesReport;
  private final IcmsReport icmsReport;

  public byte[] generateMonthly(LocalDate startDate, LocalDate endDate) {
    try {

      String zipFilePath = generateMonthlyReports(startDate, endDate);

      Path zipPath = Paths.get(zipFilePath);
      byte[] zipBytes = Files.readAllBytes(zipPath);

      String sanitizedFileName =
          zipPath.getFileName().toString().replace(":", "_").replace("\\", "/");

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentDispositionFormData("attachment", sanitizedFileName);

      return zipBytes;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private byte[] generatePaymentReport(LocalDate startDate, LocalDate endDate) throws IOException {
    return paymentReport.createPaymentReportPdf(startDate, endDate);
  }

  private byte[] generateRegisterReport(LocalDate startDate, LocalDate endDate) throws IOException {
    return registerReport.createRegisterReportPdf(startDate, endDate);
  }

  private byte[] generateSalesReport(LocalDate startDate, LocalDate endDate) throws IOException {
    return salesReport.createSalesReportPdf(startDate, endDate);
  }

  private String generateNfSalesZip(LocalDate startDate, LocalDate endDate) throws IOException {
    return nfSalesReport.createNfSalesZip(startDate, endDate);
  }

  private byte[] generateIcmsReport(LocalDate startDate, LocalDate endDate) throws IOException {
    return icmsReport.createIcmsReportPdf(null, startDate, endDate);
  }

  private String generateMonthlyReports(LocalDate startDate, LocalDate endDate) throws IOException {
    String folderName = createMonthlyFolder(startDate);
    Path folderPath = Path.of(folderName);

    generateAndSaveReports(startDate, endDate, folderPath);
    generateAndMoveNfSalesZip(startDate, endDate, folderPath);

    Path zipFilePath = compressFolder(folderPath, folderName);

    return zipFilePath.toString();
  }

  private String createMonthlyFolder(LocalDate startDate) throws IOException {
    String tempDir = System.getProperty("java.io.tmpdir");
    String folderName = tempDir + "/MES_" + startDate.format(DateTimeFormatter.ofPattern("MM"));
    Path folderPath = Path.of(folderName);

    if (!Files.exists(folderPath)) {
      Files.createDirectories(folderPath);
    }

    return folderName;
  }

  private void generateAndSaveReports(LocalDate startDate, LocalDate endDate, Path folderPath)
      throws IOException {
    saveFile(
        folderPath.resolve("Resumo_de_Vendas_por_Forma_de_Pagamento.pdf"),
        generatePaymentReport(startDate, endDate));
    saveFile(
        folderPath.resolve("Registro_de_saida_nf.pdf"), generateRegisterReport(startDate, endDate));
    saveFile(folderPath.resolve("Relacao_de_Vendas.pdf"), generateSalesReport(startDate, endDate));
    saveFile(
        folderPath.resolve("Registro_Apuracao_ICMS.pdf"), generateIcmsReport(startDate, endDate));
  }

  private void generateAndMoveNfSalesZip(LocalDate startDate, LocalDate endDate, Path folderPath)
      throws IOException {
    String monthName = startDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("pt", "BR")));
    String nfSalesZipName = capitalizeFirstLetter(monthName) + "_NFE_SAIDAS.zip";
    String nfSalesZipPath = generateNfSalesZip(startDate, endDate);

    Files.move(Path.of(nfSalesZipPath), folderPath.resolve(nfSalesZipName));
  }

  private Path compressFolder(Path folderPath, String folderName) throws IOException {
    String zipFileName = folderName + ".zip";
    Path zipFilePath = Path.of(zipFileName);
    zipFolder(folderPath, zipFilePath);
    return zipFilePath;
  }

  private String capitalizeFirstLetter(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1);
  }

  private void saveFile(Path filePath, byte[] content) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
      fos.write(content);
    }
  }

  private void zipFolder(Path sourceFolderPath, Path zipPath) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
      Files.walk(sourceFolderPath)
          .filter(path -> !Files.isDirectory(path))
          .forEach(
              path -> {
                ZipEntry zipEntry = new ZipEntry(sourceFolderPath.relativize(path).toString());
                try {
                  zos.putNextEntry(zipEntry);
                  Files.copy(path, zos);
                  zos.closeEntry();
                } catch (IOException e) {
                  throw new RuntimeException("Erro ao compactar arquivo: " + path, e);
                }
              });
    }
  }
}
