package com.hortifruti.sl.hortifruti.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseStorageSchedulerService {

    private final DatabaseStorageService databaseStorageService;

    /**
     * Verifica diariamente o armazenamento do banco de dados e executa a limpeza,
     * se necessário.
     * Executa todos os dias às 03:00
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledDatabaseCheck() {
        log.info("Verificação automática de armazenamento do banco de dados iniciada");

        try {
            if (databaseStorageService.isDatabaseOverThreshold()) {
                log.warn("Banco de dados excedeu o limite configurado. Iniciando limpeza...");
                databaseStorageService.cleanDatabaseIfNecessary();
            } else {
                log.info("Banco de dados dentro do limite configurado. Nenhuma ação necessária.");
            }
        } catch (Exception e) {
            log.error("Erro durante a verificação automática de armazenamento do banco de dados", e);
        }
    }
}