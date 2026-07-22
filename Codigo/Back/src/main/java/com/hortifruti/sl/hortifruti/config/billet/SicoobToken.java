package com.hortifruti.sl.hortifruti.config.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import java.io.IOException;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
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

  @Qualifier("billetRestTemplate")
  private final RestTemplate restTemplate;

  private final ObjectMapper objectMapper;

  public synchronized String getAccessToken() {
    long startedAt = System.currentTimeMillis();
    try {
      if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 30000) {
        return accessToken;
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", "client_credentials");
      body.add("client_id", clientId);
      body.add("scope", scope);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

      startedAt = System.currentTimeMillis();
      ResponseEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

      processTokenResponse(response);

      return accessToken;

    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      long elapsedMs = System.currentTimeMillis() - startedAt;
      log.error("[Sicoob][token] falha HTTP {} elapsedMs={}", ex.getStatusCode(), elapsedMs);
      throw new BilletException("Erro ao obter token de acesso ao Sicoob.", ex);
    } catch (ResourceAccessException ex) {
      long elapsedMs = System.currentTimeMillis() - startedAt;
      Throwable rootCause = ex.getCause();
      String causeClass =
          rootCause != null ? rootCause.getClass().getSimpleName() : ex.getClass().getSimpleName();
      log.error(
          "[Sicoob][token] falha de rede/TLS - causaRaiz={} elapsedMs={}", causeClass, elapsedMs);
      throw new BilletException("Erro de rede/TLS ao obter token de acesso ao Sicoob.", ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao obter token de acesso.", ex);
    }
  }

  public synchronized void invalidateToken() {
    this.accessToken = null;
    this.tokenExpiresAt = 0;
  }

  private void processTokenResponse(ResponseEntity<String> response) throws IOException {
    if (response.getBody() == null || response.getBody().trim().isEmpty()) {
      throw new BilletException("Resposta de token vazia do servidor.");
    }

    JsonNode jsonResponse = objectMapper.readTree(response.getBody());

    if (!jsonResponse.has("access_token") || jsonResponse.get("access_token").isNull()) {
      throw new BilletException("Token de acesso não encontrado na resposta.");
    }

    this.accessToken = jsonResponse.get("access_token").asText();

    // Usa o expires_in retornado pela API; se ausente, assume 55 minutos como fallback
    long expiresInSeconds =
        jsonResponse.has("expires_in") ? jsonResponse.get("expires_in").asLong(3300) : 3300;
    this.tokenExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000);
  }
}
