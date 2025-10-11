package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.model.Client;
import org.springframework.stereotype.Service;

/**
 * Service responsável por construir mensagens de WhatsApp
 * Independente dos templates de email
 */
@Service
public class WhatsAppMessageBuilder {

    /**
     * Constrói mensagem para extratos mensais
     */
    public String buildMonthlyStatementsMessage(String period, String customMessage) {
        StringBuilder message = new StringBuilder();
        message.append("📊 *Hortifruti SL*\n\n");
        message.append("Olá! Segue anexos contábeis.\n\n");
        message.append("📅 *Período:* ").append(period != null ? period : "Atual").append("\n\n");
        
        message.append("📎 *Arquivos inclusos:*\n");
        message.append("• Extratos bancários (BB e Sicoob)\n");
        message.append("• Planilhas Excel com movimentações\n");
        message.append("• Notas fiscais do período\n\n");
        
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            message.append("💬 *Mensagem:*\n");
            message.append(customMessage.trim()).append("\n\n");
        }
        
        message.append("Atenciosamente,\n");
        message.append("*Hortifruti SL*");
        
        return message.toString();
    }

    /**
     * Constrói mensagem para arquivos genéricos da contabilidade
     */
    public String buildGenericFilesMessage(String totalValue, String customMessage) {
        StringBuilder message = new StringBuilder();
        message.append("📊 *Hortifruti SL*\n\n");
        message.append("Prezados, segue anexos contábeis.\n\n");
        
        if (totalValue != null && !totalValue.trim().isEmpty()) {
            message.append("💰 *Resumo Financeiro:*\n");
            message.append("• Valor total: R$ ").append(totalValue).append("\n\n");
        }
        
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            message.append("💬 *Mensagem:*\n");
            message.append(customMessage.trim()).append("\n\n");
        }
        
        message.append("Atenciosamente,\n");
        message.append("*Hortifruti SL*");
        
        return message.toString();
    }

    /**
     * Constrói mensagem para documentos de cliente
     */
    public String buildClientDocumentsMessage(String clientName, String customMessage) {
        StringBuilder message = new StringBuilder();
        message.append("📄 *Hortifruti SL*\n\n");
        
        if (clientName != null && !clientName.trim().isEmpty()) {
            message.append("Prezado(a) *").append(clientName.trim()).append("*,\n\n");
        } else {
            message.append("Prezado(a) cliente,\n\n");
        }
        
        message.append("Segue documentos em anexo para sua análise.\n\n");
        
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            message.append("💬 *Mensagem:*\n");
            message.append(customMessage.trim()).append("\n\n");
        }
        
        message.append("Qualquer dúvida, estamos à disposição.\n\n");
        message.append("Atenciosamente,\n");
        message.append("*Hortifruti SL*");
        
        return message.toString();
    }

    /**
     * Constrói mensagem para documentos de cliente usando objeto Client
     */
    public String buildClientDocumentsMessage(Client client, String customMessage) {
        String clientName = client != null ? client.getClientName() : null;
        return buildClientDocumentsMessage(clientName, customMessage);
    }

    /**
     * Constrói mensagem para boletos vencidos
     */
    public String buildOverdueBoletosMessage(int totalClients, int totalBoletos, String totalAmount, String customMessage) {
        StringBuilder message = new StringBuilder();
        message.append("🚨 *Hortifruti SL - Boletos Vencidos*\n\n");
        message.append("⚠️ *Alerta de Cobrança*\n\n");
        
        message.append("📊 *Resumo:*\n");
        message.append("• Clientes: ").append(totalClients).append("\n");
        message.append("• Boletos: ").append(totalBoletos).append("\n");
        message.append("• Valor total: R$ ").append(totalAmount != null ? totalAmount : "0,00").append("\n\n");
        
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            message.append("💬 *Observação:*\n");
            message.append(customMessage.trim()).append("\n\n");
        } else {
            message.append("💡 *Ação necessária:*\n");
            message.append("Verificar sistema para detalhes dos boletos vencidos.\n\n");
        }
        
        message.append("Atenciosamente,\n");
        message.append("*Hortifruti SL*");
        
        return message.toString();
    }

    /**
     * Mensagem genérica para casos não mapeados
     */
    public String buildGenericMessage(String subject, String customMessage) {
        StringBuilder message = new StringBuilder();
        message.append("📄 *Hortifruti SL*\n\n");
        
        if (subject != null && !subject.trim().isEmpty()) {
            message.append("*Assunto:* ").append(subject.trim()).append("\n\n");
        }
        
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            message.append("💬 *Mensagem:*\n");
            message.append(customMessage.trim()).append("\n\n");
        } else {
            message.append("Por favor verificar documentos em anexo.\n\n");
        }
        
        message.append("Atenciosamente,\n");
        message.append("*Hortifruti SL*");
        
        return message.toString();
    }
}