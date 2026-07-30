package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.service.backup.folders.GoogleFolderService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BackupPathService {

  private static final String BACKUP_FOLDER_NAME = "backups";
  private final GoogleFolderService googleFolderService;

  public String getOrCreateBackupPath(String entityName, LocalDate startDate, LocalDate endDate) {

    try {
      String period =
          startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
              + "_to_"
              + endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

      String backupFolderId = googleFolderService.getFolderId(BACKUP_FOLDER_NAME);
      if (backupFolderId == null) {

        backupFolderId = googleFolderService.createFolder(BACKUP_FOLDER_NAME, null);
      }

      String year = String.valueOf(startDate.getYear());
      String yearFolderId = googleFolderService.getFolderId(year, backupFolderId);
      if (yearFolderId == null) {
        yearFolderId = googleFolderService.createFolder(year, backupFolderId);
      }

      String month = String.format("%02d", startDate.getMonthValue());
      String monthFolderId = googleFolderService.getFolderId(month, yearFolderId);
      if (monthFolderId == null) {
        monthFolderId = googleFolderService.createFolder(month, yearFolderId);
      }

      String entityFolderName = entityName + "_" + period;
      String entityFolderId = googleFolderService.getFolderId(entityFolderName, monthFolderId);
      if (entityFolderId == null) {
        entityFolderId = googleFolderService.createFolder(entityFolderName, monthFolderId);
      }

      return entityFolderId;
    } catch (BackupException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("AUTHORIZATION_REQUIRED:")) {
        throw e;
      }
      throw new BackupException(
          "Erro ao calcular ou criar o caminho de backup no Google Drive para a entidade: "
              + entityName,
          e);
    } catch (Exception e) {
      throw new BackupException(
          "Erro ao calcular ou criar o caminho de backup no Google Drive para a entidade: "
              + entityName,
          e);
    }
  }
}
