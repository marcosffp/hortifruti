package com.hortifruti.sl.hortifruti.service.notification.whatsapp;

import com.hortifruti.sl.hortifruti.exception.notification.NotificationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Canal de envio (outbound) de mensagens e documentos via WhatsApp usando a API da UltraMsg. Não
 * há processamento de mensagens recebidas do cliente — apenas envio.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppService {

  @Value("${ultramsg.token}")
  private String ultraMsgToken;

  @Value("${ultramsg.instance.id}")
  private String instanceId;

  @Value("${ultramsg.base.url}")
  private String baseUrl;

  @Qualifier("genericRestTemplate")
  private final RestTemplate restTemplate;

  /**
   * Formatar número de telefone brasileiro para WhatsApp Converte para formato internacional:
   * +55DDNNNNNNNNN
   */
  private String formatPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
      throw new IllegalArgumentException("Número de telefone não pode ser vazio");
    }

    String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");

    // Se já começa com 55 (código do Brasil), assume que está correto
    if (cleanNumber.startsWith("55") && cleanNumber.length() >= 12) {
      return "+" + cleanNumber;
    }

    // Se tem 11 dígitos (DDD + número com 9), adiciona código do país
    if (cleanNumber.length() == 11) {
      return "+55" + cleanNumber;
    }

    // Se tem 10 dígitos (DDD + número sem 9), adiciona 9 e código do país
    if (cleanNumber.length() == 10) {
      String ddd = cleanNumber.substring(0, 2);
      String numero = cleanNumber.substring(2);
      return "+55" + ddd + "9" + numero;
    }

    // Se tem 9 dígitos (sem DDD), assume DDD 31 (Belo Horizonte)
    if (cleanNumber.length() == 9) {
      return "+5531" + cleanNumber;
    }

    // Se tem 8 dígitos (sem DDD e sem 9), adiciona 9 e assume DDD 31
    if (cleanNumber.length() == 8) {
      return "+55319" + cleanNumber;
    }

    throw new IllegalArgumentException(
        "Formato de número de telefone não reconhecido: " + phoneNumber);
  }

  public boolean sendTextMessage(String phoneNumber, String message) {
    try {
      String formattedPhone = formatPhoneNumber(phoneNumber);
      String url = baseUrl + instanceId + "/messages/chat?token=" + ultraMsgToken;

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("to", formattedPhone);
      body.add("body", message);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      String responseBody = response.getBody();
      boolean hasError = responseBody != null && responseBody.contains("\"error\"");

      return response.getStatusCode().is2xxSuccessful() && !hasError;
    } catch (Exception e) {
      throw new NotificationException(
          "Erro ao enviar mensagem WhatsApp para " + phoneNumber + ": " + e.getMessage());
    }
  }

  public boolean sendDocument(
      String phoneNumber, String message, byte[] document, String fileName) {
    try {
      String formattedPhone = formatPhoneNumber(phoneNumber);
      String url = baseUrl + instanceId + "/messages/document?token=" + ultraMsgToken;

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      String documentBase64 = java.util.Base64.getEncoder().encodeToString(document);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("to", formattedPhone);
      body.add("filename", fileName);
      body.add("document", documentBase64);
      if (message != null && !message.isEmpty()) {
        body.add("caption", message);
      }

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      String responseBody = response.getBody();
      boolean hasError = responseBody != null && responseBody.contains("\"error\"");

      if (hasError) {
        throw new NotificationException(
            "Erro ao enviar documento WhatsApp para " + phoneNumber + ": " + responseBody);
      }

      return response.getStatusCode().is2xxSuccessful() && !hasError;
    } catch (Exception e) {
      throw new NotificationException(
          "Erro ao enviar documento WhatsApp para " + phoneNumber + ": " + e.getMessage());
    }
  }

  public boolean sendMultipleDocuments(
      String phoneNumber, String message, List<byte[]> documents, List<String> fileNames) {

    if (documents == null || fileNames == null) {
      throw new NotificationException("Listas de documentos ou nomes são nulas!");
    }

    if (documents.isEmpty()) {
      throw new NotificationException("Lista de documentos está vazia!");
    }

    if (documents.size() != fileNames.size()) {
      throw new NotificationException("Listas de documentos e nomes têm tamanhos diferentes!");
    }

    sendTextMessage(phoneNumber, message);

    boolean allSent = true;

    for (int i = 0; i < documents.size(); i++) {

      boolean sent =
          sendDocument(
              phoneNumber, "Documento: " + fileNames.get(i), documents.get(i), fileNames.get(i));

      if (!sent) {
        allSent = false;
      }

      // Delay de 2 segundos entre documentos para evitar rate limit
      if (i < documents.size() - 1) {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          throw new NotificationException("Envio interrompido: " + e.getMessage());
        }
      }
    }

    return allSent;
  }
}
