package com.hortifruti.sl.hortifruti.service.backup.folders;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.hortifruti.sl.hortifruti.exception.BackupException;
import com.hortifruti.sl.hortifruti.service.backup.auth.GoogleAuthService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileUploader {

  private final GoogleAuthService googleAuthService;
  private final FileMetadataFactory fileMetadataFactory;

  /** Faz upload de um arquivo para o Google Drive em uma pasta específica. */
  protected String uploadFile(String filePath, String fileName, String folderId) {
    validateFileInput(filePath, fileName);

    try {
      Drive service = googleAuthService.getDriveService();
      File fileMetadata = fileMetadataFactory.createFileMetadata(fileName, folderId);
      FileContent mediaContent = createMediaContent(filePath);

      File file = service.files().create(fileMetadata, mediaContent).setFields("id").execute();
      return file.getId();
    } catch (IOException e) {
      throw new BackupException("Erro ao fazer upload do arquivo para o Google Drive.", e);
    }
  }

  /** Valida os parâmetros de entrada para upload. */
  private void validateFileInput(String filePath, String fileName) {
    if (filePath == null || filePath.trim().isEmpty()) {
      throw new IllegalArgumentException("Caminho do arquivo não pode ser vazio");
    }
    if (fileName == null || fileName.trim().isEmpty()) {
      throw new IllegalArgumentException("Nome do arquivo não pode ser vazio");
    }
  }

  /** Cria o conteúdo de mídia para upload. */
  private FileContent createMediaContent(String filePath) {
    java.io.File fileContent = new java.io.File(filePath);

    if (!fileContent.exists()) {
      throw new BackupException("Arquivo não encontrado: " + filePath);
    }
    if (!fileContent.canRead()) {
      throw new BackupException("Sem permissão para ler o arquivo: " + filePath);
    }

    return new FileContent("text/csv", fileContent);
  }
}
