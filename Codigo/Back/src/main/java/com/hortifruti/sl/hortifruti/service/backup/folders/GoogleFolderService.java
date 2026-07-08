package com.hortifruti.sl.hortifruti.service.backup.folders;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleFolderService {

  private final FolderManager folderManager;
  private final FileUploader fileUploader;

  public String getFolderId(String folderName, String parentFolderId) {
    return folderManager.getFolderId(folderName, parentFolderId);
  }

  /** Método de compatibilidade para chamadas existentes. */
  public String getFolderId(String folderPath) {
    return folderManager.getFolderId(folderPath);
  }

  public String uploadFile(String filePath, String fileName, String folderId) {
    return fileUploader.uploadFile(filePath, fileName, folderId);
  }

  public String createFolder(String folderName, String parentFolderId) {
    return folderManager.createFolder(folderName, parentFolderId);
  }
}
