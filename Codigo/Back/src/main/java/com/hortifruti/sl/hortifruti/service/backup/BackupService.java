package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BackupService {

  private final CsvGeneratorService csvGeneratorService;
  private final GoogleDriveService googleDriveService;
  private final BackupPathService backupPathService;

  /**
   * Realiza o backup manual quando solicitado.
   *
   * @return mensagem de sucesso ou erro
   */
  public String performManualBackup() {
    try {
      performBackup();
      return "Backup concluído com sucesso";
    } catch (BackupException e) {
      throw new BackupException("Erro ao executar backup manual: " + e.getMessage(), e);
    }
  }

  /** Executa o processo de backup semanal. */
  private void performBackup() {
    try {

      // Obter o caminho da pasta de backup no Google Drive
      String folderId = backupPathService.getOrCreateBackupPath();

      // Gerar arquivos CSV
      List<String> csvFiles = csvGeneratorService.generateAllCSVs();

      // Fazer upload dos arquivos para o Google Drive
      for (String filePath : csvFiles) {
        String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
        googleDriveService.uploadFile(filePath, fileName, folderId);
      }

    } catch (Exception e) {
      throw new BackupException("Erro ao executar o processo de backup.", e);
    }
  }

  @Scheduled(cron = "0 0 0 * * MON") // Executa toda segunda-feira à meia-noite
  public void performWeeklyBackup() {
    try {
      performBackup();
    } catch (BackupException e) {
      throw new BackupException("Erro ao executar backup semanal: " + e.getMessage(), e);
    }
  }
}
