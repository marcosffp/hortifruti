package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Base64FileDecoder {

  private static final String PFX_FILE_NAME = "HORTIFRUTISANTALUZIALTDA275409060001552025.pfx";

  @Value("${google.drive.credentials}")
  private String googleDriveCredentials;

  @Value("${mtls.shared-certificate.pfx-base64}")
  private String pfx;

  @Value("${mtls.shared-certificate.pem-base64}")
  private String pem;

  @Value("${pfx.temp.directory}")
  private String pfxTempDirectory;

  @Value("${pem.temp.directory}")
  private String pemTempDirectory;

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
      throw new BilletException(
          "A propriedade 'mtls.shared-certificate.pfx-base64' está vazia ou não foi configurada.");
    }
    String outputPath = pfxTempDirectory + "/" + PFX_FILE_NAME;
    File decodedFile = decodeBase64ToFile(pfx, outputPath);
    if (decodedFile == null || !decodedFile.exists()) {
      throw new BilletException("Falha ao decodificar o arquivo PFX.");
    }
    return decodedFile;
  }

  public File decodePem() throws IOException {
    if (pem == null || pem.isEmpty()) {
      throw new BilletException(
          "A propriedade 'mtls.shared-certificate.pem-base64' está vazia ou não foi configurada.");
    }

    File tempDir = new File(pemTempDirectory);
    if (!tempDir.exists() && !tempDir.mkdirs()) {
      throw new BilletException(
          "Não foi possível criar o diretório temporário: " + pemTempDirectory);
    }

    String outputPath = pemTempDirectory + "/empresa.pem";
    File decodedFile = decodeBase64ToFile(pem, outputPath);

    if (decodedFile == null || !decodedFile.exists()) {
      throw new BilletException("Falha ao decodificar o arquivo PEM.");
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
    File file = new File(pfxTempDirectory + "/" + PFX_FILE_NAME);
    return file.exists() ? file : null;
  }

  public File getPemFile() {
    File file = new File(pemTempDirectory + "/empresa.pem");
    return file.exists() ? file : null;
  }
}
