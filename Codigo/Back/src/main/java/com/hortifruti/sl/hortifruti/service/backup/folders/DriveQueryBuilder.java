package com.hortifruti.sl.hortifruti.service.backup.folders;

import org.springframework.stereotype.Component;

@Component
public class DriveQueryBuilder {

  protected String buildFolderQuery(String folderName, String parentFolderId) {
    StringBuilder query = new StringBuilder();

    query
        .append("mimeType='application/vnd.google-apps.folder'")
        .append(" and name='")
        .append(escapeQueryValue(folderName))
        .append("'")
        .append(" and trashed=false");

    if (parentFolderId != null && !parentFolderId.isEmpty()) {
      query.append(" and '").append(parentFolderId).append("' in parents");
    }

    return query.toString();
  }

  private String escapeQueryValue(String value) {
    return value.replace("'", "\\'");
  }
}
