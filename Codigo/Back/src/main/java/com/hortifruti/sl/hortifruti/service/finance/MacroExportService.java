package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.service.invoice.tax.ReportTaxService;
import java.io.FileOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
      System.err.println("Erro de I/O durante geração de relatórios macro: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Erro ao processar arquivos macro", e);
    } catch (Exception e) {
      System.err.println("Erro geral durante geração de relatórios macro: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Erro interno durante geração de relatórios macro", e);
    } finally {
      if (zipPath != null && Files.exists(zipPath)) {
        try {
          Files.delete(zipPath);
        } catch (IOException e) {
          System.err.println("Erro ao excluir o arquivo: " + zipPath);
          e.printStackTrace();
        }
      }
    }
  }

  private String generateMacroReports(LocalDate startDate, LocalDate endDate) throws IOException {
    String folderName = createMacroFolder(startDate);
    Path folderPath = Path.of(folderName);

    generateTransactionReports(folderPath, startDate);

    generateTaxReports(startDate, endDate, folderPath);

    Path zipFilePath = compressFolder(folderPath, folderName);

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
          folderPath.resolve("Relatorios_Bancarios_" + capitalizeFirstLetter(monthName));

      for (Map.Entry<String, byte[]> entry : transactionData.entrySet()) {
        String fileName = entry.getKey();
        byte[] fileContent = entry.getValue();

        if (fileContent != null && fileContent.length > 0) {
          saveFile(bankFolder.resolve(fileName), fileContent);
        } else {
          System.err.println("Arquivo de transação está vazio: " + fileName);
        }
      }
    } catch (Exception e) {
      System.err.println("Erro ao gerar relatórios de transação: " + e.getMessage());
      throw e;
    }
  }

  private void generateTaxReports(LocalDate startDate, LocalDate endDate, Path folderPath)
      throws IOException {
    try {
      System.out.println("Iniciando geração de relatórios fiscais...");
      byte[] taxReportsZip = reportTaxService.generateMonthly(startDate, endDate);

      if (taxReportsZip != null && taxReportsZip.length > 0) {
        String monthName =
            startDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("pt", "BR")));
        String taxZipName = "Relatorios_Fiscais_" + capitalizeFirstLetter(monthName) + ".zip";
        saveFile(folderPath.resolve(taxZipName), taxReportsZip);
        System.out.println("Relatórios fiscais gerados com sucesso");
      } else {
        System.err.println("Relatórios fiscais estão vazios - continuando sem eles");
      }
    } catch (Exception e) {
      System.err.println("Erro ao gerar relatórios fiscais: " + e.getMessage());
      System.err.println("Continuando exportação sem relatórios fiscais...");

      String avisoContent =
          "AVISO: Os relatórios fiscais não puderam ser gerados devido a problemas com dados de notas fiscais.\n"
              + "Erro: "
              + e.getMessage()
              + "\n"
              + "Data: "
              + LocalDateTime.now();
      try {
        saveFile(folderPath.resolve("AVISO_Relatorios_Fiscais.txt"), avisoContent.getBytes());
      } catch (IOException ioEx) {
        System.err.println("Não foi possível criar arquivo de aviso: " + ioEx.getMessage());
      }
    }
  }

  private Path compressFolder(Path folderPath, String folderName) throws IOException {
    String zipFileName = folderName + ".zip";
    Path zipFilePath = Path.of(zipFileName);
    zipFolder(folderPath, zipFilePath);
    return zipFilePath;
  }

  private void saveFile(Path filePath, byte[] content) throws IOException {
    Files.createDirectories(filePath.getParent());
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

  private void deleteFolderRecursively(Path folderPath) {
    try {
      Files.walk(folderPath)
          .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException e) {
                  System.err.println("Erro ao deletar: " + path);
                }
              });
    } catch (IOException e) {
      System.err.println("Erro ao deletar pasta: " + folderPath);
    }
  }

  private String capitalizeFirstLetter(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1);
  }
}
