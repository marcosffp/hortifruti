package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.model.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import com.hortifruti.sl.hortifruti.service.notification.EmailTemplateService;
import com.hortifruti.sl.hortifruti.service.notification.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CombinedScoreSchedulerService {

    @Autowired
    private CombinedScoreRepository combinedScoreRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private EmailService emailService;

    @Value("${overdue.notification.emails}")
    private String overdueNotificationEmails;

    /**
     * Verifica diariamente por CombinedScores vencidos e envia notificações
     * Executa todos os dias às 07:00
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void scheduledOverdueCheck() {
        log.info("Verificação automática de CombinedScores vencidos iniciada");
        checkOverdueCombinedScores();
    }

    /**
     * Método público para verificação manual de CombinedScores vencidos
     * Pode ser chamado via endpoint ou programaticamente
     */
    public List<CombinedScore> manualOverdueCheck() {
        log.info("Verificação manual de CombinedScores vencidos iniciada");
        return checkOverdueCombinedScores();
    }

    /**
     * Lógica principal para verificação de CombinedScores vencidos
     * Separada para permitir uso tanto automático quanto manual
     */
    public List<CombinedScore> checkOverdueCombinedScores() {
        log.info("Iniciando verificação de CombinedScores vencidos...");
        
        try {
            // Usa o início do dia atual para comparação
            // Um boleto com dueDate = "2025-01-11" será considerado vencido a partir de "2025-01-12 00:00:00"
            LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
            List<CombinedScore> overdueScores = combinedScoreRepository.findOverdueUnpaidScores(startOfToday);
            
            log.info("Encontrados {} CombinedScores vencidos", overdueScores.size());
            
            if (!overdueScores.isEmpty()) {
                // Agrupa por cliente para enviar notificações em lote
                var scoresByClient = overdueScores.stream()
                    .collect(Collectors.groupingBy(CombinedScore::getClientId));
                
                log.info("Enviando notificações para {} clientes com CombinedScores vencidos", scoresByClient.size());
                
                // Envia apenas resumo para os emails de notificação de vencimento
                // (Não enviamos mais notificações diretas para clientes)
                try {
                    sendOverdueSummaryToManagement(overdueScores);
                } catch (Exception e) {
                    log.error("Erro ao enviar resumo de CombinedScores vencidos para gerência", e);
                }
            }
            
            return overdueScores;
            
        } catch (Exception e) {
            log.error("Erro durante verificação de CombinedScores vencidos", e);
            throw new RuntimeException("Erro ao verificar CombinedScores vencidos", e);
        }
    }

    /**
     * Busca apenas os CombinedScores vencidos sem enviar notificações
     * Útil para consultas e testes
     */
    public List<CombinedScore> getOverdueScoresOnly() {
        log.info("Buscando CombinedScores vencidos sem enviar notificações...");
        
        try {
            // Usa o início do dia atual para comparação
            LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
            log.info("Buscando boletos com dueDate < {} e confirmedAt IS NULL", startOfToday);
            
            List<CombinedScore> overdueScores = combinedScoreRepository.findOverdueUnpaidScores(startOfToday);
            
            log.info("Encontrados {} CombinedScores vencidos", overdueScores.size());
            
            // Log detalhado para debug
            if (overdueScores.isEmpty()) {
                log.info("Nenhum boleto vencido não pago encontrado. Critérios: dueDate < {} AND confirmedAt IS NULL", startOfToday);
            } else {
                overdueScores.forEach(score -> 
                    log.info("Boleto vencido encontrado - ID: {}, ClientID: {}, DueDate: {}, ConfirmedAt: {}", 
                        score.getId(), score.getClientId(), score.getDueDate(), score.getConfirmedAt()));
            }
            
            return overdueScores;
            
        } catch (Exception e) {
            log.error("Erro ao buscar CombinedScores vencidos", e);
            throw new RuntimeException("Erro ao buscar CombinedScores vencidos", e);
        }
    }


    /**
     * Envia resumo dos CombinedScores vencidos para os emails de notificação configurados
     */
    private void sendOverdueSummaryToManagement(List<CombinedScore> overdueScores) {
        if (overdueNotificationEmails == null || overdueNotificationEmails.trim().isEmpty()) {
            log.warn("Nenhum email configurado para notificações de CombinedScores vencidos");
            return;
        }

        String[] emails = overdueNotificationEmails.split(",");
        String subject = "Relatório de Boletos Vencidos - " + LocalDateTime.now().toLocalDate();
        String emailBody = buildManagementOverdueHtmlBody(overdueScores);

        for (String email : emails) {
            try {
                sendHtmlEmailDirectly(email.trim(), subject, emailBody);
                log.info("Resumo de CombinedScores vencidos enviado: SUCESSO");
            } catch (Exception e) {
                log.error("Erro ao enviar resumo de CombinedScores vencidos: FALHA", e);
            }
        }
    }

    /**
     * Método auxiliar para enviar email HTML diretamente
     */
    private void sendHtmlEmailDirectly(String email, String subject, String htmlBody) {
        try {
            log.info("Tentando enviar email HTML de boletos vencidos");
            
            // Usar o EmailService diretamente (injetado via @Autowired)
            // O método sendEmailWithAttachments suporta HTML (setText com true)
            boolean success = emailService.sendEmailWithAttachments(
                email, 
                subject, 
                htmlBody, 
                null, // sem anexos
                null  // sem nomes de arquivo
            );
            
            if (success) {
                log.info("Email HTML de boletos vencidos enviado: SUCESSO");
            } else {
                log.warn("Email HTML de boletos vencidos enviado: FALHA");
            }
            
        } catch (Exception e) {
            log.error("Erro ao enviar email HTML de boletos vencidos: FALHA", e);
            
            // Como fallback, log apenas informações básicas sem dados sensíveis
            log.info("Conteúdo do email seria enviado com subject: {} e {} caracteres de body", 
                subject, htmlBody.length());
        }
    }

    /**
     * Constrói o corpo do email HTML de resumo gerencial para CombinedScores vencidos
     */
    private String buildManagementOverdueHtmlBody(List<CombinedScore> overdueScores) {
        try {
            // Agrupa por cliente para o resumo
            var scoresByClient = overdueScores.stream()
                .collect(Collectors.groupingBy(CombinedScore::getClientId));
            
            BigDecimal totalOverdueAmount = overdueScores.stream()
                .map(CombinedScore::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Prepara as variáveis para o template
            Map<String, String> variables = new java.util.HashMap<>();
            variables.put("REPORT_DATE", LocalDateTime.now().toLocalDate().toString());
            variables.put("TOTAL_CLIENTS", String.valueOf(scoresByClient.size()));
            variables.put("TOTAL_OVERDUE_BOLETOS", String.valueOf(overdueScores.size()));
            variables.put("TOTAL_OVERDUE_AMOUNT", String.format("%.2f", totalOverdueAmount));
            
            // Constrói as linhas da tabela de clientes
            StringBuilder clientRows = new StringBuilder();
            
            scoresByClient.forEach((clientId, clientScores) -> {
                var client = clientRepository.findById(clientId);
                String clientName = client.map(c -> c.getClientName()).orElse("Cliente ID: " + clientId);
                
                BigDecimal clientTotal = clientScores.stream()
                    .map(CombinedScore::getTotalValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                clientRows.append("<tr>");
                clientRows.append("<td class=\"client-name\">").append(clientName).append("</td>");
                
                // Lógica para mostrar a quantidade de boletos
                if (clientScores.size() == 1) {
                    clientRows.append("<td><span class=\"boleto-count\">1 boleto</span></td>");
                } else {
                    clientRows.append("<td><span class=\"boleto-count\">").append(clientScores.size()).append(" boletos</span></td>");
                }
                
                clientRows.append("<td class=\"amount\">R$ ").append(String.format("%.2f", clientTotal)).append("</td>");
                clientRows.append("</tr>");
            });
            
            variables.put("CLIENT_ROWS", clientRows.toString());
            
            // Usa o EmailTemplateService para processar o template
            return emailTemplateService.processTemplate("overdue-management", variables);
            
        } catch (Exception e) {
            log.error("Erro ao construir email HTML de CombinedScores vencidos", e);
            // Fallback para texto simples
            return buildManagementOverdueFallbackBody(overdueScores);
        }
    }

    /**
     * Constrói um corpo de email simples como fallback
     */
    private String buildManagementOverdueFallbackBody(List<CombinedScore> overdueScores) {
        var scoresByClient = overdueScores.stream()
            .collect(Collectors.groupingBy(CombinedScore::getClientId));
        
        BigDecimal totalOverdueAmount = overdueScores.stream()
            .map(CombinedScore::getTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        StringBuilder body = new StringBuilder();
        body.append("<h2> Relatório de Boletos Vencidos</h2>");
        body.append("<p><strong>Data:</strong> ").append(LocalDateTime.now().toLocalDate()).append("</p>");
        body.append("<br>");
        body.append("<p><strong>Resumo:</strong></p>");
        body.append("<ul>");
        body.append("<li>").append(scoresByClient.size()).append(" clientes com boletos vencidos</li>");
        body.append("<li>").append(overdueScores.size()).append(" boletos em atraso</li>");
        body.append("<li>Valor total: R$ ").append(String.format("%.2f", totalOverdueAmount)).append("</li>");
        body.append("</ul>");
        body.append("<br>");
        body.append("<p><strong>Clientes:</strong></p>");
        body.append("<ul>");
        
        scoresByClient.forEach((clientId, clientScores) -> {
            var client = clientRepository.findById(clientId);
            String clientName = client.map(c -> c.getClientName()).orElse("Cliente ID: " + clientId);
            
            BigDecimal clientTotal = clientScores.stream()
                .map(CombinedScore::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            body.append("<li>").append(clientName).append(": ");
            if (clientScores.size() == 1) {
                body.append("1 boleto");
            } else {
                body.append(clientScores.size()).append(" boletos");
            }
            body.append(" - R$ ").append(String.format("%.2f", clientTotal)).append("</li>");
        });
        
        body.append("</ul>");
        body.append("<br><p>Para mais detalhes, acesse o painel administrativo do sistema.</p>");
        
        return body.toString();
    }
}