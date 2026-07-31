package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.config.notification.NotificationEnvironmentGuard;
import com.hortifruti.sl.hortifruti.dto.notification.*;
import com.hortifruti.sl.hortifruti.exception.notification.NotificationException;
import com.hortifruti.sl.hortifruti.model.notification.NotificationChannel;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.service.notification.email.EmailGreetingUtil;
import com.hortifruti.sl.hortifruti.service.notification.email.EmailTemplateService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationCoordinator notificationCoordinator;
  private final ClientRepository clientRepository;
  private final EmailTemplateService emailTemplateService;
  private final NotificationEnvironmentGuard notificationEnvironmentGuard;

  // Pode ser um único email ou uma lista separada por vírgulas (mesmo formato usado em
  // overdue.notification.emails), ex.: "a@x.com,b@y.com".
  @Value("${accounting.email}")
  private String accountingEmail;

  @Value("${notification.sender-name:}")
  private String senderName;

  public NotificationResponse sendGenericFilesToAccounting(
      List<MultipartFile> files, GenericFilesAccountingRequest request) {

    if (notificationEnvironmentGuard.isBlocked()) {
      throw new NotificationException("Envio de notificações está desabilitado em homologação.");
    }

    try {
      List<byte[]> fileContents = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();

      if (files != null && !files.isEmpty()) {
        for (MultipartFile file : files) {
          fileContents.add(file.getBytes());
          fileNames.add(file.getOriginalFilename());
        }
      }

      BigDecimal cardValue = request.cardValue() != null ? request.cardValue() : BigDecimal.ZERO;
      BigDecimal cashValue = request.cashValue() != null ? request.cashValue() : BigDecimal.ZERO;
      BigDecimal discountedCardValue = cardValue.multiply(BigDecimal.valueOf(0.4));

      String subject = "Arquivos Contábeis - Resumo Financeiro";

      String emailBody =
          buildGenericFilesMessage(
              request,
              discountedCardValue,
              cashValue,
              !fileContents.isEmpty(),
              fileContents.size());

      List<String> recipients = getAccountingRecipients();
      if (recipients.isEmpty()) {
        return new NotificationResponse(
            false, "Nenhum email de contabilidade configurado (ACCOUNTING_EMAIL).");
      }

      boolean anySent = false;
      for (String recipient : recipients) {
        try {
          boolean sent =
              notificationCoordinator.sendEmailOnly(
                  recipient, subject, emailBody, fileContents, fileNames);
          anySent = anySent || sent;
        } catch (NotificationException e) {
          if (e.getMessage() != null && e.getMessage().contains("Autorização")) {
            // Falha de autorização (token OAuth do Gmail expirado/revogado) afeta todos os
            // destinatários igualmente — não adianta continuar tentando os próximos, e o front
            // precisa receber essa exceção para mostrar o link de reautorização ao usuário.
            throw e;
          }
          log.error(
              "Falha ao enviar email de contabilidade para {}: {}", recipient, e.getMessage());
        }
      }

      return new NotificationResponse(
          anySent, anySent ? "Email enviado com sucesso" : "Falha no envio do email");

    } catch (IOException e) {
      throw new NotificationException("Erro ao processar arquivos: " + e.getMessage());
    }
  }

  public NotificationResponse sendDocumentsToClient(
      List<MultipartFile> files, ClientDocumentsRequest request) {

    if (notificationEnvironmentGuard.isBlocked()) {
      throw new NotificationException("Envio de notificações está desabilitado em homologação.");
    }

    try {
      Client client =
          clientRepository
              .findById(request.clientId())
              .orElseThrow(() -> new NotificationException("Cliente não encontrado"));

      requireContactForChannel(client, request.channel());

      List<byte[]> attachments = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();

      if (files != null && !files.isEmpty()) {
        for (MultipartFile file : files) {
          attachments.add(file.getBytes());
          fileNames.add(file.getOriginalFilename());
        }
      }

      String subject = "Documentos - " + client.getClientName();
      String emailBody = buildClientMessage(request, client);

      var whatsappContext =
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
          whatsappContext,
          attachments,
          fileNames);

    } catch (IOException e) {
      throw new NotificationException("Erro ao processar arquivos: " + e.getMessage());
    }
  }

  // Email/telefone deixaram de ser obrigatórios no cadastro do cliente, então
  // validamos aqui, antes de tentar enviar, em vez de deixar o
  // NotificationCoordinator/provedor externo estourar um erro genérico.
  private void requireContactForChannel(Client client, NotificationChannel channel) {
    boolean needsEmail =
        channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH;
    boolean needsWhatsApp =
        channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH;

    if (needsEmail && (client.getEmail() == null || client.getEmail().isBlank())) {
      throw new NotificationException(
          "Cliente \"" + client.getClientName() + "\" não possui e-mail cadastrado.");
    }
    if (needsWhatsApp && (client.getPhoneNumber() == null || client.getPhoneNumber().isBlank())) {
      throw new NotificationException(
          "Cliente \"" + client.getClientName() + "\" não possui telefone cadastrado.");
    }
  }

  // Público para que o front possa exibir a lista atual (configurada em ACCOUNTING_EMAIL) na tela
  // de notificações, sem precisar duplicar esse valor num env var próprio do front.
  public List<String> getAccountingRecipients() {
    List<String> recipients = new ArrayList<>();
    if (accountingEmail == null) {
      return recipients;
    }
    for (String email : accountingEmail.split(",")) {
      String trimmed = email.trim();
      if (!trimmed.isEmpty()) {
        recipients.add(trimmed);
      }
    }
    return recipients;
  }

  private String buildGenericFilesMessage(
      GenericFilesAccountingRequest request,
      BigDecimal discountedCardValue,
      BigDecimal cashValue,
      boolean hasFiles,
      int filesCount) {

    Map<String, String> variables = new HashMap<>();
    variables.put("CARD_VALUE", String.format("%.2f", discountedCardValue));
    variables.put("CASH_VALUE", String.format("%.2f", cashValue));

    boolean hasFinancialValues =
        discountedCardValue.compareTo(BigDecimal.ZERO) != 0
            || cashValue.compareTo(BigDecimal.ZERO) != 0;

    variables.put("HAS_FINANCIAL_VALUES", hasFinancialValues ? "true" : "");
    variables.put("NO_FINANCIAL_VALUES", hasFinancialValues ? "" : "true");

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

    return emailTemplateService.processTemplate("generic-files", variables);
  }

  private String buildClientMessage(ClientDocumentsRequest request, Client client) {

    Map<String, String> variables = new HashMap<>();
    variables.put("CLIENT_NAME", client.getClientName());

    LocalDate today = EmailGreetingUtil.today();
    variables.put("CURRENT_DATE", today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    variables.put("GREETING", EmailGreetingUtil.timeOfDayGreeting());
    variables.put("PERIOD_RANGE", EmailGreetingUtil.weekPeriodLabel(today));
    variables.put("SENDER_NAME", senderName);

    variables.put("DEFAULT_MESSAGE", "true");
    variables.put("CUSTOM_MESSAGE", request.customMessage() != null ? request.customMessage() : "");

    return emailTemplateService.processTemplate("client-documents", variables);
  }
}
