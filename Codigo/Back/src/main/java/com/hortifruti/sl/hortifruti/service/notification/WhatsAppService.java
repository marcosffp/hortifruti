package com.hortifruti.sl.hortifruti.service.notification;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

  @Value("${ultramsg.token}")
  private String ultraMsgToken;

  @Value("${ultramsg.instance.id}")
  private String instanceId;

  @Value("${ultramsg.base.url}")
  private String baseUrl;

  private final RestTemplate restTemplate;

  /**
   * Formatar número de telefone brasileiro para WhatsApp Converte para formato internacional:
   * +55DDNNNNNNNNN
   */
  private String formatPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
      throw new IllegalArgumentException("Número de telefone não pode ser vazio");
    }

    // Remove todos os caracteres não numéricos
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

    // Se não conseguiu formatar, lança exceção
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

      System.out.println("WhatsApp - Número original: " + phoneNumber);
      System.out.println("WhatsApp - Número formatado: " + formattedPhone);
      System.out.println("WhatsApp - URL: " + url.replace(ultraMsgToken, "***TOKEN***"));
      System.out.println("WhatsApp - Status response: " + response.getStatusCode());
      System.out.println("WhatsApp - Response body: " + response.getBody());

      // Verificar se tem erro na resposta
      String responseBody = response.getBody();
      boolean hasError = responseBody != null && responseBody.contains("\"error\"");

      return response.getStatusCode().is2xxSuccessful() && !hasError;
    } catch (Exception e) {
      System.err.println("Erro ao enviar WhatsApp para " + phoneNumber + ": " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public boolean sendDocument(
      String phoneNumber, String message, byte[] document, String fileName) {
    try {
      String formattedPhone = formatPhoneNumber(phoneNumber);
      String url = baseUrl + instanceId + "/messages/document?token=" + ultraMsgToken;

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      // Converter documento para base64
      String documentBase64 = java.util.Base64.getEncoder().encodeToString(document);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("to", formattedPhone);
      body.add("filename", fileName);
      body.add("document", documentBase64);
      if (message != null && !message.isEmpty()) {
        body.add("caption", message);
      }

      System.out.println("WhatsApp Document - Dados enviados:");
      System.out.println("  to: " + formattedPhone);
      System.out.println("  filename: " + fileName);
      System.out.println("  caption: " + message);
      System.out.println("  document size: " + document.length + " bytes");
      System.out.println("  document base64 length: " + documentBase64.length() + " chars");

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      System.out.println("WhatsApp Document - Número original: " + phoneNumber);
      System.out.println("WhatsApp Document - Número formatado: " + formattedPhone);
      System.out.println("WhatsApp Document - URL: " + url.replace(ultraMsgToken, "***TOKEN***"));
      System.out.println("WhatsApp Document - Status response: " + response.getStatusCode());
      System.out.println("WhatsApp Document - Response body: " + response.getBody());

      // Verificar se tem erro na resposta
      String responseBody = response.getBody();
      boolean hasError = responseBody != null && responseBody.contains("\"error\"");

      return response.getStatusCode().is2xxSuccessful() && !hasError;
    } catch (Exception e) {
      System.err.println(
          "Erro ao enviar documento WhatsApp para " + phoneNumber + ": " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public boolean sendMultipleDocuments(
      String phoneNumber, String message, List<byte[]> documents, List<String> fileNames) {
    boolean allSent = true;

    // Enviar mensagem de texto primeiro
    sendTextMessage(phoneNumber, message);

    // Enviar cada documento
    for (int i = 0; i < documents.size() && i < fileNames.size(); i++) {
      boolean sent =
          sendDocument(
              phoneNumber, "Documento: " + fileNames.get(i), documents.get(i), fileNames.get(i));
      if (!sent) {
        allSent = false;
      }
    }

    return allSent;
  }

  /**
   * Método genérico para envio de mensagens Usado pelo NotificationService para enviar mensagens
   * simples
   */
  public boolean sendMessage(String phoneNumber, String message) {
    return sendTextMessage(phoneNumber, message);
  }

  /**
   * Método para enviar mensagem com anexos Usado pelo NotificationService para enviar mensagens com
   * documentos
   */
  public boolean sendMessage(
      String phoneNumber, String message, List<byte[]> attachments, List<String> fileNames) {
    if (attachments == null || attachments.isEmpty()) {
      return sendTextMessage(phoneNumber, message);
    }
    return sendMultipleDocuments(phoneNumber, message, attachments, fileNames);
  }
}
