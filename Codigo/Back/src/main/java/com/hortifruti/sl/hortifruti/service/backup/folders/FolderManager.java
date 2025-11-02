package com.hortifruti.sl.hortifruti.service.backup.folders;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.hortifruti.sl.hortifruti.exception.BackupException;
import com.hortifruti.sl.hortifruti.service.backup.auth.GoogleAuthService;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FolderManager {

  private final GoogleAuthService googleAuthService;
  private final DriveQueryBuilder queryBuilder;

  /** Verifica se uma pasta existe dentro de uma pasta pai e retorna seu ID. */
  protected String getFolderId(String folderName, String parentFolderId) {
    try {
      Drive service = googleAuthService.getDriveService();
      String query = queryBuilder.buildFolderQuery(folderName, parentFolderId);

      FileList result =
          service
              .files()
              .list()
              .setQ(query)
              .setSpaces("drive")
              .setFields("files(id, name)")
              .execute();

      return extractFolderIdFromResult(result, folderName);
    } catch (IOException e) {
      throw new BackupException("Erro ao verificar pasta no Google Drive.", e);
    }
  }

  /** Método de compatibilidade para chamadas existentes. */
  protected String getFolderId(String folderPath) {
    String folderName = extractFolderNameFromPath(folderPath);
    return getFolderId(folderName, null);
  }

  /** Cria uma nova pasta no Google Drive. */
  protected String createFolder(String folderName, String parentFolderId) {
    try {
      Drive service = googleAuthService.getDriveService();
      File fileMetadata = createFolderMetadata(folderName, parentFolderId);

      File folder = service.files().create(fileMetadata).setFields("id").execute();
      return folder.getId();
    } catch (IOException e) {
      throw new BackupException("Erro ao criar a pasta no Google Drive.", e);
    }
  }

  /** Extrai o ID da pasta do resultado da consulta. */
  private String extractFolderIdFromResult(FileList result, String folderName) {
    List<File> files = result.getFiles();
    if (!files.isEmpty()) {
      String folderId = files.get(0).getId();
      return folderId;
    }
    return null;
  }

  /** Extrai o nome da pasta do caminho completo. */
  private String extractFolderNameFromPath(String folderPath) {
    if (folderPath.contains("/")) {
      return folderPath.substring(folderPath.lastIndexOf("/") + 1);
    }
    return folderPath;
  }

  /** Cria metadata para uma pasta. */
  private File createFolderMetadata(String folderName, String parentFolderId) {
    File fileMetadata = new File();
    fileMetadata.setName(folderName);
    fileMetadata.setMimeType("application/vnd.google-apps.folder");

    if (parentFolderId != null && !parentFolderId.isEmpty()) {
      fileMetadata.setParents(Collections.singletonList(parentFolderId));
    }

    return fileMetadata;
  }
}
