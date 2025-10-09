package com.hortifruti.sl.hortifruti.config.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
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

  /**
   * Obtém um token de acesso para a API do Sicoob.
   *
   * @return Token de acesso válido
   * @throws BilletException Se houver erro ao obter ou processar o token
   */
  public String getAccessToken() {
    try {
      // Verifica se o token atual ainda é válido
      if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 30000) {
        return accessToken;
      }

      // Configura os headers da requisição
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      // Configura o corpo da requisição
      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", "client_credentials");
      body.add("client_id", clientId);
      body.add("scope", scope);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

      // Faz a requisição para obter o token
      ResponseEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

      // Processa a resposta
      return processTokenResponse(response);

    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao obter token de acesso: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao obter token de acesso.", ex);
    }
  }

  /**
   * Processa a resposta da API para extrair o token de acesso.
   *
   * @param response Resposta da API
   * @return Token de acesso
   * @throws IOException Se houver erro ao processar a resposta
   * @throws BilletException Se o token não for encontrado ou a resposta for inválida
   */
  private String processTokenResponse(ResponseEntity<String> response) throws IOException {
    if (response.getBody() == null || response.getBody().trim().isEmpty()) {
      throw new BilletException("A resposta da API de autenticação está vazia ou nula.");
    }

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(response.getBody());

    // Verifica se o token está presente na resposta
    if (!root.has("access_token") || root.get("access_token").isNull()) {
      throw new BilletException("Token de acesso não encontrado na resposta da API.");
    }

    accessToken = root.path("access_token").asText();
    int expiresIn = root.path("expires_in").asInt();

    // Calcula o tempo de expiração do token
    tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);

    return accessToken;
  }
}
