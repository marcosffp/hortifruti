package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
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

  /**
   * Calcula e cria o caminho correto para o backup no Google Drive com base no tipo de entidade e
   * período.
   *
   * @param entityName Nome da entidade (ex.: "Statement", "Purchase").
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   * @return O ID da pasta de backup no Google Drive.
   */
  public String getOrCreateBackupPath(String entityName, LocalDate startDate, LocalDate endDate) {

    try {
      // Formatar o período no formato "2025-06_to_2025-09"
      String period =
          startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
              + "_to_"
              + endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

      String backupFolderId = googleFolderService.getFolderId(BACKUP_FOLDER_NAME);
      if (backupFolderId == null) {

        backupFolderId = googleFolderService.createFolder(BACKUP_FOLDER_NAME, null);
      }

      // Obter ou criar a pasta do ano
      String year = String.valueOf(startDate.getYear());
      String yearFolderId = googleFolderService.getFolderId(year, backupFolderId);
      if (yearFolderId == null) {
        yearFolderId = googleFolderService.createFolder(year, backupFolderId);
      }

      // Obter ou criar a pasta do mês
      String month = String.format("%02d", startDate.getMonthValue());
      String monthFolderId = googleFolderService.getFolderId(month, yearFolderId);
      if (monthFolderId == null) {
        monthFolderId = googleFolderService.createFolder(month, yearFolderId);
      }

      // Obter ou criar a pasta da entidade com o período
      String entityFolderName = entityName + "_" + period;
      String entityFolderId = googleFolderService.getFolderId(entityFolderName, monthFolderId);
      if (entityFolderId == null) {
        entityFolderId = googleFolderService.createFolder(entityFolderName, monthFolderId);
      }

      return entityFolderId;
    } catch (BackupException e) {
      // Re-lançar exceções de autorização sem encapsular
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
