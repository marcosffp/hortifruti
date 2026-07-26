package com.hortifruti.sl.hortifruti.service.notification.whatsapp;

import com.hortifruti.sl.hortifruti.model.purchase.Client;
import org.springframework.stereotype.Service;

/** Service responsável por construir mensagens de WhatsApp Independente dos templates de email */
@Service
public class WhatsAppMessageBuilder {

  public String buildMonthlyStatementsMessage(String period, String customMessage) {
    StringBuilder message = new StringBuilder();
    message.append("Olá! Segue anexos contábeis.\n\n");
    message.append("*Período:* ").append(period != null ? period : "Atual").append("\n\n");

    message.append("*Arquivos inclusos:*\n");
    message.append("• Extratos bancários (BB e Sicoob)\n");
    message.append("• Planilhas Excel com movimentações\n");
    message.append("• Notas fiscais do período\n\n");

    if (customMessage != null && !customMessage.trim().isEmpty()) {
      message.append("*Mensagem:*\n");
      message.append(customMessage.trim()).append("\n\n");
    }

    message.append("Atenciosamente, ");
    message.append("*Hortifruti SL*");

    return message.toString();
  }

  public String buildGenericFilesMessage(String totalValue, String customMessage) {
    StringBuilder message = new StringBuilder();

    message.append("Prezados, segue anexos contábeis.\n\n");

    if (totalValue != null && !totalValue.trim().isEmpty()) {
      message.append("*Resumo Financeiro:*\n");
      message.append("• Valor total: R$ ").append(totalValue).append("\n\n");
    }

    if (customMessage != null && !customMessage.trim().isEmpty()) {
      message.append("*Mensagem:*\n");
      message.append(customMessage.trim()).append("\n\n");
    }

    message.append("Atenciosamente, ");
    message.append("*Hortifruti SL*");

    return message.toString();
  }

  public String buildClientDocumentsMessage(String clientName, String customMessage) {
    StringBuilder message = new StringBuilder();

    if (clientName != null && !clientName.trim().isEmpty()) {
      message.append("Prezado(a) *").append(clientName.trim()).append("*,\n\n");
    } else {
      message.append("Prezado(a) cliente,\n\n");
    }

    message.append("Segue documentos em anexo para sua análise.\n\n");

    if (customMessage != null && !customMessage.trim().isEmpty()) {
      message.append("*Mensagem:*\n");
      message.append(customMessage.trim()).append("\n\n");
    }

    message.append("Qualquer dúvida, estamos à disposição.\n\n");
    message.append("Atenciosamente,\n");
    message.append("*Hortifruti SL*");

    return message.toString();
  }

  public String buildClientDocumentsMessage(Client client, String customMessage) {
    String clientName = client != null ? client.getClientName() : null;
    return buildClientDocumentsMessage(clientName, customMessage);
  }

  /** Legenda para envio de boleto em PDF. Necessária porque o WhatsApp não exibe preview de PDF. */
  public String buildBilletMessage(String period) {
    StringBuilder message = new StringBuilder();
    message.append("Segue o boleto referente a ").append(period != null ? period : "sua cobrança");
    message.append(".\n\n");
    message.append("Atenciosamente, ");
    message.append("*Hortifruti SL*");
    return message.toString();
  }

  /** Legenda para envio de XML de NF-e. Necessária porque o WhatsApp não exibe preview de XML. */
  public String buildNfeXmlMessage(String nfNumber) {
    StringBuilder message = new StringBuilder();
    message.append("Segue XML da NFe nº ").append(nfNumber != null ? nfNumber : "");
    message.append(".\n\n");
    message.append("Atenciosamente, ");
    message.append("*Hortifruti SL*");
    return message.toString();
  }

  public String buildGenericMessage(String subject, String customMessage) {
    StringBuilder message = new StringBuilder();

    if (subject != null && !subject.trim().isEmpty()) {
      message.append("*Assunto:* ").append(subject.trim()).append("\n\n");
    }

    if (customMessage != null && !customMessage.trim().isEmpty()) {
      message.append("*Mensagem:*\n");
      message.append(customMessage.trim()).append("\n\n");
    } else {
      message.append("Por favor verificar documentos em anexo.\n\n");
    }

    message.append("Atenciosamente, ");
    message.append("*Hortifruti SL*");

    return message.toString();
  }
}
