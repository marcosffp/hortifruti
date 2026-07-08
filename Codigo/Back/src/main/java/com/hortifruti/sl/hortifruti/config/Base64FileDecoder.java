package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Base64FileDecoder {

  @Value("${google.drive.credentials}")
  private String googleDriveCredentials;

  @Value("${document.pfx}")
  private String pfx;

  @Value("${pfx.temp.directory}")
  private String pfxTempDirectory;

  @Value("${google.temp.directory}")
  private String googleTempDirectory;

  public File decodeGoogleDriveCredentials() throws IOException {
    if (googleDriveCredentials == null || googleDriveCredentials.isEmpty()) {
      throw new BackupException(
          "A propriedade 'google.drive.credentials' está vazia ou não foi configurada.");
    }

    String outputPath = googleTempDirectory + "/drive_credentials.json";
    File decodedFile = decodeBase64ToFile(googleDriveCredentials, outputPath);
    return decodedFile;
  }

  public File decodePfx() throws IOException {
    if (pfx == null || pfx.isEmpty()) {
      throw new BilletException("A propriedade 'document.pfx' está vazia ou não foi configurada.");
    }
    String outputPath = pfxTempDirectory + "/HORTIFRUTISANTALUZIALTDA275409060001552025.pfx";
    File decodedFile = decodeBase64ToFile(pfx, outputPath);
    if (decodedFile == null) {
      throw new BilletException("Falha ao decodificar o arquivo PFX.");
    }
    if (decodedFile.exists()) {
    } else {
      System.err.println("[ERROR] Falha ao decodificar o arquivo PFX.");
    }
    return decodedFile;
  }

  private File decodeBase64ToFile(String base64, String outputPath) throws IOException {
    byte[] decodedBytes = Base64.getDecoder().decode(base64);
    File outputFile = new File(outputPath);
    if (!outputFile.getParentFile().exists()) {
      outputFile.getParentFile().mkdirs();
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      fos.write(decodedBytes);
    }
    return outputFile;
  }

  public File getGoogleDriveCredentialsFile() {
    File file = new File(googleTempDirectory + "/drive_credentials.json");
    return file.exists() ? file : null;
  }

  public File getPfxFile() {
    File file = new File(pfxTempDirectory + "/HORTIFRUTISANTALUZIALTDA275409060001552025.pfx");
    return file.exists() ? file : null;
  }
}
