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

  @Value("${document.pem}")
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

public File decodePem() throws IOException {
    System.out.println("[DEBUG] Iniciando decodePem()");

    if (pem == null || pem.isEmpty()) {
        System.err.println("[ERROR] Propriedade 'document.pem' está vazia ou não foi configurada.");
        throw new BilletException("A propriedade 'document.pem' está vazia ou não foi configurada.");
    }

    System.out.println("[DEBUG] Tamanho da string PEM recebida (base64): " + pem.length() + " caracteres");
    System.out.println("[DEBUG] pemTempDirectory configurado: " + pemTempDirectory);

    File tempDir = new File(pemTempDirectory);
    System.out.println("[DEBUG] Diretório temp existe? " + tempDir.exists()
            + " | é diretório? " + tempDir.isDirectory()
            + " | tem permissão de escrita? " + tempDir.canWrite());

    if (!tempDir.exists()) {
        System.err.println("[ERROR] Diretório '" + pemTempDirectory + "' não existe. Tentando criar...");
        boolean created = tempDir.mkdirs();
        System.out.println("[DEBUG] Diretório criado com sucesso? " + created);
    }

    String outputPath = pemTempDirectory + "/empresa.pem";
    System.out.println("[DEBUG] Caminho de saída do arquivo PEM: " + outputPath);

    File decodedFile;
    try {
        decodedFile = decodeBase64ToFile(pem, outputPath);
    } catch (Exception e) {
        System.err.println("[ERROR] Exceção lançada dentro de decodeBase64ToFile: " + e.getClass().getName()
                + " - " + e.getMessage());
        e.printStackTrace();
        throw e;
    }

    if (decodedFile == null) {
        System.err.println("[ERROR] decodeBase64ToFile retornou null para o caminho: " + outputPath);
        throw new BilletException("Falha ao decodificar o arquivo PEM.");
    }

    System.out.println("[DEBUG] Objeto File retornado. Caminho absoluto: " + decodedFile.getAbsolutePath());
    System.out.println("[DEBUG] Arquivo existe (decodedFile.exists())? " + decodedFile.exists());

    if (decodedFile.exists()) {
        System.out.println("[DEBUG] Tamanho do arquivo gerado: " + decodedFile.length() + " bytes");
        System.out.println("[DEBUG] Arquivo pode ser lido? " + decodedFile.canRead());
    } else {
        System.err.println("[ERROR] Falha ao decodificar o arquivo PEM. Arquivo não existe em: "
                + decodedFile.getAbsolutePath());
        // Aqui você provavelmente quer lançar uma exceção também,
        // já que hoje só loga e segue retornando um File que não existe.
        // throw new BilletException("Arquivo PEM decodificado não foi encontrado em disco: " + decodedFile.getAbsolutePath());
    }

    System.out.println("[DEBUG] Finalizando decodePem() com sucesso.");
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

  public File getPemFile() {
    File file = new File(pemTempDirectory + "/empresa.pem");
    return file.exists() ? file : null;
  }
}
