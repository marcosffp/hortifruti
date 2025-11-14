package com.hortifruti.sl.hortifruti.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseStorageSchedulerService {

  private final DatabaseStorageService databaseStorageService;

  /**
   * Verifica o armazenamento do banco de dados e notifica se necessário. Este método será chamado
   * manualmente via endpoint.
   */
  public void scheduledDatabaseCheck() {
    log.info("Verificação manual de armazenamento do banco de dados iniciada");

    try {
      if (databaseStorageService.isDatabaseOverThreshold()) {
        log.warn("Banco de dados excedeu o limite configurado. Enviando notificação...");
        databaseStorageService.checkDatabaseStorage();
      } else {
        log.info("Banco de dados dentro do limite configurado. Nenhuma ação necessária.");
      }
    } catch (Exception e) {
      log.error("Erro durante a verificação manual de armazenamento do banco de dados", e);
    }
  }
}
