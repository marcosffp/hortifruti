package com.hortifruti.sl.hortifruti.service.invoice.tax;

import com.hortifruti.sl.hortifruti.service.invoice.tax.icms.IcmsReport;
import com.hortifruti.sl.hortifruti.service.invoice.tax.nfSales.NfSalesReport;
import com.hortifruti.sl.hortifruti.service.invoice.tax.payment.PaymentReport;
import com.hortifruti.sl.hortifruti.service.invoice.tax.registerReport.RegisterReport;
import com.hortifruti.sl.hortifruti.service.invoice.tax.sales.SalesReport;
import com.hortifruti.sl.hortifruti.util.FileZipUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ReportTaxService {
  private final PaymentReport paymentReport;
  private final RegisterReport registerReport;
  private final SalesReport salesReport;
  private final NfSalesReport nfSalesReport;
  private final IcmsReport icmsReport;

  public byte[] generateMonthly(LocalDate startDate, LocalDate endDate) {
    Path zipPath = null;
    try {
      String zipFilePath = generateMonthlyReports(startDate, endDate);
      zipPath = Paths.get(zipFilePath);

      if (!Files.exists(zipPath) || Files.size(zipPath) == 0) {
        throw new RuntimeException("Arquivo ZIP não foi gerado corretamente ou está vazio");
      }

      byte[] zipBytes = Files.readAllBytes(zipPath);
      return zipBytes;
    } catch (IOException e) {
      log.error("Erro de I/O durante geração de relatórios", e);
      throw new RuntimeException("Erro ao processar arquivos", e);
    } catch (Exception e) {
      log.error("Erro geral durante geração de relatórios", e);
      throw new RuntimeException("Erro interno durante geração de relatórios", e);
    } finally {
      if (zipPath != null && Files.exists(zipPath)) {
        try {
          Files.delete(zipPath);
        } catch (IOException e) {
          log.error("Erro ao excluir o arquivo: {}", zipPath, e);
        }
      }
    }
  }

  /**
   * Igual ao gerado por {@link #generateMonthly}, mas retorna os arquivos soltos (PDFs + XMLs de
   * NF-e) em vez de um ZIP único, para que possam ser salvos como uma pasta comum dentro do
   * relatório macro, no mesmo formato usado pelos relatórios bancários.
   */
  public Map<String, byte[]> generateMonthlyFiles(LocalDate startDate, LocalDate endDate) {
    Map<String, byte[]> files = new HashMap<>();

    try {
      byte[] paymentData = generatePaymentReport(startDate, endDate);
      if (paymentData != null && paymentData.length > 0) {
        files.put("Resumo_de_Vendas_por_Forma_de_Pagamento.pdf", paymentData);
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatório de pagamento - continuando sem ele", e);
    }

    try {
      byte[] registerData = generateRegisterReport(startDate, endDate);
      if (registerData != null && registerData.length > 0) {
        files.put("Registro_de_saida_nf.pdf", registerData);
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatório de registro de saída - continuando sem ele", e);
    }

    try {
      byte[] salesData = generateSalesReport(startDate, endDate);
      if (salesData != null && salesData.length > 0) {
        files.put("Relacao_de_Vendas.pdf", salesData);
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatório de vendas - continuando sem ele", e);
    }

    try {
      byte[] icmsData = generateIcmsReport(startDate, endDate);
      if (icmsData != null && icmsData.length > 0) {
        files.put("Registro_Apuracao_ICMS.pdf", icmsData);
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatório de apuração de ICMS - continuando sem ele", e);
    }

    try {
      String monthName =
          startDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("pt", "BR")));
      String nfSalesFolder = FileZipUtils.capitalizeFirstLetter(monthName) + "_NFE_SAIDAS";
      List<File> xmlFiles = nfSalesReport.listXmlFiles(startDate, endDate);

      for (File xmlFile : xmlFiles) {
        try {
          if (xmlFile.exists() && xmlFile.length() > 0) {
            files.put(
                nfSalesFolder + "/" + xmlFile.getName(), Files.readAllBytes(xmlFile.toPath()));
          }
        } finally {
          Files.deleteIfExists(xmlFile.toPath());
        }
      }
    } catch (Exception e) {
      log.error("Erro ao gerar XMLs de notas fiscais - continuando sem eles", e);
    }

    return files;
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
    try {
      byte[] paymentData = generatePaymentReport(startDate, endDate);
      if (paymentData != null && paymentData.length > 0) {
        FileZipUtils.saveFile(
            folderPath.resolve("Resumo_de_Vendas_por_Forma_de_Pagamento.pdf"), paymentData);
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatório de pagamento - continuando sem ele", e);
    }

    try {
      byte[] registerData = generateRegisterReport(startDate, endDate);
      if (registerData != null && registerData.length > 0) {
        FileZipUtils.saveFile(folderPath.resolve("Registro_de_saida_nf.pdf"), registerData);
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatório de registro de saída - continuando sem ele", e);
    }

    try {
      byte[] salesData = generateSalesReport(startDate, endDate);
      if (salesData != null && salesData.length > 0) {
        FileZipUtils.saveFile(folderPath.resolve("Relacao_de_Vendas.pdf"), salesData);
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatório de vendas - continuando sem ele", e);
    }

    try {
      byte[] icmsData = generateIcmsReport(startDate, endDate);
      if (icmsData != null && icmsData.length > 0) {
        FileZipUtils.saveFile(folderPath.resolve("Registro_Apuracao_ICMS.pdf"), icmsData);
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatório de apuração de ICMS - continuando sem ele", e);
    }
  }

  private void generateAndMoveNfSalesZip(LocalDate startDate, LocalDate endDate, Path folderPath)
      throws IOException {
    String monthName = startDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("pt", "BR")));
    String nfSalesZipName = FileZipUtils.capitalizeFirstLetter(monthName) + "_NFE_SAIDAS.zip";
    String nfSalesZipPath = generateNfSalesZip(startDate, endDate);

    Path targetPath = folderPath.resolve(nfSalesZipName);

    if (Files.exists(targetPath)) {
      Files.delete(targetPath);
    }

    Files.move(Path.of(nfSalesZipPath), targetPath);
  }

  private Path compressFolder(Path folderPath, String folderName) throws IOException {
    return FileZipUtils.compressFolder(folderPath, folderName);
  }
}
