package com.hortifruti.sl.hortifruti.service.backup;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.BackupException;

import io.netty.handler.codec.base64.Base64Decoder;
import lombok.AllArgsConstructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GoogleDriveService {

  private static final String APPLICATION_NAME = "Hortifruti SL Backup";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";
  private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
  private final Base64FileDecoder base64FileDecoder;

  /**
   * Cria um cliente autorizado do Google Drive.
   *
   * @return Um cliente Drive autorizado.
   */
  private Drive getDriveService() {
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
          .setApplicationName(APPLICATION_NAME)
          .build();
    } catch (IOException | GeneralSecurityException e) {
      throw new BackupException("Erro ao criar o cliente do Google Drive.", e);
    }
  }

  /**
   * Cria credenciais de autorização para a aplicação.
   *
   * @param HTTP_TRANSPORT O transporte de rede HTTP.
   * @return Uma instância de Credential autorizada.
   */
  private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) {
    try {
      base64FileDecoder.decodeGoogleDriveCredentials();
      java.io.File credentialsFile = base64FileDecoder.getGoogleDriveCredentialsFile();
      if (!credentialsFile.exists()) {
        throw new BackupException("Arquivo de credenciais não encontrado: ");
      }

      GoogleClientSecrets clientSecrets =
          GoogleClientSecrets.load(
              JSON_FACTORY,
              new InputStreamReader(new FileInputStream(credentialsFile), StandardCharsets.UTF_8));

      GoogleAuthorizationCodeFlow flow =
          new GoogleAuthorizationCodeFlow.Builder(
                  HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
              .setDataStoreFactory(
                  new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
              .setAccessType("offline")
              .build();
      LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
      return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    } catch (IOException e) {
      throw new BackupException("Erro ao carregar as credenciais do Google Drive.", e);
    }
  }

  /**
   * Verifica se uma pasta existe dentro de uma pasta pai e retorna seu ID.
   *
   * @param folderName O nome da pasta para verificar.
   * @param parentFolderId ID da pasta pai (opcional).
   * @return O ID da pasta no Google Drive, ou null se não encontrada.
   */
  protected String getFolderId(String folderName, String parentFolderId) {
    try {
      Drive service = getDriveService();
      String query =
          "mimeType='application/vnd.google-apps.folder' and name='"
              + folderName
              + "' and trashed=false";

      // Adicionar condição de pasta pai se especificada
      if (parentFolderId != null && !parentFolderId.isEmpty()) {
        query += " and '" + parentFolderId + "' in parents";
      }

      FileList result =
          service
              .files()
              .list()
              .setQ(query)
              .setSpaces("drive")
              .setFields("files(id, name)")
              .execute();

      List<File> files = result.getFiles();
      if (!files.isEmpty()) {
        return files.get(0).getId();
      }
      return null;
    } catch (IOException e) {
      throw new BackupException("Erro ao verificar pasta no Google Drive.", e);
    }
  }

  /**
   * Método de compatibilidade para chamadas existentes. Busca pastas pelo nome sem considerar
   * estrutura hierárquica.
   */
  protected String getFolderId(String folderPath) {
    // Extrair o nome da pasta do caminho
    String folderName = folderPath;
    if (folderPath.contains("/")) {
      folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
    }

    return getFolderId(folderName, null);
  }

  /**
   * Faz upload de um arquivo para o Google Drive em uma pasta específica.
   *
   * @param filePath Caminho local do arquivo para upload.
   * @param fileName Nome que o arquivo terá no Drive.
   * @param folderId ID da pasta no Drive onde o arquivo será salvo.
   * @return O ID do arquivo criado no Drive.
   */
  protected String uploadFile(String filePath, String fileName, String folderId) {
    try {
      Drive service = getDriveService();

      File fileMetadata = new File();
      fileMetadata.setName(fileName);
      if (folderId != null && !folderId.isEmpty()) {
        fileMetadata.setParents(Collections.singletonList(folderId));
      }

      java.io.File filecontent = new java.io.File(filePath);
      FileContent mediaContent = new FileContent("text/csv", filecontent);

      File file = service.files().create(fileMetadata, mediaContent).setFields("id").execute();

      return file.getId();
    } catch (IOException e) {
      throw new BackupException("Erro ao fazer upload do arquivo para o Google Drive.", e);
    }
  }

  /**
   * Cria uma nova pasta no Google Drive.
   *
   * @param folderName Nome da pasta a ser criada.
   * @param parentFolderId ID da pasta pai (ou null para criar na raiz).
   * @return O ID da pasta criada.
   */
  protected String createFolder(String folderName, String parentFolderId) {
    try {
      Drive service = getDriveService();

      File fileMetadata = new File();
      fileMetadata.setName(folderName);
      fileMetadata.setMimeType("application/vnd.google-apps.folder");

      if (parentFolderId != null && !parentFolderId.isEmpty()) {
        fileMetadata.setParents(Collections.singletonList(parentFolderId));
      }

      File folder = service.files().create(fileMetadata).setFields("id").execute();

      return folder.getId();
    } catch (IOException e) {
      throw new BackupException("Erro ao criar a pasta no Google Drive.", e);
    }
  }
}
