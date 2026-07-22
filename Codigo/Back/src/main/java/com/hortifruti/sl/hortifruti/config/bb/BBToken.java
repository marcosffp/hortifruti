package com.hortifruti.sl.hortifruti.config.bb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.exception.BBApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/** Obtem e cacheia o access_token OAuth2 (client_credentials) da API do BB. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BBToken {

  private volatile String accessToken;
  private volatile long tokenExpiresAt;

  @Value("${bb.ambiente}")
  private String ambiente;

  @Value("${bb.basic}")
  private String basicAuth;

  @Value("${bb.scope}")
  private String scope;

  @Qualifier("bbRestTemplate")
  private final RestTemplate restTemplate;

  private final ObjectMapper objectMapper;

  public synchronized String getAccessToken() {
    if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 30_000) {
      return accessToken;
    }

    try {
      String url = BBEndpoints.oauthTokenUrl(ambiente);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", "client_credentials");
      if (scope != null && !scope.isBlank()) {
        body.add("scope", scope);
      }

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      processTokenResponse(response);
      return accessToken;

    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      log.error("Falha ao obter access_token do BB: {}", ex.getResponseBodyAsString(), ex);
      throw new BBApiException("Falha ao autenticar com a API do BB.");
    } catch (BBApiException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Erro inesperado ao obter access_token do BB.", ex);
      throw new BBApiException("Erro inesperado ao autenticar com a API do BB.");
    }
  }

  public synchronized void invalidateToken() {
    this.accessToken = null;
    this.tokenExpiresAt = 0;
  }

  private void processTokenResponse(ResponseEntity<String> response) {
    if (response.getBody() == null || response.getBody().isBlank()) {
      throw new BBApiException("Resposta de token vazia da API do BB.");
    }

    try {
      JsonNode json = objectMapper.readTree(response.getBody());

      if (!json.hasNonNull("access_token")) {
        throw new BBApiException("Resposta de token da API do BB sem access_token.");
      }

      this.accessToken = json.get("access_token").asText();
      long expiresInSeconds =
          json.hasNonNull("expires_in") ? json.get("expires_in").asLong(600) : 600;
      this.tokenExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000);
    } catch (BBApiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BBApiException("Erro ao interpretar resposta de token da API do BB.", ex);
    }
  }
}
