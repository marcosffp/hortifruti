package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class BilletSchedulerService {

  private final NotificationService notificationService;

  /**
   * Executa verificação de boletos vencidos todos os dias às 09:00
   * Para notificar gerente sobre boletos que venceram no dia anterior
   */
  @Scheduled(cron = "0 0 9 * * ?") // Todo dia às 09:00
  public void checkOverdueBilletsDaily() {
    log.info("Iniciando verificação automática de boletos vencidos - {}", LocalDateTime.now());
    
    try {
      NotificationResponse response = notificationService.checkAndNotifyOverdueBillets();
      
      if (response.success()) {
        log.info("Verificação de boletos vencidos concluída com sucesso: {}", response.message());
      } else {
        log.error("Erro na verificação de boletos vencidos: {}", response.message());
      }
      
    } catch (Exception e) {
      log.error("Erro inesperado na verificação automática de boletos vencidos", e);
    }
  }

  /**
   * Executa verificação manual de boletos vencidos
   * Pode ser chamado via endpoint ou outras partes do sistema
   */
  public NotificationResponse executeManualCheck() {
    log.info("Executando verificação manual de boletos vencidos - {}", LocalDateTime.now());
    
    try {
      return notificationService.checkAndNotifyOverdueBillets();
    } catch (Exception e) {
      log.error("Erro na verificação manual de boletos vencidos", e);
      return new NotificationResponse(
          false, 
          "Erro na verificação manual: " + e.getMessage(),
          "ERRO",
          "ERRO"
      );
    }
  }

  /**
   * Para teste: executa a cada 5 minutos durante desenvolvimento
   * Desabilitado por padrão - deve ser habilitado apenas para testes
   */
  @Scheduled(cron = "0 */5 * * * ?") // A cada 5 minutos
  @ConditionalOnProperty(name = "scheduler.test.enabled", havingValue = "true")
  public void testCheckOverdueBillets() {
    log.info("TESTE - Verificação de boletos vencidos a cada 5 minutos - {}", LocalDateTime.now());
    checkOverdueBilletsDaily();
  }
}