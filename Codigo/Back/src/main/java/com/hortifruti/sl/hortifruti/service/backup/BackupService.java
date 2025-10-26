package com.hortifruti.sl.hortifruti.service.backup;

import com.hortifruti.sl.hortifruti.exception.BackupException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class BackupService {

  private final CsvGeneratorService csvGeneratorService;
  private final GoogleDriveService googleDriveService;
  private final BackupPathService backupPathService;
  private final EntityCleanupService entityCleanupService;

  /**
   * Realiza o backup completo para um período especificado.
   *
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   * @return Mensagem de sucesso ou erro.
   */
  public String performBackupForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
    log.info("Iniciando backup para o período: {} a {}", startDate, endDate);
    try {
      // Gerar arquivos CSV
      log.info("Gerando arquivos CSV...");
      List<String> csvFiles = csvGeneratorService.generateCSVsForPeriod(startDate, endDate);
      log.info("Arquivos CSV gerados com sucesso. Lista de arquivos: {}", csvFiles);

      // Fazer upload dos arquivos
      for (String filePath : csvFiles) {
        String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
        log.info("Preparando upload do arquivo: {}", fileName);

        String folderId =
            backupPathService.getOrCreateBackupPath(
                "Backup", startDate.toLocalDate(), endDate.toLocalDate());
        log.info(
            "Pasta de destino no Google Drive obtida/criada com sucesso. Folder ID: {}", folderId);

        googleDriveService.uploadFile(filePath, fileName, folderId);
        log.info("Upload concluído com sucesso para o arquivo: {}", fileName);

        // Remover o arquivo temporário após o upload
        try {
          Files.delete(Paths.get(filePath));
          log.info("Arquivo temporário removido com sucesso: {}", filePath);
        } catch (IOException e) {
          log.warn(
              "Não foi possível remover o arquivo temporário: {}. Erro: {}",
              filePath,
              e.getMessage());
        }
      }

      // Remover entidades do banco
      log.info(
          "Iniciando remoção de entidades do banco de dados para o período: {} a {}",
          startDate,
          endDate);
      entityCleanupService.cleanupEntitiesForPeriod(startDate, endDate);
      log.info("Entidades removidas com sucesso do banco de dados.");

      return "Backup para o período " + startDate + " a " + endDate + " concluído com sucesso.";
    } catch (Exception e) {
      log.error("Erro ao executar o backup: {}", e.getMessage(), e);
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
  public String handleBackupRequest(String startDate, String endDate) {
    log.info("Recebendo solicitação de backup com startDate: {} e endDate: {}", startDate, endDate);
    try {
      if (startDate != null && endDate != null) {
        log.info("Tentando converter startDate e endDate para LocalDateTime...");

        // Ajusta o formato das datas para evitar duplicação
        String formattedStartDate = startDate.contains("T") ? startDate : startDate + "T00:00:00";
        String formattedEndDate = endDate.contains("T") ? endDate : endDate + "T23:59:59";

        log.debug(
            "Datas formatadas: startDate = {}, endDate = {}", formattedStartDate, formattedEndDate);

        LocalDateTime start = LocalDateTime.parse(formattedStartDate);
        LocalDateTime end = LocalDateTime.parse(formattedEndDate);

        log.info("Datas convertidas com sucesso: startDate = {}, endDate = {}", start, end);
        return performBackupForPeriod(start, end);
      } else {
        log.warn(
            "Parâmetros startDate ou endDate estão nulos. startDate: {}, endDate: {}",
            startDate,
            endDate);
      }
    } catch (Exception e) {
      log.error("Erro ao processar a solicitação de backup: {}", e.getMessage(), e);
      throw new BackupException("Erro ao processar a solicitação de backup: " + e.getMessage(), e);
    }
    return "Backup não realizado: parâmetros inválidos ou erro desconhecido.";
  }

  /**
   * Realiza o backup para um período especificado com link de autenticação.
   *
   * @param startDate Data inicial do período.
   * @param endDate Data final do período.
   * @return Mensagem de sucesso ou erro.
   */
  public String handleBackupRequestWithAuthLink(String startDate, String endDate) {
    log.info("Recebendo solicitação de backup com startDate: {} e endDate: {}", startDate, endDate);
    try {
      // Verificar se as credenciais estão disponíveis
      if (!googleDriveService.areCredentialsAvailable()) {
        log.info("Credenciais do Google Drive não disponíveis. Gerando link de autenticação...");
        String authLink = googleDriveService.getAuthorizationUrl();
        log.info("Link de autenticação gerado: {}", authLink);
        return "As credenciais do Google Drive não estão configuradas. Autorize o acesso usando o link: "
            + authLink;
      }

      // Continuar com o backup se as credenciais estiverem disponíveis
      if (startDate != null && endDate != null) {
        log.info("Tentando converter startDate e endDate para LocalDateTime...");

        // Ajusta o formato das datas para evitar duplicação
        String formattedStartDate = startDate.contains("T") ? startDate : startDate + "T00:00:00";
        String formattedEndDate = endDate.contains("T") ? endDate : endDate + "T23:59:59";

        log.debug(
            "Datas formatadas: startDate = {}, endDate = {}", formattedStartDate, formattedEndDate);

        LocalDateTime start = LocalDateTime.parse(formattedStartDate);
        LocalDateTime end = LocalDateTime.parse(formattedEndDate);

        log.info("Datas convertidas com sucesso: startDate = {}, endDate = {}", start, end);
        return performBackupForPeriod(start, end);
      } else {
        log.warn(
            "Parâmetros startDate ou endDate estão nulos. startDate: {}, endDate: {}",
            startDate,
            endDate);
      }
    } catch (Exception e) {
      log.error("Erro ao processar a solicitação de backup: {}", e.getMessage(), e);
      throw new BackupException("Erro ao processar a solicitação de backup: " + e.getMessage(), e);
    }
    return "Backup não realizado: parâmetros inválidos ou erro desconhecido.";
  }
}
