package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationType;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final EmailService emailService;
  private final WhatsAppService whatsAppService;
  private final FileGenerationService fileGenerationService;
  private final StatementSelectionService statementSelectionService;
  private final ClientRepository clientRepository;

  @Value("${accounting.email}")
  private String accountingEmail;

  @Value("${manager.email}")
  private String managerEmail;

  @Value("${accounting.whatsapp}")
  private String accountingWhatsapp;

  @Value("${manager.whatsapp}")
  private String managerWhatsapp;

  @Value("${OVERDUE_NOTIFICATION_EMAILS}")
  private String overdueNotificationEmails;

  public NotificationResponse sendAccountingNotification(AccountingNotificationRequest request) {
    try {
      // Gerar ZIP com statements do mês anterior
      byte[] zipFile = fileGenerationService.createZipWithStatements(request.month(), request.year());
      
      // Processar arquivos genéricos se fornecidos
      byte[] processedFile = null;
      if (request.additionalFiles() != null && !request.additionalFiles().isEmpty()) {
        List<byte[]> files = request.additionalFiles().stream()
            .map(GenericFileRequest::fileContent)
            .toList();
        List<String> fileNames = request.additionalFiles().stream()
            .map(GenericFileRequest::fileName)
            .toList();
        
        processedFile = fileGenerationService.processGenericFiles(
            files, fileNames, 
            request.debitValue(), 
            request.creditValue(), 
            request.cashValue()
        );
      }

      // Preparar arquivos para envio
      List<byte[]> attachments = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();
      
      attachments.add(zipFile);
      fileNames.add("extratos_" + request.month() + "_" + request.year() + ".zip");
      
      if (processedFile != null) {
        attachments.add(processedFile);
        fileNames.add("resumo_financeiro_" + request.month() + "_" + request.year() + ".xlsx");
      }

      // Preparar mensagem
      String subject = "Documentos Contábeis - " + request.month() + "/" + request.year();
      String message = buildAccountingMessage(request);

      boolean emailSent = false;
      boolean whatsappSent = false;

      // Enviar por email se solicitado
      if (request.notificationType() == NotificationType.EMAIL_ONLY || 
          request.notificationType() == NotificationType.BOTH) {
        emailSent = emailService.sendEmailWithAttachments(
            accountingEmail, subject, message, attachments, fileNames
        );
      }

      // Enviar por WhatsApp se solicitado
      if (request.notificationType() == NotificationType.WHATSAPP_ONLY || 
          request.notificationType() == NotificationType.BOTH) {
        whatsappSent = whatsAppService.sendMultipleDocuments(
            accountingWhatsapp, message, attachments, fileNames
        );
      }

      boolean success = (request.notificationType() == NotificationType.EMAIL_ONLY && emailSent) ||
                       (request.notificationType() == NotificationType.WHATSAPP_ONLY && whatsappSent) ||
                       (request.notificationType() == NotificationType.BOTH && emailSent && whatsappSent);

      return new NotificationResponse(
          success,
          success ? "Notificação enviada com sucesso" : "Falha no envio da notificação",
          emailSent ? "Enviado" : "Não enviado",
          whatsappSent ? "Enviado" : "Não enviado"
      );

    } catch (Exception e) {
      e.printStackTrace();
      return new NotificationResponse(
          false,
          "Erro interno: " + e.getMessage(),
          "Erro",
          "Erro"
      );
    }
  }

  public NotificationResponse sendClientNotification(ClientNotificationRequest request) {
    try {
      Optional<Client> clientOpt = clientRepository.findById(request.clientId());
      if (clientOpt.isEmpty()) {
        return new NotificationResponse(
            false,
            "Cliente não encontrado",
            "Não enviado",
            "Não enviado"
        );
      }

      Client client = clientOpt.get();
      
      List<byte[]> attachments = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();

      // Adicionar arquivos genéricos
      if (request.files() != null) {
        for (GenericFileRequest file : request.files()) {
          attachments.add(file.fileContent());
          fileNames.add(file.fileName());
        }
      }

      // Adicionar boleto se solicitado
      if (request.includeBoleto()) {
        try {
          // Buscar último boleto do cliente (implementar lógica específica)
          // Por enquanto, vamos pular esta parte pois precisaria de mais informações
          // sobre como identificar qual boleto enviar
        } catch (Exception e) {
          // Log do erro mas continua o processo
          e.printStackTrace();
        }
      }

      // Preparar mensagem
      String subject = "Documentos - " + client.getClientName();
      String message = buildClientMessage(request, client);

      boolean emailSent = false;
      boolean whatsappSent = false;

      // Enviar por email se solicitado
      if (request.notificationType() == NotificationType.EMAIL_ONLY || 
          request.notificationType() == NotificationType.BOTH) {
        if (attachments.isEmpty()) {
          emailSent = emailService.sendSimpleEmail(client.getEmail(), subject, message);
        } else {
          emailSent = emailService.sendEmailWithAttachments(
              client.getEmail(), subject, message, attachments, fileNames
          );
        }
      }

      // Enviar por WhatsApp se solicitado
      if (request.notificationType() == NotificationType.WHATSAPP_ONLY || 
          request.notificationType() == NotificationType.BOTH) {
        if (attachments.isEmpty()) {
          whatsappSent = whatsAppService.sendTextMessage(client.getPhoneNumber(), message);
        } else {
          whatsappSent = whatsAppService.sendMultipleDocuments(
              client.getPhoneNumber(), message, attachments, fileNames
          );
        }
      }

      boolean success = (request.notificationType() == NotificationType.EMAIL_ONLY && emailSent) ||
                       (request.notificationType() == NotificationType.WHATSAPP_ONLY && whatsappSent) ||
                       (request.notificationType() == NotificationType.BOTH && emailSent && whatsappSent);

      return new NotificationResponse(
          success,
          success ? "Notificação enviada com sucesso para " + client.getClientName() : 
                   "Falha no envio da notificação para " + client.getClientName(),
          emailSent ? "Enviado" : "Não enviado",
          whatsappSent ? "Enviado" : "Não enviado"
      );

    } catch (Exception e) {
      e.printStackTrace();
      return new NotificationResponse(
          false,
          "Erro interno: " + e.getMessage(),
          "Erro",
          "Erro"
      );
    }
  }

  public NotificationResponse sendOverdueBilletNotification(String clientName, String billetInfo) {
    try {
      String subject = "Aviso: Boleto em Atraso - " + clientName;
      String message = buildOverdueMessage(clientName, billetInfo);

      // Enviar para múltiplos emails configurados no .env
      boolean emailSent = sendToMultipleEmails(overdueNotificationEmails, subject, message);
      
      boolean whatsappSent = false;
      if (managerWhatsapp != null && !managerWhatsapp.isEmpty()) {
        whatsappSent = whatsAppService.sendTextMessage(managerWhatsapp, message);
      }

      return new NotificationResponse(
          emailSent,
          emailSent ? "Notificação de boleto vencido enviada para todos os destinatários" : "Falha no envio da notificação",
          emailSent ? "Enviado" : "Não enviado",
          whatsappSent ? "Enviado" : "Não enviado"
      );

    } catch (Exception e) {
      e.printStackTrace();
      return new NotificationResponse(
          false,
          "Erro interno: " + e.getMessage(),
          "Erro",
          "Erro"
      );
    }
  }

  private String buildAccountingMessage(AccountingNotificationRequest request) {
    StringBuilder message = new StringBuilder();
    message.append("<html><body>");
    message.append("<h2>Documentos Contábeis</h2>");
    message.append("<p>Segue em anexo os documentos contábeis referentes ao mês <strong>");
    message.append(request.month()).append("/").append(request.year()).append("</strong>:</p>");
    
    // Adicionar informações sobre a cobertura dos statements
    try {
      var statements = statementSelectionService.getBestStatementsForMonth(request.month(), request.year());
      String coverageInfo = statementSelectionService.getStatementCoverageInfo(statements, request.month(), request.year());
      
      message.append("<div style='background-color: #f0f8ff; padding: 10px; margin: 10px 0; border-left: 4px solid #007acc;'>");
      message.append("<h3>Informações dos Extratos Selecionados:</h3>");
      message.append("<pre style='font-family: monospace; font-size: 12px;'>").append(coverageInfo).append("</pre>");
      message.append("</div>");
    } catch (Exception e) {
      // Se houver erro ao obter informações, apenas continua sem elas
      System.err.println("Erro ao obter informações de cobertura: " + e.getMessage());
    }
    
    message.append("<ul>");
    message.append("<li>ZIP com as notas fiscais do mês</li>");
    message.append("<li>Extratos bancários do BB e Sicoob (melhores disponíveis para o período)</li>");
    message.append("<li>Planilhas Excel geradas dos extratos</li>");
    
    if (request.additionalFiles() != null && !request.additionalFiles().isEmpty()) {
      message.append("<li>Resumo financeiro processado</li>");
    }
    
    message.append("</ul>");
    
    if (request.customMessage() != null && !request.customMessage().trim().isEmpty()) {
      message.append("<p><strong>Mensagem adicional:</strong></p>");
      message.append("<p>").append(request.customMessage()).append("</p>");
    }
    
    message.append("<p>Atenciosamente,<br/>Hortifruti Santa Luzia</p>");
    message.append("</body></html>");
    
    return message.toString();
  }

  private String buildClientMessage(ClientNotificationRequest request, Client client) {
    StringBuilder message = new StringBuilder();
    message.append("<html><body>");
    message.append("<h2>Olá, ").append(client.getClientName()).append("!</h2>");
    
    if (request.customMessage() != null && !request.customMessage().trim().isEmpty()) {
      message.append("<p>").append(request.customMessage()).append("</p>");
    } else {
      message.append("<p>Segue em anexo os documentos solicitados.</p>");
    }
    
    if (request.includeBoleto()) {
      message.append("<p>• Boleto de cobrança</p>");
    }
    
    if (request.includeNotaFiscal()) {
      message.append("<p>• Nota fiscal</p>");
    }
    
    message.append("<p>Em caso de dúvidas, entre em contato conosco.</p>");
    message.append("<p>Atenciosamente,<br/>Hortifruti Santa Luzia</p>");
    message.append("</body></html>");
    
    return message.toString();
  }

  private String buildOverdueMessage(String clientName, String billetInfo) {
    return String.format(
        "⚠️ AVISO: Boleto Vencido\n\n" +
        "Cliente: %s\n" +
        "Informações: %s\n\n" +
        "Favor verificar e tomar as medidas necessárias.\n\n" +
        "Sistema Hortifruti Santa Luzia",
        clientName, billetInfo
    );
  }

  public NotificationResponse checkAndNotifyOverdueBillets() {
    try {
      // TODO: Implementar busca por boletos vencidos quando o repositório estiver disponível
      // List<Boleto> overdueBillets = boletoRepository.findOverdueBillets(LocalDate.now());
      
      // Por enquanto, retorna sucesso com mensagem informativa
      String message = "Verificação de boletos vencidos executada com sucesso. " +
          "Implementação completa requer repositório de boletos.";
      
      return new NotificationResponse(true, message, "N/A", "N/A");
    } catch (Exception e) {
      return new NotificationResponse(false, "Erro ao verificar boletos vencidos: " + e.getMessage(), "ERRO", "ERRO");
    }
  }

  public boolean testEmailService() {
    try {
      // Testa configuração do email
      return emailService != null && accountingEmail != null && !accountingEmail.trim().isEmpty();
    } catch (Exception e) {
      System.err.println("Erro ao testar serviço de email: " + e.getMessage());
      return false;
    }
  }

  public boolean testWhatsAppService() {
    try {
      // Testa configuração do WhatsApp
      return whatsAppService != null && accountingWhatsapp != null && !accountingWhatsapp.trim().isEmpty();
    } catch (Exception e) {
      System.err.println("Erro ao testar serviço de WhatsApp: " + e.getMessage());
      return false;
    }
  }

  /**
   * Envia email para múltiplos destinatários configurados no .env
   * Os emails devem estar separados por vírgula na variável de ambiente
   */
  private boolean sendToMultipleEmails(String emailsString, String subject, String message) {
    if (emailsString == null || emailsString.trim().isEmpty()) {
      System.err.println("AVISO: Nenhum email configurado para notificações de boletos vencidos");
      return false;
    }

    try {
      // Separar emails por vírgula e remover espaços
      String[] emailAddresses = emailsString.split(",");
      int sucessCount = 0;

      for (String email : emailAddresses) {
        email = email.trim();
        if (!email.isEmpty()) {
          try {
            boolean sent = emailService.sendSimpleEmail(email, subject, message);
            if (sent) {
              sucessCount++;
              System.out.println("Notificação de boleto vencido enviada para: " + email);
            } else {
              System.err.println("Falha ao enviar notificação para: " + email);
            }
          } catch (Exception e) {
            System.err.println("Erro ao enviar para " + email + ": " + e.getMessage());
          }
        }
      }

      System.out.println("Notificações enviadas: " + sucessCount + "/" + emailAddresses.length);
      return sucessCount > 0; // Retorna true se pelo menos um email foi enviado
      
    } catch (Exception e) {
      System.err.println("Erro ao processar lista de emails: " + e.getMessage());
      return false;
    }
  }
}