package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class BackupPathService {

  private final GoogleDriveService googleDriveService;
  private static final String BACKUP_FOLDER_NAME = "backups";

  /**
   * Calcula e cria o caminho correto para o backup no Google Drive com base no tipo de entidade e período.
   *
   * @param entityName Nome da entidade (ex.: "Statement", "Purchase").
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   * @return O ID da pasta de backup no Google Drive.
   */
  public String getOrCreateBackupPath(String entityName, LocalDate startDate, LocalDate endDate) {
    log.info("Iniciando cálculo do caminho de backup para a entidade: {} no período: {} a {}", entityName, startDate, endDate);
    try {
      // Formatar o período no formato "2025-06_to_2025-09"
      String period = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "_to_" +
                      endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      log.debug("Período formatado: {}", period);

      // Obter ou criar a pasta principal de backups
      log.info("Verificando existência da pasta principal de backups: {}", BACKUP_FOLDER_NAME);
      String backupFolderId = googleDriveService.getFolderId(BACKUP_FOLDER_NAME);
      if (backupFolderId == null) {
        log.info("Pasta principal de backups não encontrada. Criando pasta: {}", BACKUP_FOLDER_NAME);
        backupFolderId = googleDriveService.createFolder(BACKUP_FOLDER_NAME, null);
        log.info("Pasta principal de backups criada com sucesso. ID: {}", backupFolderId);
      } else {
        log.info("Pasta principal de backups encontrada. ID: {}", backupFolderId);
      }

      // Obter ou criar a pasta do ano
      String year = String.valueOf(startDate.getYear());
      log.info("Verificando existência da pasta do ano: {}", year);
      String yearFolderId = googleDriveService.getFolderId(year, backupFolderId);
      if (yearFolderId == null) {
        log.info("Pasta do ano não encontrada. Criando pasta: {}", year);
        yearFolderId = googleDriveService.createFolder(year, backupFolderId);
        log.info("Pasta do ano criada com sucesso. ID: {}", yearFolderId);
      } else {
        log.info("Pasta do ano encontrada. ID: {}", yearFolderId);
      }

      // Obter ou criar a pasta do mês
      String month = String.format("%02d", startDate.getMonthValue());
      log.info("Verificando existência da pasta do mês: {}", month);
      String monthFolderId = googleDriveService.getFolderId(month, yearFolderId);
      if (monthFolderId == null) {
        log.info("Pasta do mês não encontrada. Criando pasta: {}", month);
        monthFolderId = googleDriveService.createFolder(month, yearFolderId);
        log.info("Pasta do mês criada com sucesso. ID: {}", monthFolderId);
      } else {
        log.info("Pasta do mês encontrada. ID: {}", monthFolderId);
      }

      // Obter ou criar a pasta da entidade com o período
      String entityFolderName = entityName + "_" + period;
      log.info("Verificando existência da pasta da entidade: {}", entityFolderName);
      String entityFolderId = googleDriveService.getFolderId(entityFolderName, monthFolderId);
      if (entityFolderId == null) {
        log.info("Pasta da entidade não encontrada. Criando pasta: {}", entityFolderName);
        entityFolderId = googleDriveService.createFolder(entityFolderName, monthFolderId);
        log.info("Pasta da entidade criada com sucesso. ID: {}", entityFolderId);
      } else {
        log.info("Pasta da entidade encontrada. ID: {}", entityFolderId);
      }

      log.info("Caminho de backup calculado com sucesso. ID final da pasta: {}", entityFolderId);
      return entityFolderId;
    } catch (Exception e) {
      log.error("Erro ao calcular ou criar o caminho de backup no Google Drive para a entidade: {}. Erro: {}", entityName, e.getMessage(), e);
      throw new BackupException(
          "Erro ao calcular ou criar o caminho de backup no Google Drive para a entidade: " + entityName, e);
    }
  }
}