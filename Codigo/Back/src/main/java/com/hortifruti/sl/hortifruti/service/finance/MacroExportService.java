package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.service.finance.transaction.TransactionExportService;
import com.hortifruti.sl.hortifruti.service.invoice.tax.ReportTaxService;
import com.hortifruti.sl.hortifruti.util.FileZipUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MacroExportService {

  private final TransactionExportService transactionExportService;
  private final ReportTaxService reportTaxService;

  public Map<String, byte[]> exportMacroReports() throws IOException {
    LocalDate now = LocalDate.now();
    LocalDate firstDayLastMonth = now.minusMonths(1).withDayOfMonth(1);
    LocalDate lastDayLastMonth = now.withDayOfMonth(1).minusDays(1);

    Path zipPath = null;
    try {
      String zipFilePath = generateMacroReports(firstDayLastMonth, lastDayLastMonth);
      zipPath = Path.of(zipFilePath);

      if (!Files.exists(zipPath) || Files.size(zipPath) == 0) {
        throw new RuntimeException("Arquivo ZIP macro não foi gerado corretamente ou está vazio");
      }

      byte[] zipBytes = Files.readAllBytes(zipPath);

      String currentMonth =
          now.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
      String zipFileName = "Relatorio-Macro-Hortifruti-Santa-Luzia-" + currentMonth + ".zip";

      Map<String, byte[]> result = new HashMap<>();
      result.put(zipFileName, zipBytes);
      return result;

    } catch (IOException e) {
      log.error("Erro de I/O durante geração de relatórios macro", e);
      throw new RuntimeException("Erro ao processar arquivos macro", e);
    } catch (Exception e) {
      log.error("Erro geral durante geração de relatórios macro", e);
      throw new RuntimeException("Erro interno durante geração de relatórios macro", e);
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

  private String generateMacroReports(LocalDate startDate, LocalDate endDate) throws IOException {
    String folderName = createMacroFolder(startDate);
    Path folderPath = Path.of(folderName);

    generateTransactionReports(folderPath, startDate);

    generateTaxReports(startDate, endDate, folderPath);

    Path zipFilePath = FileZipUtils.compressFolder(folderPath, folderName);

    deleteFolderRecursively(folderPath);

    return zipFilePath.toString();
  }

  private String createMacroFolder(LocalDate startDate) throws IOException {
    String tempDir = System.getProperty("java.io.tmpdir");
    String folderName =
        tempDir + "/MACRO_RELATORIO_" + startDate.format(DateTimeFormatter.ofPattern("MM_yyyy"));
    Path folderPath = Path.of(folderName);

    if (!Files.exists(folderPath)) {
      Files.createDirectories(folderPath);
    }

    return folderName;
  }

  private void generateTransactionReports(Path folderPath, LocalDate startDate) throws IOException {
    try {
      Map<String, byte[]> transactionData = transactionExportService.exportTransactionsAsZip();

      String monthName =
          startDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("pt", "BR")));
      Path bankFolder =
          folderPath.resolve(
              "Relatorios_Bancarios_" + FileZipUtils.capitalizeFirstLetter(monthName));

      for (Map.Entry<String, byte[]> entry : transactionData.entrySet()) {
        String fileName = entry.getKey();
        byte[] fileContent = entry.getValue();

        if (fileContent != null && fileContent.length > 0) {
          FileZipUtils.saveFile(bankFolder.resolve(fileName), fileContent);
        } else {
          log.warn("Arquivo de transação está vazio: {}", fileName);
        }
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatórios de transação", e);
      throw e;
    }
  }

  private void generateTaxReports(LocalDate startDate, LocalDate endDate, Path folderPath)
      throws IOException {
    try {
      log.info("Iniciando geração de relatórios fiscais...");
      byte[] taxReportsZip = reportTaxService.generateMonthly(startDate, endDate);

      if (taxReportsZip != null && taxReportsZip.length > 0) {
        String monthName =
            startDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("pt", "BR")));
        String taxZipName =
            "Relatorios_Fiscais_" + FileZipUtils.capitalizeFirstLetter(monthName) + ".zip";
        FileZipUtils.saveFile(folderPath.resolve(taxZipName), taxReportsZip);
        log.info("Relatórios fiscais gerados com sucesso");
      } else {
        log.warn("Relatórios fiscais estão vazios - continuando sem eles");
      }
    } catch (Exception e) {
      log.error("Erro ao gerar relatórios fiscais - continuando exportação sem eles", e);

      String avisoContent =
          "AVISO: Os relatórios fiscais não puderam ser gerados devido a problemas com dados de notas fiscais.\n"
              + "Erro: "
              + e.getMessage()
              + "\n"
              + "Data: "
              + LocalDateTime.now();
      try {
        FileZipUtils.saveFile(
            folderPath.resolve("AVISO_Relatorios_Fiscais.txt"), avisoContent.getBytes());
      } catch (IOException ioEx) {
        log.error("Não foi possível criar arquivo de aviso", ioEx);
      }
    }
  }

  private void deleteFolderRecursively(Path folderPath) {
    try {
      Files.walk(folderPath)
          .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException e) {
                  log.error("Erro ao deletar: {}", path, e);
                }
              });
    } catch (IOException e) {
      log.error("Erro ao deletar pasta: {}", folderPath, e);
    }
  }
}
