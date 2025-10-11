package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.Client;

import com.hortifruti.sl.hortifruti.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationCoordinator notificationCoordinator;
  private final FileGenerationService fileGenerationService;
  private final ClientRepository clientRepository;
  private final EmailTemplateService emailTemplateService;

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
  @Transactional(readOnly = true)
  public NotificationResponse sendMonthlyStatements(MonthlyStatementsRequest request) {
    try {
      log.info("Enviando extratos mensais {}/{}", request.month(), request.year());
      
      // Gerar ZIP com statements
      byte[] zipFile = fileGenerationService.createZipWithStatements(request.month(), request.year());
      List<byte[]> attachments = List.of(zipFile);
      List<String> fileNames = List.of("extratos_" + request.month() + "_" + request.year() + ".zip");

      // Preparar dados
      String subject = "Documentos Contábeis - " + request.month() + "/" + request.year();
      String emailBody = buildMonthlyMessage(request);
      String period = request.month() + "/" + request.year();

      // Context para WhatsApp
      var whatsAppContext = NotificationCoordinator.WhatsAppMessageContext.builder()
          .period(period)
          .customMessage(request.customMessage());

      return notificationCoordinator.sendNotification(
          accountingEmail,
          accountingWhatsapp,
          request.channel(),
          subject,
          emailBody,
          NotificationCoordinator.WhatsAppMessageType.MONTHLY_STATEMENTS,
          whatsAppContext,
          attachments,
          fileNames
      );
      
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
      
      // Calcular valores com desconto de 60%
      BigDecimal total = BigDecimal.ZERO;
      if (request.debitValue() != null) total = total.add(request.debitValue());
      if (request.creditValue() != null) total = total.add(request.creditValue());
      if (request.cashValue() != null) total = total.add(request.cashValue());
      BigDecimal totalComDesconto = total.multiply(BigDecimal.valueOf(0.4));

      // Preparar dados
      String subject = "Arquivos Contábeis - Resumo Financeiro";
      String emailBody = buildGenericFilesMessage(request, total, totalComDesconto);
      String totalValue = String.format("%.2f", totalComDesconto);

      // Context para WhatsApp
      var whatsAppContext = NotificationCoordinator.WhatsAppMessageContext.builder()
          .totalValue(totalValue)
          .customMessage(request.customMessage());

      return notificationCoordinator.sendNotification(
          accountingEmail,
          accountingWhatsapp,
          request.channel(),
          subject,
          emailBody,
          NotificationCoordinator.WhatsAppMessageType.GENERIC_FILES,
          whatsAppContext,
          fileContents,
          fileNames
      );
      
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
      
      // Adicionar arquivos enviados
      if (files != null && !files.isEmpty()) {
        for (MultipartFile file : files) {
          attachments.add(file.getBytes());
          fileNames.add(file.getOriginalFilename());
        }
      }

      String subject = "Documentos - " + client.getClientName();
      String emailBody = buildClientMessage(request, client);

      // Context para WhatsApp
      var whatsAppContext = NotificationCoordinator.WhatsAppMessageContext.builder()
          .client(client)
          .customMessage(request.customMessage());

      return notificationCoordinator.sendNotification(
          client.getEmail(),
          client.getPhoneNumber(),
          request.channel(),
          subject,
          emailBody,
          NotificationCoordinator.WhatsAppMessageType.CLIENT_DOCUMENTS,
          whatsAppContext,
          attachments,
          fileNames
      );
      
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
      // Testar serviços através do coordinator
      boolean emailOk = notificationCoordinator != null && accountingEmail != null && !accountingEmail.isEmpty();
      boolean whatsappOk = notificationCoordinator != null && accountingWhatsapp != null && !accountingWhatsapp.isEmpty();
      
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

  private String buildMonthlyMessage(MonthlyStatementsRequest request) {
    Map<String, String> variables = new HashMap<>();
    variables.put("MONTH", String.valueOf(request.month()));
    variables.put("YEAR", String.valueOf(request.year()));
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      variables.put("CUSTOM_MESSAGE", request.customMessage());
      variables.put("DEFAULT_MESSAGE", ""); // Não mostrar mensagem padrão
    } else {
      variables.put("CUSTOM_MESSAGE", ""); // Não mostrar Mensagem
      variables.put("DEFAULT_MESSAGE", "true"); // Mostrar mensagem padrão
    }
    
    return emailTemplateService.processTemplate("monthly-statements", variables);
  }

  private String buildGenericFilesMessage(GenericFilesAccountingRequest request, BigDecimal adjustedDebit, BigDecimal adjustedCredit) {
    Map<String, String> variables = new HashMap<>();
    variables.put("DISCOUNTED_VALUE", String.format("%.2f", adjustedCredit));
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      variables.put("CUSTOM_MESSAGE", request.customMessage());
      variables.put("DEFAULT_MESSAGE", ""); // Não mostrar mensagem padrão
    } else {
      variables.put("CUSTOM_MESSAGE", ""); // Não mostrar Mensagem
      variables.put("DEFAULT_MESSAGE", "true"); // Mostrar mensagem padrão
    }
    
    return emailTemplateService.processTemplate("generic-files", variables);
  }

  private String buildClientMessage(ClientDocumentsRequest request, Client client) {
    Map<String, String> variables = new HashMap<>();
    variables.put("CLIENT_NAME", client.getClientName());
    
    // Adicionar data atual
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    variables.put("CURRENT_DATE", today.format(formatter));
    
    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      variables.put("CUSTOM_MESSAGE", request.customMessage());
      variables.put("DEFAULT_MESSAGE", ""); // Não mostrar mensagem padrão
    } else {
      variables.put("CUSTOM_MESSAGE", ""); // Não mostrar mensagem customizada
      variables.put("DEFAULT_MESSAGE", "true"); // Mostrar mensagem padrão
    }
    
    return emailTemplateService.processTemplate("client-documents", variables);
  }




}
