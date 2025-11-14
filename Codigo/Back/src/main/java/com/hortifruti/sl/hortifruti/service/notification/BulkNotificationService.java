package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.dto.notification.BulkNotificationResponse;
import com.hortifruti.sl.hortifruti.dto.notification.NotificationResponse;
import com.hortifruti.sl.hortifruti.model.enumeration.NotificationChannel;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import java.io.IOException;
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
public class BulkNotificationService {

  private static final String CUSTOM_MESSAGE = "CUSTOM_MESSAGE";
  private static final String DEFAULT_MESSAGE = "DEFAULT_MESSAGE";
  private static final String EMPTY_STRING = "";

  private final NotificationCoordinator notificationCoordinator;
  private final ClientRepository clientRepository;
  private final EmailTemplateService emailTemplateService;

  @Value("${accounting.email}")
  private String accountingEmail;

  /** Envia notificações em massa para múltiplos destinatários */
  public BulkNotificationResponse sendBulkNotifications(
      List<MultipartFile> files,
      List<Long> clientIds,
      List<String> channels,
      String destinationType,
      String customMessage) {
    try {
      // Validações
      if (files == null || files.isEmpty()) {
        throw new IllegalArgumentException("Pelo menos um arquivo deve ser fornecido");
      }

      if (channels == null || channels.isEmpty()) {
        throw new IllegalArgumentException("Pelo menos um canal deve ser selecionado");
      }

      // Converter arquivos para bytes
      List<byte[]> fileContents = new ArrayList<>();
      List<String> fileNames = new ArrayList<>();

      for (MultipartFile file : files) {
        fileContents.add(file.getBytes());
        fileNames.add(file.getOriginalFilename());
      }

      // Determinar canais
      boolean sendEmail = channels.contains("email");
      boolean sendWhatsApp = channels.contains("whatsapp");
      NotificationChannel channel = determineChannel(sendEmail, sendWhatsApp);

      // Enviar para contabilidade ou clientes
      if ("contabilidade".equalsIgnoreCase(destinationType)) {
        return sendToAccounting(fileContents, fileNames, customMessage, channel);
      } else {
        return sendToClients(clientIds, fileContents, fileNames, customMessage, channel);
      }

    } catch (IOException e) {
      return BulkNotificationResponse.failure(
          "Erro ao processar arquivos: " + e.getMessage(), List.of());
    } catch (Exception e) {
      return BulkNotificationResponse.failure(
          "Erro ao enviar notificações: " + e.getMessage(), List.of());
    }
  }

  /** Envia notificações para a contabilidade */
  private BulkNotificationResponse sendToAccounting(
      List<byte[]> fileContents,
      List<String> fileNames,
      String customMessage,
      NotificationChannel channel) {
    try {
      String subject = String.format("Documentos Contábeis - %d arquivo(s)", fileContents.size());
      String emailBody = buildAccountingMessage(fileContents.size(), customMessage);

      boolean success =
          switch (channel) {
            case EMAIL ->
                notificationCoordinator.sendEmailOnly(
                    accountingEmail, subject, emailBody, fileContents, fileNames);
            case WHATSAPP, BOTH -> {
              var whatsAppContext =
                  NotificationCoordinator.WhatsAppMessageContext.builder()
                      .customMessage(customMessage)
                      .subject(subject);

              NotificationResponse response =
                  notificationCoordinator.sendNotification(
                      accountingEmail,
                      channel,
                      subject,
                      emailBody,
                      NotificationCoordinator.WhatsAppMessageType.GENERIC_FILES,
                      whatsAppContext,
                      fileContents,
                      fileNames);
              yield response.success();
            }
          };

      if (success) {
        String channelText = getChannelText(channel);
        return BulkNotificationResponse.success(
            1,
            String.format(
                "%d arquivo(s) enviado(s) para contabilidade via %s",
                fileContents.size(), channelText));
      } else {
        return BulkNotificationResponse.failure(
            "Falha ao enviar para contabilidade", List.of("Contabilidade"));
      }

    } catch (Exception e) {
      return BulkNotificationResponse.failure(
          "Erro ao enviar para contabilidade: " + e.getMessage(), List.of("Contabilidade"));
    }
  }

