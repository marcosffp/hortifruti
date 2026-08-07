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
    } catch (RuntimeException e) {
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

  /** Nome do arquivo final de cada um dos 4 relatórios fiscais "core" + como gerá-lo. */
  @FunctionalInterface
  private interface ReportGenerator {
    byte[] generate(LocalDate startDate, LocalDate endDate) throws IOException;
  }

  private record ReportSpec(String fileName, ReportGenerator generator) {}

  /** Destino de cada relatório gerado — mapa em memória ({@link #generateMonthlyFiles}) ou arquivo em disco ({@link #generateAndSaveReports}). */
  @FunctionalInterface
  private interface ReportSink {
    void accept(String fileName, byte[] data) throws IOException;
  }

  private List<ReportSpec> coreReportSpecs() {
    return List.of(
        new ReportSpec(
            "Resumo_de_Vendas_por_Forma_de_Pagamento.pdf", this::generatePaymentReport),
        new ReportSpec("Registro_de_saida_nf.pdf", this::generateRegisterReport),
        new ReportSpec("Relacao_de_Vendas.pdf", this::generateSalesReport),
        new ReportSpec("Registro_Apuracao_ICMS.pdf", this::generateIcmsReport));
  }

  /**
   * Gera os 4 relatórios fiscais "core" (pagamento, registro de saída, vendas, apuração de ICMS) e
   * entrega cada um ao {@code sink} — usado tanto para juntar tudo num {@code Map} em memória
   * quanto para salvar cada um como arquivo em disco (ver {@link #generateMonthlyFiles} e {@link
   * #generateAndSaveReports}). Falha em um relatório individual não interrompe os demais — só é
   * logada, o relatório final sai sem aquele item.
   */
  private void generateCoreReports(LocalDate startDate, LocalDate endDate, ReportSink sink) {
    for (ReportSpec spec : coreReportSpecs()) {
      try {
        byte[] data = spec.generator().generate(startDate, endDate);
        if (data != null && data.length > 0) {
          sink.accept(spec.fileName(), data);
        }
      } catch (Exception e) {
        log.error("Erro ao gerar relatório '{}' - continuando sem ele", spec.fileName(), e);
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

    generateCoreReports(startDate, endDate, files::put);

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

  private void generateAndSaveReports(LocalDate startDate, LocalDate endDate, Path folderPath) {
    generateCoreReports(
        startDate, endDate, (fileName, data) -> FileZipUtils.saveFile(folderPath.resolve(fileName), data));
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
