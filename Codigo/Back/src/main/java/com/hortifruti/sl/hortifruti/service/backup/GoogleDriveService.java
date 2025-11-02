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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveService {

  private static final String APPLICATION_NAME = "Hortifruti SL Backup";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "temp/google/tokens";
  private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
  private final Base64FileDecoder base64FileDecoder;

  @Value("${google.redirect.uri}")
  private String redirectUri;

  /**
   * Cria um cliente autorizado do Google Drive.
   *
   * @return Um cliente Drive autorizado.
   */
  private Drive getDriveService() {
    log.info("Iniciando criação do cliente autorizado do Google Drive.");
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      Drive drive =
          new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
              .setApplicationName(APPLICATION_NAME)
              .build();
      log.info("Cliente autorizado do Google Drive criado com sucesso.");
      return drive;
    } catch (IOException | GeneralSecurityException e) {
      log.error("Erro ao criar o cliente do Google Drive.", e);
      throw new BackupException("Erro ao criar o cliente do Google Drive.", e);
    }
  }

  /**
   * Cria credenciais de autorização para a aplicação.
   *
   * @param HTTP_TRANSPORT O transporte de rede HTTP.
   * @return Uma instância de Credential autorizada.
   */
  private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    log.info("Iniciando criação de credenciais de autorização.");
    try {
      base64FileDecoder.decodeGoogleDriveCredentials();
      java.io.File credentialsFile = base64FileDecoder.getGoogleDriveCredentialsFile();
      if (!credentialsFile.exists()) {
        log.error("Arquivo de credenciais não encontrado.");
        throw new BackupException("Arquivo de credenciais não encontrado.");
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

      log.info("Verificando se já existe uma credencial válida...");

      // Verificar se o diretório de tokens existe
      java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
      if (!tokensDir.exists()) {
        log.info("Diretório de tokens não existe. Criando: {}", TOKENS_DIRECTORY_PATH);
        tokensDir.mkdirs();
      }

      Credential credential = flow.loadCredential("user");
      if (credential != null) {
        log.info("Credencial encontrada. Verificando validade do token...");
        log.info("Access Token presente: {}", credential.getAccessToken() != null);
        log.info("Refresh Token presente: {}", credential.getRefreshToken() != null);

        // Verificar se o token de acesso está válido
        if (credential.getAccessToken() != null) {
          // Verificar se o token não expirou
          if (credential.getExpiresInSeconds() == null || credential.getExpiresInSeconds() > 60) {
            log.info("Token válido encontrado. Reutilizando credencial existente.");
            return credential;
          } else {
            log.info("Token expirado. Tentando renovar...");
            if (credential.getRefreshToken() != null && credential.refreshToken()) {
              log.info("Token renovado com sucesso.");
              return credential;
            } else {
              log.warn(
                  "Não foi possível renovar o token. Refresh token: {}",
                  credential.getRefreshToken() != null ? "Presente" : "Ausente");
            }
          }
        }
      } else {
        log.info("Nenhuma credencial encontrada no armazenamento.");
      }

      log.warn("Nenhuma credencial válida encontrada. Será necessário autenticar novamente.");

      // Se não existir credencial válida, gerar a URL e lançar exceção com a URL
      String authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();

      log.info("URL de autorização gerada: {}", authorizationUrl);
      throw new BackupException("AUTHORIZATION_REQUIRED:" + authorizationUrl);

    } catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
      log.error("Erro de autenticação no Google Drive.", e);
      handleTokenException(e, HTTP_TRANSPORT);
    } catch (IOException e) {
      log.error("Erro ao carregar as credenciais do Google Drive.", e);
      throw new BackupException("Erro ao carregar as credenciais do Google Drive.", e);
    }
    return null;
  }

  private void handleTokenException(
      com.google.api.client.auth.oauth2.TokenResponseException e, NetHttpTransport HTTP_TRANSPORT) {
    if (e.getDetails() != null && "invalid_grant".equals(e.getDetails().getError())) {
      log.error(
          "Token expirado ou revogado. Excluindo diretório de tokens e tentando novamente...");
      java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
      if (tokensDir.exists()) {
        deleteDirectory(tokensDir);
        log.info("Diretório de tokens excluído com sucesso.");
      }
      try {
        GoogleAuthorizationCodeFlow flow =
            new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT,
                    JSON_FACTORY,
                    GoogleClientSecrets.load(
                        JSON_FACTORY,
                        new InputStreamReader(
                            new FileInputStream(base64FileDecoder.getGoogleDriveCredentialsFile()),
                            StandardCharsets.UTF_8)),
                    SCOPES)
                .setDataStoreFactory(
                    new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        log.info("Credenciais recriadas com sucesso após token expirado.");
        new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
      } catch (IOException retryException) {
        log.error("Erro ao recriar credenciais após token expirado.", retryException);
        throw new BackupException(
            "Erro ao recriar credenciais após token expirado.", retryException);
      }
    }
    throw new BackupException("Erro de autenticação no Google Drive.", e);
  }

  // Método auxiliar para excluir diretório recursivamente
  private void deleteDirectory(java.io.File directory) {
    log.info("Excluindo diretório: {}", directory.getAbsolutePath());
    if (directory.isDirectory()) {
      java.io.File[] files = directory.listFiles();
      if (files != null) {
        for (java.io.File file : files) {
          deleteDirectory(file);
        }
      }
    }
    directory.delete();
    log.info("Diretório excluído: {}", directory.getAbsolutePath());
  }

  /**
   * Verifica se uma pasta existe dentro de uma pasta pai e retorna seu ID.
   *
   * @param folderName O nome da pasta para verificar.
   * @param parentFolderId ID da pasta pai (opcional).
   * @return O ID da pasta no Google Drive, ou null se não encontrada.
   */
  protected String getFolderId(String folderName, String parentFolderId) {
    log.info("Verificando existência da pasta '{}' no Google Drive.", folderName);
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
        log.info("Pasta encontrada. ID: {}", files.get(0).getId());
        return files.get(0).getId();
      }
      log.info("Pasta '{}' não encontrada.", folderName);
      return null;
    } catch (IOException e) {
      log.error("Erro ao verificar pasta no Google Drive.", e);
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
    log.info("Iniciando upload do arquivo '{}' para a pasta '{}'.", fileName, folderId);
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
      log.info("Upload concluído. ID do arquivo no Google Drive: {}", file.getId());
      return file.getId();
    } catch (IOException e) {
      log.error("Erro ao fazer upload do arquivo.", e);
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
    log.info("Criando pasta '{}' no Google Drive.", folderName);
    try {
      Drive service = getDriveService();
      File fileMetadata = new File();
      fileMetadata.setName(folderName);
      fileMetadata.setMimeType("application/vnd.google-apps.folder");

      if (parentFolderId != null && !parentFolderId.isEmpty()) {
        fileMetadata.setParents(Collections.singletonList(parentFolderId));
      }

      File folder = service.files().create(fileMetadata).setFields("id").execute();
      log.info("Pasta '{}' criada com sucesso. ID: {}", folderName, folder.getId());
      return folder.getId();
    } catch (IOException e) {
      log.error("Erro ao criar a pasta no Google Drive.", e);
      throw new BackupException("Erro ao criar a pasta no Google Drive.", e);
    }
  }

  /**
   * Verifica se as credenciais estão disponíveis para uso.
   *
   * @return true se as credenciais estiverem disponíveis, caso contrário, false.
   */
  public boolean areCredentialsAvailable() {
    log.info("Verificando se as credenciais estão disponíveis.");
    try {
      java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
      if (!tokensDir.exists() || !tokensDir.isDirectory()) {
        log.info("Diretório de tokens não existe ou não é um diretório.");
        return false;
      }

      java.io.File[] files = tokensDir.listFiles();
      if (files == null || files.length == 0) {
        log.info("Nenhum arquivo de token encontrado no diretório.");
        return false;
      }

      // Tentar carregar as credenciais para verificar se são válidas
      base64FileDecoder.decodeGoogleDriveCredentials();
      java.io.File credentialsFile = base64FileDecoder.getGoogleDriveCredentialsFile();
      if (!credentialsFile.exists()) {
        log.info("Arquivo de credenciais do Google não encontrado.");
        return false;
      }

      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
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

      Credential credential = flow.loadCredential("user");
      boolean available =
          credential != null
              && (credential.getAccessToken() != null || credential.getRefreshToken() != null);

      log.info("Credenciais disponíveis: {}", available);
      return available;

    } catch (Exception e) {
      log.error("Erro ao verificar disponibilidade das credenciais: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Obtém a URL de autorização para o Google Drive.
   *
   * @return A URL de autorização.
   */
  public String getAuthorizationUrl() {
    log.info("Obtendo URL de autorização para o Google Drive.");
    try {
      base64FileDecoder.decodeGoogleDriveCredentials();
      java.io.File credentialsFile = base64FileDecoder.getGoogleDriveCredentialsFile();
      if (!credentialsFile.exists()) {
        log.error("Arquivo de credenciais não encontrado.");
        throw new BackupException("Arquivo de credenciais não encontrado.");
      }

      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
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
      String url = flow.newAuthorizationUrl().setRedirectUri(receiver.getRedirectUri()).build();
      log.info("URL de autorização gerada com sucesso: {}", url);
      return url;
    } catch (Exception e) {
      log.error("Erro ao gerar o link de autorização do Google Drive.", e);
      throw new BackupException("Erro ao gerar o link de autorização do Google Drive.", e);
    }
  }

  /**
   * Lida com o callback de autorização do Google.
   *
   * @param authorizationCode Código de autorização recebido do Google.
   */
  public void handleOAuth2Callback(String authorizationCode) {
    log.info("Recebendo código de autorização do Google: {}", authorizationCode);
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      base64FileDecoder.decodeGoogleDriveCredentials();
      java.io.File credentialsFile = base64FileDecoder.getGoogleDriveCredentialsFile();

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

      // Corrigir: Usar o flow para trocar o código por um token e salvar as credenciais
      com.google.api.client.auth.oauth2.TokenResponse response =
          flow.newTokenRequest(authorizationCode).setRedirectUri(redirectUri).execute();

      // Salvar as credenciais usando o flow
      Credential credential = flow.createAndStoreCredential(response, "user");

      log.info("Token de autorização recebido e armazenado com sucesso.");
      log.info("Access Token: {}", credential.getAccessToken() != null ? "Presente" : "Ausente");
      log.info("Refresh Token: {}", credential.getRefreshToken() != null ? "Presente" : "Ausente");

    } catch (Exception e) {
      log.error("Erro ao processar o callback de autorização.", e);
      throw new BackupException("Erro ao processar o callback de autorização.", e);
    }
  }
}
