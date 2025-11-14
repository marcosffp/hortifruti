package com.hortifruti.sl.hortifruti.service.backup.folders;

import com.google.api.services.drive.model.File;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Component
public class FileMetadataFactory {

  /** Cria metadata para um arquivo a ser enviado. */
  protected File createFileMetadata(String fileName, String folderId) {
    File fileMetadata = new File();
    fileMetadata.setName(fileName);

    if (folderId != null && !folderId.isEmpty()) {
      fileMetadata.setParents(Collections.singletonList(folderId));
    }

    return fileMetadata;
  }

  /** Cria metadata para uma pasta. */
  protected File createFolderMetadata(String folderName, String parentFolderId) {
    File fileMetadata = new File();
    fileMetadata.setName(folderName);
    fileMetadata.setMimeType("application/vnd.google-apps.folder");

    if (parentFolderId != null && !parentFolderId.isEmpty()) {
      fileMetadata.setParents(Collections.singletonList(parentFolderId));
    }

    return fileMetadata;
  }
}
