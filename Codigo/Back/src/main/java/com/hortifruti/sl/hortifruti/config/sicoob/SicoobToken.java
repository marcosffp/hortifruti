package com.hortifruti.sl.hortifruti.config.sicoob;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SicoobToken {
  private String accessToken;
  private long tokenExpiresAt;

  @Value("${sicoob.client.id}")
  private String clientId;

  @Value("${sicoob.auth.url}")
  private String authUrl;

  @Value("${sicoob.scope}")
  private String scope;

  @Qualifier("sicoobRestTemplate")
  private final RestTemplate restTemplate;

  /**
   * Obtém um token de acesso para a API do Sicoob
   * 
   * @return Token de acesso
   * @throws IOException Se houver erro ao processar a resposta
   */
  public String getAccessToken() throws IOException {
    if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 30000) {
      return accessToken;
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "client_credentials");
    body.add("client_id", clientId);
    body.add("scope", scope);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(response.getBody());

    accessToken = root.path("access_token").asText();
    int expiresIn = root.path("expires_in").asInt();
    tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);

    return accessToken;
  }
}
