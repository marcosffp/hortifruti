package com.hortifruti.sl.hortifruti.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Utilitário compartilhado de manipulação de arquivos/pastas usado pelos serviços de export. */
public class FileZipUtils {

  private FileZipUtils() {}

  public static void saveFile(Path filePath, byte[] content) throws IOException {
    if (filePath.getParent() != null) {
      Files.createDirectories(filePath.getParent());
    }
    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
      fos.write(content);
    }
  }

  public static Path compressFolder(Path folderPath, String folderName) throws IOException {
    String zipFileName = folderName + ".zip";
    Path zipFilePath = Path.of(zipFileName);
    zipFolder(folderPath, zipFilePath);
    return zipFilePath;
  }

  public static void zipFolder(Path sourceFolderPath, Path zipPath) throws IOException {
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

  public static String capitalizeFirstLetter(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1);
  }
}
