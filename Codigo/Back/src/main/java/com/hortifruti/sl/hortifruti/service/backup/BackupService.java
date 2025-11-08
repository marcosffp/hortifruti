package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.dto.BackupResponse;
import com.hortifruti.sl.hortifruti.exception.BackupException;
import com.hortifruti.sl.hortifruti.service.backup.folders.GoogleFolderService;
import com.hortifruti.sl.hortifruti.service.scheduler.DatabaseStorageService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BackupService {

  private final GoogleFolderService googleFolderService;

  private final CsvGeneratorService csvGeneratorService;
  private final BackupPathService backupPathService;
  private final EntityCleanupService entityCleanupService;
  private final DatabaseStorageService databaseStorageService;

  /**
   * Realiza o backup completo para um período especificado.
   *
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   * @return Mensagem de sucesso ou erro.
   */
  public BackupResponse performBackupForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      // Gerar arquivos CSV
      List<String> csvFiles = csvGeneratorService.generateCSVsForPeriod(startDate, endDate);

      // Fazer upload dos arquivos
      for (String filePath : csvFiles) {
        String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);

        String folderId =
            backupPathService.getOrCreateBackupPath(
                "Backup", startDate.toLocalDate(), endDate.toLocalDate());

        googleFolderService.uploadFile(filePath, fileName, folderId);

        try {
          Files.delete(Paths.get(filePath));
        } catch (IOException e) {
          System.out.println("Não foi possível deletar o arquivo temporário: " + filePath);
        }
      }

      entityCleanupService.cleanupEntitiesForPeriod(startDate, endDate);

      return new BackupResponse(
          "Backup para o período " + startDate + " a " + endDate + " concluído com sucesso.");
    } catch (BackupException e) {
      // Re-lançar exceções de autorização para serem tratadas no método
      // handleBackupRequestWithAuthLink
      if (e.getMessage() != null && e.getMessage().startsWith("AUTHORIZATION_REQUIRED:")) {
        throw e;
      }
      throw new BackupException(
          "Erro ao executar o backup para o período: " + startDate + " a " + endDate, e);
    } catch (Exception e) {
      throw new BackupException(
          "Erro ao executar o backup para o período: " + startDate + " a " + endDate, e);
    }
  }

  /**
   * Realiza o backup para um período especificado.
   *
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   * @return Mensagem de sucesso ou erro.
   */
  public BackupResponse handleBackupRequest(String startDate, String endDate) {
    try {
      if (startDate != null && endDate != null) {

        // Ajusta o formato das datas para evitar duplicação
        String formattedStartDate = startDate.contains("T") ? startDate : startDate + "T00:00:00";
        String formattedEndDate = endDate.contains("T") ? endDate : endDate + "T23:59:59";

        LocalDateTime start = LocalDateTime.parse(formattedStartDate);
        LocalDateTime end = LocalDateTime.parse(formattedEndDate);

        return performBackupForPeriod(start, end);
      }
    } catch (Exception e) {
      throw new BackupException("Erro ao processar a solicitação de backup: " + e.getMessage(), e);
    }
    return new BackupResponse("Backup não realizado: parâmetros inválidos ou erro desconhecido.");
  }

  /**
   * Realiza o backup para um período especificado com link de autenticação.
   *
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   * @return Mensagem de sucesso ou erro.
   */
  public BackupResponse handleBackupRequestWithAuthLink(String startDate, String endDate) {
    try {
      if (startDate != null && endDate != null) {

        String formattedStartDate = startDate.contains("T") ? startDate : startDate + "T00:00:00";
        String formattedEndDate = endDate.contains("T") ? endDate : endDate + "T23:59:59";

        LocalDateTime start = LocalDateTime.parse(formattedStartDate);
        LocalDateTime end = LocalDateTime.parse(formattedEndDate);

        return performBackupForPeriod(start, end);
      }
    } catch (BackupException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("AUTHORIZATION_REQUIRED:")) {
        String authUrl = e.getMessage().substring("AUTHORIZATION_REQUIRED:".length());
        return new BackupResponse(authUrl);
      }
      throw e;
    } catch (Exception e) {
      throw new BackupException("Erro ao processar a solicitação de backup: " + e.getMessage(), e);
    }
    return new BackupResponse("Backup não realizado: parâmetros inválidos ou erro desconhecido.");
  }

  public BigDecimal getDatabaseSizeInMB() {
    return databaseStorageService.getDatabaseSizeInMB();
  }

  public BigDecimal getMaxDatabaseSizeInMB() {
    return databaseStorageService.getMaxStorageInMB();
  }
}
