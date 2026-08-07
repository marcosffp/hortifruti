package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.dto.backup.BackupResponse;
import com.hortifruti.sl.hortifruti.exception.backup.BackupException;
import com.hortifruti.sl.hortifruti.service.backup.folders.GoogleFolderService;
import com.hortifruti.sl.hortifruti.service.scheduler.DatabaseStorageService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class BackupService {

  private final GoogleFolderService googleFolderService;

  private final CsvGeneratorService csvGeneratorService;
  private final BackupPathService backupPathService;
  private final EntityCleanupService entityCleanupService;
  private final DatabaseStorageService databaseStorageService;

  public BackupResponse performBackupForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
    try {
      List<String> csvFiles = csvGeneratorService.generateCSVsForPeriod(startDate, endDate);

      for (String filePath : csvFiles) {
        String fileName = Paths.get(filePath).getFileName().toString();

        String folderId =
            backupPathService.getOrCreateBackupPath(
                "Backup", startDate.toLocalDate(), endDate.toLocalDate());

        googleFolderService.uploadFile(filePath, fileName, folderId);

        try {
          Files.delete(Paths.get(filePath));
        } catch (IOException e) {
          log.warn("Não foi possível deletar o arquivo temporário: {}", filePath, e);
        }
      }

      entityCleanupService.cleanupEntitiesForPeriod(startDate, endDate);

      return new BackupResponse(
          "Backup para o período " + startDate + " a " + endDate + " concluído com sucesso.");
    } catch (BackupException e) {
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

  public BackupResponse handleBackupRequest(String startDate, String endDate) {
    try {
      if (startDate != null && endDate != null) {

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
