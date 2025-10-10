package com.hortifruti.sl.hortifruti.service.notification;

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

import java.util.List;

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

  public boolean sendTextMessage(String phoneNumber, String message) {
    try {
      String url = baseUrl + instanceId + "/messages/chat";

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + ultraMsgToken);
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("to", phoneNumber);
      body.add("body", message);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean sendDocument(String phoneNumber, String message, byte[] document, String fileName) {
    try {
      String url = baseUrl + instanceId + "/messages/document";

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + ultraMsgToken);
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("to", phoneNumber);
      body.add("caption", message);
      body.add("filename", fileName);
      
      // Criar um recurso temporário para o arquivo
      body.add("document", new org.springframework.core.io.ByteArrayResource(document) {
        @Override
        public String getFilename() {
          return fileName;
        }
      });

      HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean sendMultipleDocuments(String phoneNumber, String message, List<byte[]> documents, List<String> fileNames) {
    boolean allSent = true;
    
    // Enviar mensagem de texto primeiro
    sendTextMessage(phoneNumber, message);
    
    // Enviar cada documento
    for (int i = 0; i < documents.size() && i < fileNames.size(); i++) {
      boolean sent = sendDocument(phoneNumber, "Documento: " + fileNames.get(i), documents.get(i), fileNames.get(i));
      if (!sent) {
        allSent = false;
      }
    }
    
    return allSent;
  }
}