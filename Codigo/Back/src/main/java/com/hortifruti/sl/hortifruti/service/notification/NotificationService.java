package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.Client;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
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

  /**
   * Envio mensal para contabilidade: Extratos BB/Sicoob + Notas fiscais
   */
  public NotificationResponse sendMonthlyStatements(MonthlyStatementsRequest request) {
    try {
      log.info("Enviando extratos mensais {}/{}", request.month(), request.year());
      
      // Gerar ZIP com statements (pega os com maior data do mês)
      byte[] zipFile = fileGenerationService.createZipWithStatements(request.month(), request.year());
      
      List<byte[]> attachments = List.of(zipFile);
      List<String> fileNames = List.of("extratos_" + request.month() + "_" + request.year() + ".zip");

      String subject = "Documentos Contábeis - " + request.month() + "/" + request.year();
      String message = buildMonthlyMessage(request);

      return sendToAccounting(request.channel(), subject, message, attachments, fileNames);
      
    } catch (Exception e) {
      log.error("Erro ao enviar extratos mensais", e);
      return new NotificationResponse(false, "Erro: " + e.getMessage());
    }
  }

  /**
   * Envio para contabilidade: Arquivos genéricos com cálculo de redução de 60%
   */
  public NotificationResponse sendGenericFilesToAccounting(List<MultipartFile> files, GenericFilesAccountingRequest request) {
    try {
      log.info("Enviando arquivos genéricos para contabilidade");
      
      List<byte[]> fileContents = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();
      
      // Converter arquivos
      for (MultipartFile file : files) {
        fileContents.add(file.getBytes());
        fileNames.add(file.getOriginalFilename());
      }
      
      // Calcular valores com redução de 60%
      BigDecimal adjustedDebit = request.debitValue().multiply(BigDecimal.valueOf(0.4));
      BigDecimal adjustedCredit = request.creditValue().multiply(BigDecimal.valueOf(0.4));

      String subject = "Arquivos Contábeis - Resumo Financeiro";
      String message = buildGenericFilesMessage(request, adjustedDebit, adjustedCredit);

      return sendToAccounting(request.channel(), subject, message, fileContents, fileNames);
      
    } catch (IOException e) {
      log.error("Erro ao processar arquivos", e);
      return new NotificationResponse(false, "Erro ao processar arquivos: " + e.getMessage());
    }
  }

  /**
   * Envio para cliente: Boleto + Nota Fiscal + arquivos genéricos
   */
  public NotificationResponse sendDocumentsToClient(List<MultipartFile> files, ClientDocumentsRequest request) {
    try {
      log.info("Enviando documentos para cliente ID: {}", request.clientId());
      
      Optional<Client> clientOpt = clientRepository.findById(request.clientId());
      if (clientOpt.isEmpty()) {
        return new NotificationResponse(false, "Cliente não encontrado");
      }
      
      Client client = clientOpt.get();
      List<byte[]> attachments = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();
      
      // Adicionar arquivos genéricos
      if (files != null && !files.isEmpty()) {
        for (MultipartFile file : files) {
          attachments.add(file.getBytes());
          fileNames.add(file.getOriginalFilename());
        }
      }
      
      // Gerar boleto se solicitado
      if (request.includeBoleto()) {
        try {
          byte[] boleto = fileGenerationService.generateClientBoleto(request.clientId());
          attachments.add(boleto);
          fileNames.add("boleto_cliente_" + request.clientId() + ".xlsx");
        } catch (IOException e) {
          log.error("Erro ao gerar boleto para cliente {}", request.clientId(), e);
        }
      }
      
      // Gerar nota fiscal se solicitado
      if (request.includeNotaFiscal()) {
        try {
          byte[] notaFiscal = fileGenerationService.generateClientNotaFiscal(request.clientId());
          attachments.add(notaFiscal);
          fileNames.add("nota_fiscal_cliente_" + request.clientId() + ".xlsx");
        } catch (IOException e) {
          log.error("Erro ao gerar nota fiscal para cliente {}", request.clientId(), e);
        }
      }

      String subject = "Documentos - " + client.getClientName();
      String message = buildClientMessage(request, client);

      return sendToClient(client, request.channel(), subject, message, attachments, fileNames);
      
    } catch (IOException e) {
      log.error("Erro ao processar arquivos do cliente", e);
      return new NotificationResponse(false, "Erro ao processar arquivos: " + e.getMessage());
    }
  }



  /**
   * Teste dos serviços de comunicação
   */
  public NotificationResponse testCommunicationServices() {
    try {
      boolean emailOk = emailService != null && accountingEmail != null && !accountingEmail.isEmpty();
      boolean whatsappOk = whatsAppService != null && accountingWhatsapp != null && !accountingWhatsapp.isEmpty();
      
      String message = String.format("Email: %s, WhatsApp: %s", 
          emailOk ? "OK" : "FALHA", whatsappOk ? "OK" : "FALHA");
      
      return NotificationResponse.withStatuses(emailOk && whatsappOk, message, 
          emailOk ? "OK" : "FALHA", whatsappOk ? "OK" : "FALHA");
      
    } catch (Exception e) {
      log.error("Erro ao testar serviços", e);
      return new NotificationResponse(false, "Erro ao testar serviços: " + e.getMessage());
    }
  }

  // Métodos auxiliares privados
  
  private NotificationResponse sendToAccounting(NotificationChannel channel, String subject, 
                                               String message, List<byte[]> attachments, List<String> fileNames) {
    boolean emailSent = false;
    boolean whatsappSent = false;

    if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
      emailSent = emailService.sendEmailWithAttachments(accountingEmail, subject, message, attachments, fileNames);
    }

    if (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH) {
      whatsappSent = whatsAppService.sendMultipleDocuments(accountingWhatsapp, message, attachments, fileNames);
    }

    boolean success = (channel == NotificationChannel.EMAIL && emailSent) ||
                     (channel == NotificationChannel.WHATSAPP && whatsappSent) ||
                     (channel == NotificationChannel.BOTH && emailSent && whatsappSent);

    return NotificationResponse.withStatuses(success, 
        success ? "Enviado com sucesso" : "Falha no envio",
        emailSent ? "OK" : (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH ? "FALHA" : "N/A"),
        whatsappSent ? "OK" : (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH ? "FALHA" : "N/A"));
  }

  private NotificationResponse sendToClient(Client client, NotificationChannel channel, String subject,
                                          String message, List<byte[]> attachments, List<String> fileNames) {
    boolean emailSent = false;
    boolean whatsappSent = false;

    if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
      if (client.getEmail() != null && !client.getEmail().isEmpty()) {
        emailSent = emailService.sendEmailWithAttachments(client.getEmail(), subject, message, attachments, fileNames);
      }
    }

    if (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH) {
      log.info("=== DEBUG WHATSAPP ===");
      log.info("Cliente ID: {}", client.getId());
      log.info("Cliente Nome: {}", client.getClientName());
      log.info("Número do banco (raw): '{}'", client.getPhoneNumber());
      log.info("Número é null: {}", client.getPhoneNumber() == null);
      log.info("Número é vazio: {}", client.getPhoneNumber() != null ? client.getPhoneNumber().isEmpty() : "N/A");

      if (client.getPhoneNumber() != null && !client.getPhoneNumber().isEmpty()) {
        log.info("Enviando WhatsApp para: '{}'", client.getPhoneNumber());
        whatsappSent = whatsAppService.sendMultipleDocuments(client.getPhoneNumber(), message, attachments, fileNames);
        log.info("Resultado WhatsApp: {}", whatsappSent);
      } else {
        log.warn("Número de telefone do cliente {} é null ou vazio", client.getClientName());
      }
    }

    boolean success = (channel == NotificationChannel.EMAIL && emailSent) ||
                     (channel == NotificationChannel.WHATSAPP && whatsappSent) ||
                     (channel == NotificationChannel.BOTH && emailSent && whatsappSent);

    return NotificationResponse.withStatuses(success,
        success ? "Enviado para " + client.getClientName() : "Falha no envio para " + client.getClientName(),
        emailSent ? "OK" : (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH ? "FALHA" : "N/A"),
        whatsappSent ? "OK" : (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH ? "FALHA" : "N/A"));
  }

  private String buildMonthlyMessage(MonthlyStatementsRequest request) {
    StringBuilder msg = new StringBuilder();
    msg.append("Prezados,\n\n");
    msg.append("Segue anexos referentes ao mês ").append(request.month()).append("/").append(request.year()).append(":\n\n");
    msg.append("• Extratos bancários (BB e Sicoob)\n");
    msg.append("• Planilhas Excel com movimentos\n");
    msg.append("• Notas fiscais do período\n\n");
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      msg.append("Observações:\n").append(request.customMessage()).append("\n\n");
    }
    
    msg.append("Atenciosamente,\nHortifruti SL");
    return msg.toString();
  }

  private String buildGenericFilesMessage(GenericFilesAccountingRequest request, BigDecimal adjustedDebit, BigDecimal adjustedCredit) {
    StringBuilder msg = new StringBuilder();
    msg.append("Prezados,\n\n");
    msg.append("Segue anexos contábeis solicitados.\n\n");
    msg.append("Resumo Financeiro (após ajuste de 60%):\n");
    msg.append("• Débito: R$ ").append(adjustedDebit).append("\n");
    msg.append("• Crédito: R$ ").append(adjustedCredit).append("\n");
    msg.append("• Dinheiro: R$ ").append(request.cashValue()).append("\n\n");
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      msg.append("Observações:\n").append(request.customMessage()).append("\n\n");
    }
    
    msg.append("Atenciosamente,\nHortifruti SL");
    return msg.toString();
  }

  private String buildClientMessage(ClientDocumentsRequest request, Client client) {
    StringBuilder msg = new StringBuilder();
    msg.append("Prezado(a) ").append(client.getClientName()).append(",\n\n");
    msg.append("Segue anexos solicitados:\n\n");
    
    if (request.includeBoleto()) msg.append("• Boleto de pagamento\n");
    if (request.includeNotaFiscal()) msg.append("• Nota fiscal\n");
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      msg.append("\n").append(request.customMessage()).append("\n");
    }
    
    msg.append("\nAtenciosamente,\nHortifruti SL");
    return msg.toString();
  }


}
