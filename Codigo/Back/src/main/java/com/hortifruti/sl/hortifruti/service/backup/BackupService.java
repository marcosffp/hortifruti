package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import java.util.List;
import lombok.AllArgsConstructor;
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

  /**
   * Realiza o backup acionado pelo GitHub Actions.
   *
   * @return mensagem de sucesso ou erro
   */
  public String performSchedulerBackup() {
    try {
      performBackup();
      return "Backup acionado pelo Scheduler concluído com sucesso";
    } catch (BackupException e) {
      throw new BackupException("Erro ao executar backup via Scheduler: " + e.getMessage(), e);
    }
  }

  /** Executa o processo de backup. */
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
}
