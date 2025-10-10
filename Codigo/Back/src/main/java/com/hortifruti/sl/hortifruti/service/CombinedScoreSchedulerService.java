package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.model.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import com.hortifruti.sl.hortifruti.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CombinedScoreSchedulerService {

    @Autowired
    private CombinedScoreRepository combinedScoreRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Verifica diariamente por CombinedScores vencidos e envia notificações
     * Executa todos os dias às 09:00
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void checkOverdueCombinedScores() {
        log.info("Iniciando verificação de CombinedScores vencidos...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<CombinedScore> overdueScores = combinedScoreRepository.findOverdueUnpaidScores(now);
            
            log.info("Encontrados {} CombinedScores vencidos", overdueScores.size());
            
            if (!overdueScores.isEmpty()) {
                // Agrupa por cliente para enviar notificações em lote
                var scoresByClient = overdueScores.stream()
                    .collect(Collectors.groupingBy(CombinedScore::getClientId));
                
                log.info("Enviando notificações para {} clientes com CombinedScores vencidos", scoresByClient.size());
                
                scoresByClient.forEach((clientId, clientScores) -> {
                    try {
                        sendOverdueNotification(clientId, clientScores);
                        log.info("Notificação enviada para cliente ID: {}, {} CombinedScores vencidos", 
                               clientId, clientScores.size());
                    } catch (Exception e) {
                        log.error("Erro ao enviar notificação para cliente ID: {}", clientId, e);
                    }
                });
            }
            
        } catch (Exception e) {
            log.error("Erro durante verificação de CombinedScores vencidos", e);
        }
    }

    /**
     * Envia notificação de CombinedScores vencidos para um cliente específico
     */
    private void sendOverdueNotification(Long clientId, List<CombinedScore> overdueScores) {
        var client = clientRepository.findById(clientId);
        
        if (client.isEmpty()) {
            log.warn("Cliente com ID {} não encontrado", clientId);
            return;
        }
        
        var clientData = client.get();
        
        try {
            // Envia por email
            String subject = "⚠️ Aviso: Pagamentos em Atraso - " + clientData.getClientName();
            String emailBody = buildOverdueEmailBody(clientData.getClientName(), overdueScores);
            
            sendEmailDirectly(clientData.getEmail(), subject, emailBody);
            
            // Envia por WhatsApp
            String whatsappMessage = buildOverdueWhatsAppMessage(clientData.getClientName(), overdueScores);
            
            sendWhatsAppDirectly(clientData.getPhoneNumber(), whatsappMessage);
            
            log.info("Notificações de CombinedScores vencidos enviadas para cliente: {} ({})", 
                   clientData.getClientName(), clientData.getEmail());
                   
        } catch (Exception e) {
            log.error("Erro ao enviar notificações para cliente {}", clientData.getClientName(), e);
        }
    }

    /**
     * Método auxiliar para enviar email diretamente
     */
    private void sendEmailDirectly(String email, String subject, String body) {
        try {
            // Usar o EmailService diretamente através do NotificationService
            // Como não temos acesso direto, vamos usar reflection ou criar um método auxiliar
            var emailService = notificationService.getClass().getDeclaredField("emailService");
            emailService.setAccessible(true);
            var emailServiceInstance = emailService.get(notificationService);
            
            // Chama o método sendEmail do EmailService
            var sendEmailMethod = emailServiceInstance.getClass().getMethod("sendEmail", String.class, String.class, String.class);
            sendEmailMethod.invoke(emailServiceInstance, email, subject, body);
            
            log.info("Email enviado com sucesso para: {}", email);
        } catch (Exception e) {
            log.error("Erro ao enviar email para: {}", email, e);
        }
    }

    /**
     * Método auxiliar para enviar WhatsApp diretamente
     */
    private void sendWhatsAppDirectly(String phoneNumber, String message) {
        try {
            // Usar o WhatsAppService diretamente através do NotificationService
            var whatsAppService = notificationService.getClass().getDeclaredField("whatsAppService");
            whatsAppService.setAccessible(true);
            var whatsAppServiceInstance = whatsAppService.get(notificationService);
            
            // Chama o método sendTextMessage do WhatsAppService
            var sendTextMethod = whatsAppServiceInstance.getClass().getMethod("sendTextMessage", String.class, String.class);
            sendTextMethod.invoke(whatsAppServiceInstance, phoneNumber, message);
            
            log.info("WhatsApp enviado com sucesso para: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Erro ao enviar WhatsApp para: {}", phoneNumber, e);
        }
    }

    /**
     * Constrói o corpo do email para CombinedScores vencidos
     */
    private String buildOverdueEmailBody(String clientName, List<CombinedScore> overdueScores) {
        StringBuilder body = new StringBuilder();
        
        body.append("Prezado(a) ").append(clientName).append(",\n\n");
        body.append("Identificamos que há pagamentos em atraso em sua conta.\n\n");
        body.append("Detalhes dos pagamentos vencidos:\n\n");
        
        BigDecimal totalOverdue = BigDecimal.ZERO;
        
        for (CombinedScore score : overdueScores) {
            body.append("• Data de vencimento: ").append(score.getDueDate().toLocalDate());
            body.append(" - Valor: R$ ").append(String.format("%.2f", score.getTotalValue()));
            body.append(" (Vencido há ")
                .append(java.time.temporal.ChronoUnit.DAYS.between(score.getDueDate().toLocalDate(), LocalDateTime.now().toLocalDate()))
                .append(" dias)\n");
            
            totalOverdue = totalOverdue.add(score.getTotalValue());
        }
        
        body.append("\n📊 Total em atraso: R$ ").append(String.format("%.2f", totalOverdue)).append("\n\n");
        body.append("Para regularizar sua situação, entre em contato conosco ou acesse nosso sistema.\n\n");
        body.append("Atenciosamente,\n");
        body.append("Equipe HortiFruti SL");
        
        return body.toString();
    }

    /**
     * Constrói a mensagem do WhatsApp para CombinedScores vencidos
     */
    private String buildOverdueWhatsAppMessage(String clientName, List<CombinedScore> overdueScores) {
        StringBuilder message = new StringBuilder();
        
        message.append("🚨 *AVISO IMPORTANTE* 🚨\n\n");
        message.append("Olá *").append(clientName).append("*!\n\n");
        message.append("Identificamos pagamentos em atraso em sua conta:\n\n");
        
        BigDecimal totalOverdue = BigDecimal.ZERO;
        
        for (CombinedScore score : overdueScores) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                score.getDueDate().toLocalDate(), 
                LocalDateTime.now().toLocalDate()
            );
            
            message.append("💰 Vencimento: ").append(score.getDueDate().toLocalDate());
            message.append("\n💵 Valor: R$ ").append(String.format("%.2f", score.getTotalValue()));
            message.append("\n⏰ Atraso: ").append(daysOverdue).append(" dias\n\n");
            
            totalOverdue = totalOverdue.add(score.getTotalValue());
        }
        
        message.append("📊 *Total em atraso: R$ ").append(String.format("%.2f", totalOverdue)).append("*\n\n");
        message.append("📞 Entre em contato para regularizar sua situação.\n\n");
        message.append("_Equipe HortiFruti SL_");
        
        return message.toString();
    }

    /**
     * Método para verificação manual de CombinedScores vencidos
     */
    public void manualOverdueCheck() {
        log.info("Verificação manual de CombinedScores vencidos iniciada");
        checkOverdueCombinedScores();
    }
}