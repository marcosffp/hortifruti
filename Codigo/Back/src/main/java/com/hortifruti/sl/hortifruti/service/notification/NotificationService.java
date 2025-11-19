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
    System.out.println("Iniciando envio de arquivos genéricos para contabilidade...");
    try {
      List<byte[]> fileContents = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();

      // Converter arquivos (se fornecidos)
      if (files != null && !files.isEmpty()) {
        System.out.println("Arquivos recebidos: " + files.size());
        for (MultipartFile file : files) {
          fileContents.add(file.getBytes());
          fileNames.add(file.getOriginalFilename());
          System.out.println("Arquivo processado: " + file.getOriginalFilename());
        }
      } else {
        System.out.println("Nenhum arquivo foi enviado.");
      }

      BigDecimal cardValue = request.cardValue() != null ? request.cardValue() : BigDecimal.ZERO;
      BigDecimal cashValue = request.cashValue() != null ? request.cashValue() : BigDecimal.ZERO;

      System.out.println("Valores recebidos - Cartão: " + cardValue + ", Dinheiro: " + cashValue);

      BigDecimal discountedCardValue = cardValue.multiply(BigDecimal.valueOf(0.4));
      System.out.println("Valor com desconto (40%) - Cartão: " + discountedCardValue);

      // Preparar dados
      String subject = "Arquivos Contábeis - Resumo Financeiro";
      boolean hasFiles = fileContents != null && !fileContents.isEmpty();
      int filesCount = hasFiles ? fileContents.size() : 0;
      System.out.println("Quantidade de arquivos: " + filesCount);

      String emailBody =
          buildGenericFilesMessage(request, discountedCardValue, cashValue, hasFiles, filesCount);
      System.out.println("Corpo do email gerado com sucesso.");

      // Enviar apenas por email (sem WhatsApp para contabilidade)
      try {
        System.out.println("Enviando email para: " + accountingEmail);
        boolean emailSent =
            notificationCoordinator.sendEmailOnly(
                accountingEmail, subject, emailBody, fileContents, fileNames);

        System.out.println("Resultado do envio de email: " + emailSent);
        return new NotificationResponse(
            emailSent, emailSent ? "Email enviado com sucesso" : "Falha no envio do email");

      } catch (Exception e) {
        System.out.println("Erro ao enviar email: " + e.getMessage());
        return new NotificationResponse(false, "Erro ao enviar email: " + e.getMessage());
      }

    } catch (IOException e) {
      System.out.println("Erro ao processar arquivos: " + e.getMessage());
      return new NotificationResponse(false, "Erro ao processar arquivos: " + e.getMessage());
    }
  }

  /** Envio para cliente: Boleto + Nota Fiscal + arquivos genéricos */
  public NotificationResponse sendDocumentsToClient(
      List<MultipartFile> files, ClientDocumentsRequest request) {
    System.out.println("Iniciando envio de documentos para cliente...");
    try {
      Optional<Client> clientOpt = clientRepository.findById(request.clientId());
      if (clientOpt.isEmpty()) {
        System.out.println("Cliente não encontrado: ID " + request.clientId());
        return new NotificationResponse(false, "Cliente não encontrado");
      }

      Client client = clientOpt.get();
      System.out.println("Cliente encontrado: " + client.getClientName());

      List<byte[]> attachments = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();

      // Adicionar arquivos enviados
      if (files != null && !files.isEmpty()) {
        System.out.println("Arquivos recebidos: " + files.size());
        for (MultipartFile file : files) {
          attachments.add(file.getBytes());
          fileNames.add(file.getOriginalFilename());
          System.out.println("Arquivo processado: " + file.getOriginalFilename());
        }
      } else {
        System.out.println("Nenhum arquivo foi enviado.");
      }

      String subject = "Documentos - " + client.getClientName();
      String emailBody = buildClientMessage(request, client);
      System.out.println("Corpo do email gerado com sucesso.");

      // Context para WhatsApp
      var whatsAppContext =
          NotificationCoordinator.WhatsAppMessageContext.builder()
              .client(client)
              .customMessage(request.customMessage());
      System.out.println("Contexto do WhatsApp criado.");

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
      System.out.println("Erro ao processar arquivos: " + e.getMessage());
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
    System.out.println("Construindo mensagem de arquivos genéricos...");
    Map<String, String> variables = new HashMap<>();
    variables.put("CARD_VALUE", String.format("%.2f", discountedCardValue));
    variables.put("CASH_VALUE", String.format("%.2f", cashValue));

    // Controle de valores financeiros
    boolean hasFinancialValues =
        discountedCardValue.compareTo(BigDecimal.ZERO) != 0
            || cashValue.compareTo(BigDecimal.ZERO) != 0;
    variables.put("HAS_FINANCIAL_VALUES", hasFinancialValues ? "true" : "");
    variables.put("NO_FINANCIAL_VALUES", hasFinancialValues ? "" : "true");

    // Controle de arquivos
    variables.put("HAS_FILES", hasFiles ? "true" : "");
    variables.put("NO_FILES", hasFiles ? "" : "true");
    variables.put("FILES_COUNT", String.valueOf(filesCount));

    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      variables.put("CUSTOM_MESSAGE", request.customMessage());
      variables.put("DEFAULT_MESSAGE", "");
    } else {
      variables.put("CUSTOM_MESSAGE", "");
      variables.put("DEFAULT_MESSAGE", "true");
    }

    System.out.println("Mensagem de arquivos genéricos construída com sucesso.");
    return emailTemplateService.processTemplate("generic-files", variables);
  }

  private String buildClientMessage(ClientDocumentsRequest request, Client client) {
    System.out.println("Construindo mensagem para cliente...");
    Map<String, String> variables = new HashMap<>();
    variables.put("CLIENT_NAME", client.getClientName());

    // Adicionar data atual
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.format.DateTimeFormatter formatter =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    variables.put("CURRENT_DATE", today.format(formatter));

    if (request.customMessage() != null && !request.customMessage().isEmpty()) {
      variables.put("CUSTOM_MESSAGE", request.customMessage());
      variables.put("DEFAULT_MESSAGE", "");
    } else {
      variables.put("CUSTOM_MESSAGE", "");
      variables.put("DEFAULT_MESSAGE", "true");
    }

    System.out.println("Mensagem para cliente construída com sucesso.");
    return emailTemplateService.processTemplate("client-documents", variables);
  }
}