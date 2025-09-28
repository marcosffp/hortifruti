package com.hortifruti.sl.hortifruti.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class SicoobService {

  @Value("${sicoob.client.id}")
  private String clientId;

  @Value("${sicoob.auth.url}")
  private String authUrl;

  @Value("${sicoob.api.url}")
  private String apiUrl;

  @Value("${sicoob.scope}")
  private String scope;

  private final RestTemplate restTemplate;
  private String accessToken;
  private long tokenExpiresAt;

  @Autowired
  public SicoobService(@Qualifier("sicoobRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Obtém um token de acesso para a API do Sicoob
   * 
   * @return Token de acesso
   * @throws IOException Se houver erro ao processar a resposta
   */
  public String getAccessToken() throws IOException {
    // Verificar se o token ainda é válido
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

    // Parse da resposta para extrair o token
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(response.getBody());

    accessToken = root.path("access_token").asText();
    int expiresIn = root.path("expires_in").asInt();
    tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);

    return accessToken;
  }

  /**
   * Faz uma requisição GET para um endpoint da API do Sicoob
   * 
   * @param endpoint Endpoint da API
   * @return Resposta da API
   * @throws IOException Se houver erro na comunicação ou no processamento da
   *                     resposta
   */
  public JsonNode get(String endpoint) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.getForEntity(apiUrl + endpoint, String.class, entity);

    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(response.getBody());
  }

  /**
   * Faz uma requisição POST para um endpoint da API do Sicoob
   * 
   * @param endpoint Endpoint da API
   * @param body     Corpo da requisição
   * @return Resposta da API
   * @throws IOException Se houver erro na comunicação ou no processamento da
   *                     resposta
   */
  public JsonNode post(String endpoint, Object body) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Object> entity = new HttpEntity<>(body, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(apiUrl + endpoint, entity, String.class);

    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(response.getBody());
  }

  /**
   * Emite um boleto através da API do Sicoob
   * 
   * @param boleto Dados do boleto a ser emitido
   * @return Resposta da API contendo os detalhes do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da
   *                     resposta
   */
  public JsonNode emitirBoleto(JsonNode boleto) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<JsonNode> entity = new HttpEntity<>(boleto, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(
        apiUrl + "/cobranca-bancaria/v3/boletos", entity, String.class);

    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(response.getBody());
  }
}