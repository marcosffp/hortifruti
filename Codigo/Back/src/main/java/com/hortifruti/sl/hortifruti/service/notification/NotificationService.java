package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationCoordinator notificationCoordinator;
  private final ClientRepository clientRepository;
  private final EmailTemplateService emailTemplateService;

  @Value("${accounting.email}")
  private String accountingEmail;

  public NotificationResponse sendGenericFilesToAccounting(
      List<MultipartFile> files, GenericFilesAccountingRequest request) {
    try {
      List<byte[]> fileContents = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();

      // Converter arquivos (se fornecidos)
      if (files != null && !files.isEmpty()) {
        for (MultipartFile file : files) {
          fileContents.add(file.getBytes());
          fileNames.add(file.getOriginalFilename());
        }
      }

      BigDecimal cardValue = request.cardValue() != null ? request.cardValue() : BigDecimal.ZERO;
      BigDecimal cashValue = request.cashValue() != null ? request.cashValue() : BigDecimal.ZERO;

      BigDecimal discountedCardValue = cardValue.multiply(BigDecimal.valueOf(0.4));

      // Preparar dados
      String subject = "Arquivos Contábeis - Resumo Financeiro";
      boolean hasFiles = fileContents != null && !fileContents.isEmpty();
      int filesCount = hasFiles ? fileContents.size() : 0;
      String emailBody =
          buildGenericFilesMessage(request, discountedCardValue, cashValue, hasFiles, filesCount);

      // Enviar apenas por email (sem WhatsApp para contabilidade)
      try {
        boolean emailSent =
            notificationCoordinator.sendEmailOnly(
                accountingEmail, subject, emailBody, fileContents, fileNames);

        return new NotificationResponse(
            emailSent, emailSent ? "Email enviado com sucesso" : "Falha no envio do email");

      } catch (Exception e) {
        return new NotificationResponse(false, "Erro ao enviar email: " + e.getMessage());
      }

    } catch (IOException e) {
      return new NotificationResponse(false, "Erro ao processar arquivos: " + e.getMessage());
    }
  }

  /** Envio para cliente: Boleto + Nota Fiscal + arquivos genéricos */
  public NotificationResponse sendDocumentsToClient(
      List<MultipartFile> files, ClientDocumentsRequest request) {
    try {
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
      var whatsAppContext =
          NotificationCoordinator.WhatsAppMessageContext.builder()
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
          fileNames);

    } catch (IOException e) {
      return new NotificationResponse(false, "Erro ao processar arquivos: " + e.getMessage());
    }
  }

  // Métodos auxiliares privados

  private String buildGenericFilesMessage(
      GenericFilesAccountingRequest request,
      BigDecimal discountedCardValue,
      BigDecimal cashValue,
      boolean hasFiles,
      int filesCount) {
    Map<String, String> variables = new HashMap<>();
    variables.put("CARD_VALUE", String.format("%.2f", discountedCardValue));
    variables.put("CASH_VALUE", String.format("%.2f", cashValue));

    // Controle de valores financeiros - só exibir se pelo menos um valor for diferente de zero
    boolean hasFinancialValues =
        discountedCardValue.compareTo(BigDecimal.ZERO) != 0
            || cashValue.compareTo(BigDecimal.ZERO) != 0;
    if (hasFinancialValues) {
      variables.put("HAS_FINANCIAL_VALUES", "true");
      variables.put("NO_FINANCIAL_VALUES", "");
    } else {
      variables.put("HAS_FINANCIAL_VALUES", "");
      variables.put("NO_FINANCIAL_VALUES", "true");
    }

    // Controle de arquivos
    if (hasFiles) {
      variables.put("HAS_FILES", "true");
      variables.put("NO_FILES", "");
      variables.put("FILES_COUNT", String.valueOf(filesCount));
    } else {
      variables.put("HAS_FILES", "");
      variables.put("NO_FILES", "true");
      variables.put("FILES_COUNT", "0");
    }

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
    java.time.format.DateTimeFormatter formatter =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
