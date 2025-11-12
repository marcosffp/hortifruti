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
import org.springframework.stereotype.Service;

@Service
public class NfSalesZipGenerator {

  public String generateZipFromXmlFiles(List<File> xmlFiles, LocalDate startDate, LocalDate endDate)
      throws IOException {
    String folderName = formatFolderName(startDate, endDate);
    Path folderPath = Paths.get(folderName);

    Files.createDirectories(folderPath);

    for (File xmlFile : xmlFiles) {
      try {
        if (!xmlFile.exists() || xmlFile.length() == 0) {
          System.err.println("Arquivo XML inválido ou vazio: " + xmlFile.getName());
          continue;
        }
        Path sourcePath = xmlFile.toPath();
        Path destinationPath = folderPath.resolve(sourcePath.getFileName());
        System.out.println("Copiando arquivo de: " + sourcePath + " para: " + destinationPath);

        Files.copy(sourcePath, destinationPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        System.err.println("Erro ao copiar arquivo XML: " + xmlFile.getName());
        e.printStackTrace();
        throw new IOException("Erro ao copiar arquivo XML: " + xmlFile.getName(), e);
      }
    }

    String zipFileName = folderName + ".zip";
    try (FileOutputStream fos = new FileOutputStream(zipFileName);
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
                  System.err.println("Erro ao adicionar arquivo ao ZIP: " + file);
                  e.printStackTrace();
                  throw new RuntimeException("Erro ao adicionar arquivo ao ZIP: " + file, e);
                }
              });
    } catch (IOException e) {
      System.err.println("Erro ao criar o arquivo ZIP: " + zipFileName);
      e.printStackTrace();
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
                  System.err.println("Erro ao excluir: " + path);
                  e.printStackTrace();
                }
              });
    } catch (IOException e) {
      System.err.println("Erro ao excluir a pasta: " + folderPath);
      e.printStackTrace();
    }

    return zipFileName;
  }

  private String formatFolderName(LocalDate startDate, LocalDate endDate) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
    String startMonthYear = startDate.format(formatter);
    String endMonthYear = endDate.format(formatter);
    return "NF_Sales_" + startMonthYear + "_to_" + endMonthYear;
  }
}
