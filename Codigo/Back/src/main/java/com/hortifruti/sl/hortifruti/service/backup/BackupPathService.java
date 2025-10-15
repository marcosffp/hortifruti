package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BackupPathService {

  private final GoogleDriveService googleDriveService;
  private static final String BACKUP_FOLDER_NAME = "backups";

  /**
   * Calcula e cria o caminho correto para o backup semanal no Google Drive.
   *
   * @return O ID da pasta de backup no Google Drive.
   */
  protected String getOrCreateBackupPath() {
    try {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        String month = String.format("%02d", now.getMonthValue());
        String date = now.toString(); // Formato yyyy-MM-dd

        // Obter ou criar pasta principal de backups
        String backupFolderId = googleDriveService.getFolderId(BACKUP_FOLDER_NAME);
        if (backupFolderId == null) {
            backupFolderId = googleDriveService.createFolder(BACKUP_FOLDER_NAME, null);
        }

        // Obter ou criar pasta do ano dentro da pasta de backups
        String yearStr = String.valueOf(year);
        String yearFolderId = googleDriveService.getFolderId(yearStr, backupFolderId);
        if (yearFolderId == null) {
            yearFolderId = googleDriveService.createFolder(yearStr, backupFolderId);
        }

        // Obter ou criar pasta do mês dentro da pasta do ano
        String monthFolderId = googleDriveService.getFolderId(month, yearFolderId);
        if (monthFolderId == null) {
            monthFolderId = googleDriveService.createFolder(month, yearFolderId);
        }

        // Obter ou criar pasta da data dentro da pasta do mês
        String dateFolderId = googleDriveService.getFolderId(date, monthFolderId);
        if (dateFolderId == null) {
            dateFolderId = googleDriveService.createFolder(date, monthFolderId);
        }

        return dateFolderId;
    } catch (Exception e) {
        throw new BackupException(
            "Erro ao calcular ou criar o caminho de backup no Google Drive.", e);
    }
  }
}
