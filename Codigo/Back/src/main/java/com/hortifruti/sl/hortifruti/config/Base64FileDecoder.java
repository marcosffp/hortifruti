package com.hortifruti.sl.hortifruti.config;

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

  /**
   * Decodifica o arquivo Google Drive Credentials e o salva no diretório configurado.
   *
   * @return O arquivo decodificado.
   * @throws IOException Se ocorrer um erro ao salvar o arquivo.
   */
  public File decodeGoogleDriveCredentials() throws IOException {
    if (googleDriveCredentials == null || googleDriveCredentials.isEmpty()) {
      System.err.println(
          "[ERROR] A propriedade 'google.drive.credentials' está vazia ou não foi configurada.");
      throw new IllegalArgumentException(
          "A propriedade 'google.drive.credentials' está vazia ou não foi configurada.");
    }
    System.out.println(
        "[DEBUG] Decodificando credenciais do Google Drive para o diretório: "
            + googleTempDirectory);
    String outputPath = googleTempDirectory + "/drive_credentials.json";
    File decodedFile = decodeBase64ToFile(googleDriveCredentials, outputPath);
    System.out.println(
        "[DEBUG] Arquivo de credenciais decodificado: " + decodedFile.getAbsolutePath());
    return decodedFile;
  }

  /**
   * Decodifica o arquivo Sicoob PFX e o salva no diretório configurado.
   *
   * @return O arquivo decodificado.
   * @throws IOException Se ocorrer um erro ao salvar o arquivo.
   */
  public File decodePfx() throws IOException {
    if (pfx == null || pfx.isEmpty()) {
      throw new IllegalArgumentException(
          "A propriedade 'document.pfx' está vazia ou não foi configurada.");
    }
    System.out.println("[DEBUG] Decodificando PFX para o diretório: " + pfxTempDirectory);
    String outputPath = pfxTempDirectory + "/HORTIFRUTISANTALUZIALTDA275409060001552025.pfx";
    File decodedFile = decodeBase64ToFile(pfx, outputPath);
    if (decodedFile == null) {
      throw new IOException("Falha ao decodificar o arquivo PFX.");
    }
    if (decodedFile.exists()) {
      System.out.println(
          "[DEBUG] Arquivo PFX decodificado com sucesso: " + decodedFile.getAbsolutePath());
    } else {
      System.err.println("[ERROR] Falha ao decodificar o arquivo PFX.");
    }
    return decodedFile;
  }

  /**
   * Método genérico para decodificar uma string Base64 e salvar como arquivo.
   *
   * @param base64 A string codificada em Base64.
   * @param outputPath O caminho onde o arquivo será salvo.
   * @return O arquivo decodificado.
   * @throws IOException Se ocorrer um erro ao salvar o arquivo.
   */
  private File decodeBase64ToFile(String base64, String outputPath) throws IOException {
    byte[] decodedBytes = Base64.getDecoder().decode(base64);
    File outputFile = new File(outputPath);
    if (!outputFile.getParentFile().exists()) {
      outputFile.getParentFile().mkdirs(); // Garante que o diretório exista
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      fos.write(decodedBytes);
    }
    return outputFile;
  }

  /**
   * Verifica se o arquivo Google Drive Credentials já existe no diretório configurado.
   *
   * @return O arquivo, se existir; caso contrário, null.
   */
  public File getGoogleDriveCredentialsFile() {
    File file = new File(googleTempDirectory + "/drive_credentials.json");
    return file.exists() ? file : null;
  }

  /**
   * Verifica se o arquivo Sicoob PFX já existe no diretório configurado.
   *
   * @return O arquivo, se existir; caso contrário, null.
   */
  public File getPfxFile() {
    File file = new File(pfxTempDirectory + "/HORTIFRUTISANTALUZIALTDA275409060001552025.pfx");
    return file.exists() ? file : null;
  }
}