  /** Envia notificações para múltiplos clientes */
  private BulkNotificationResponse sendToClients(
      List<Long> clientIds,
      List<byte[]> fileContents,
      List<String> fileNames,
      String customMessage,
      NotificationChannel channel) {
    if (clientIds == null || clientIds.isEmpty()) {
      throw new IllegalArgumentException("Pelo menos um cliente deve ser selecionado");
    }

    int successCount = 0;
    List<String> failedRecipients = new ArrayList<>();

    for (Long clientId : clientIds) {
      try {
        Optional<Client> clientOpt = clientRepository.findById(clientId);

        if (clientOpt.isEmpty()) {
          failedRecipients.add("Cliente ID: " + clientId);
          continue;
        }

        Client client = clientOpt.get();

        // Validar se o cliente tem os contatos necessários
        if (channel == NotificationChannel.EMAIL
            && (client.getEmail() == null || client.getEmail().isEmpty())) {
          failedRecipients.add(client.getClientName() + " (sem e-mail)");
          continue;
        }

        if (channel == NotificationChannel.WHATSAPP
            && (client.getPhoneNumber() == null || client.getPhoneNumber().isEmpty())) {
          failedRecipients.add(client.getClientName() + " (sem telefone)");
          continue;
        }

        if (channel == NotificationChannel.BOTH) {
          if ((client.getEmail() == null || client.getEmail().isEmpty())
              && (client.getPhoneNumber() == null || client.getPhoneNumber().isEmpty())) {
            failedRecipients.add(client.getClientName() + " (sem contatos)");
            continue;
          }
        }

        // Enviar notificação
        String subject = String.format("Documentos - %s", client.getClientName());
        String emailBody = buildClientMessage(client, fileContents.size(), customMessage);

        var whatsAppContext =
            NotificationCoordinator.WhatsAppMessageContext.builder()
                .client(client)
                .customMessage(customMessage);

        NotificationResponse response =
            notificationCoordinator.sendNotification(
                client.getEmail(),
                client.getPhoneNumber(),
                channel,
                subject,
                emailBody,
                NotificationCoordinator.WhatsAppMessageType.CLIENT_DOCUMENTS,
                whatsAppContext,
                fileContents,
                fileNames);

        if (response.success()) {
          successCount++;
        } else {
          failedRecipients.add(client.getClientName());
        }

      } catch (Exception e) {
        failedRecipients.add("Cliente ID: " + clientId + " (erro)");
      }
    }

    // Construir resposta
    if (successCount == 0) {
      return BulkNotificationResponse.failure(
          "Nenhuma notificação foi enviada com sucesso", failedRecipients);
    } else if (failedRecipients.isEmpty()) {
      String channelText = getChannelText(channel);
      return BulkNotificationResponse.success(
          successCount,
          String.format(
              "%d arquivo(s) enviado(s) com sucesso para %d destinatário(s) via %s",
              fileContents.size(), successCount, channelText));
    } else {
      return BulkNotificationResponse.partial(
          successCount, failedRecipients.size(), failedRecipients);
    }
  }

  /** Constrói mensagem para contabilidade */
  private String buildAccountingMessage(int filesCount, String customMessage) {
    Map<String, String> variables = new HashMap<>();
    variables.put("FILES_COUNT", String.valueOf(filesCount));

    if (customMessage != null && !customMessage.isEmpty()) {
      variables.put(CUSTOM_MESSAGE, customMessage);
      variables.put(DEFAULT_MESSAGE, EMPTY_STRING);
    } else {
      variables.put(CUSTOM_MESSAGE, EMPTY_STRING);
      variables.put(DEFAULT_MESSAGE, "true");
    }

    return emailTemplateService.processTemplate("accounting-documents", variables);
  }

  /** Constrói mensagem para cliente */
  private String buildClientMessage(Client client, int filesCount, String customMessage) {
    Map<String, String> variables = new HashMap<>();
    variables.put("CLIENT_NAME", client.getClientName());
    variables.put("FILES_COUNT", String.valueOf(filesCount));

    // Adicionar data atual
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.format.DateTimeFormatter formatter =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    variables.put("CURRENT_DATE", today.format(formatter));

    if (customMessage != null && !customMessage.isEmpty()) {
      variables.put(CUSTOM_MESSAGE, customMessage);
      variables.put(DEFAULT_MESSAGE, EMPTY_STRING);
    } else {
      variables.put(CUSTOM_MESSAGE, EMPTY_STRING);
      variables.put(DEFAULT_MESSAGE, "true");
    }

    return emailTemplateService.processTemplate("client-documents", variables);
  }

  /** Determina o canal baseado nas seleções */
  private NotificationChannel determineChannel(boolean sendEmail, boolean sendWhatsApp) {
    if (sendEmail && sendWhatsApp) {
      return NotificationChannel.BOTH;
    } else if (sendEmail) {
      return NotificationChannel.EMAIL;
    } else if (sendWhatsApp) {
      return NotificationChannel.WHATSAPP;
    } else {
      throw new IllegalArgumentException("Pelo menos um canal deve ser selecionado");
    }
  }

  /** Retorna texto descritivo do canal */
  private String getChannelText(NotificationChannel channel) {
    return switch (channel) {
      case EMAIL -> "e-mail";
      case WHATSAPP -> "WhatsApp";
      case BOTH -> "e-mail e WhatsApp";
    };
  }
}
