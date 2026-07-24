package com.hortifruti.sl.hortifruti.service.invoice.tax.nfSales;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NfSalesZipGenerator {

  public String generateZipFromXmlFiles(List<File> xmlFiles, LocalDate startDate, LocalDate endDate)
      throws IOException {
    String folderName = formatFolderName(startDate, endDate);

    Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
    Path folderPath = tempDir.resolve(folderName);

    Files.createDirectories(folderPath);

    for (File xmlFile : xmlFiles) {
      try {
        if (!xmlFile.exists() || xmlFile.length() == 0) {
          log.warn("Arquivo XML inválido ou vazio: {}", xmlFile.getName());
          continue;
        }
        Path sourcePath = xmlFile.toPath();
        Path destinationPath = folderPath.resolve(sourcePath.getFileName());

        Files.copy(sourcePath, destinationPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        log.error("Erro ao copiar arquivo XML: {}", xmlFile.getName(), e);
        throw new IOException("Erro ao copiar arquivo XML: " + xmlFile.getName(), e);
      }
    }

    String zipFileName = folderName + ".zip";
    Path zipFilePath = tempDir.resolve(zipFileName);

    try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
        ZipOutputStream zos = new ZipOutputStream(fos)) {

      Files.walk(folderPath)
          .filter(Files::isRegularFile)
          .forEach(
              file -> {
                try {
                  ZipEntry zipEntry = new ZipEntry(folderPath.relativize(file).toString());
                  zos.putNextEntry(zipEntry);
                  Files.copy(file, zos);
                  zos.closeEntry();
                } catch (IOException e) {
                  log.error("Erro ao adicionar arquivo ao ZIP: {}", file, e);
                  throw new RuntimeException("Erro ao adicionar arquivo ao ZIP: " + file, e);
                }
              });
    } catch (IOException e) {
      log.error("Erro ao criar o arquivo ZIP: {}", zipFileName, e);
      throw new IOException("Erro ao criar o arquivo ZIP: " + zipFileName, e);
    }

    try {
      Files.walk(folderPath)
          .sorted((path1, path2) -> path2.compareTo(path1))
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException e) {
                  log.error("Erro ao excluir: {}", path, e);
                }
              });
    } catch (IOException e) {
      log.error("Erro ao excluir a pasta: {}", folderPath, e);
    }

    return zipFilePath.toString();
  }

  private String formatFolderName(LocalDate startDate, LocalDate endDate) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
    String startMonthYear = startDate.format(formatter);
    String endMonthYear = endDate.format(formatter);
    return "NF_Sales_" + startMonthYear + "_to_" + endMonthYear;
  }
}
