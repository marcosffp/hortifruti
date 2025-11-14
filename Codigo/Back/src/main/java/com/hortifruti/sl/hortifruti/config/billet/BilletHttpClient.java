package com.hortifruti.sl.hortifruti.config.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class BilletHttpClient {

  @Value("${sicoob.api.url}")
  private String apiUrl;

  private final RestTemplate restTemplate;
  private final SicoobToken sicoobToken;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JsonNode get(String endpoint) throws IOException {
    try {
      HttpHeaders headers = createHeaders();
      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<String> response =
          restTemplate.exchange(apiUrl + endpoint, HttpMethod.GET, entity, String.class);

      return processResponse(response);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição GET: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição GET.", ex);
    }
  }

  public JsonNode post(String endpoint, Object body) throws IOException {
    try {
      HttpHeaders headers = createHeaders();
      HttpEntity<Object> entity = new HttpEntity<>(body, headers);

      ResponseEntity<String> response =
          restTemplate.postForEntity(apiUrl + endpoint, entity, String.class);

      return processResponse(response);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição POST: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição POST.", ex);
    }
  }

  public JsonNode postCancel(String endpoint, Object body) throws IOException {
    try {
      HttpHeaders headers = createHeaders();
      HttpEntity<Object> entity = new HttpEntity<>(body, headers);

      ResponseEntity<String> response =
          restTemplate.postForEntity(apiUrl + endpoint, entity, String.class);

      if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
        return null;
      }

      return processResponse(response);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição POST: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição POST.", ex);
    }
  }

  public ResponseEntity<String> put(String endpoint, Object body) throws IOException {
    try {
      HttpHeaders headers = createHeaders();
      HttpEntity<Object> entity = new HttpEntity<>(body, headers);

      return restTemplate.exchange(apiUrl + endpoint, HttpMethod.PUT, entity, String.class);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição PUT: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição PUT.", ex);
    }
  }

  public ResponseEntity<String> delete(String endpoint) throws IOException {
    try {
      HttpHeaders headers = createHeaders();
      HttpEntity<String> entity = new HttpEntity<>(headers);

      return restTemplate.exchange(apiUrl + endpoint, HttpMethod.DELETE, entity, String.class);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição DELETE: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição DELETE.", ex);
    }
  }

  public ResponseEntity<JsonNode> getWithResponse(String endpoint) throws IOException {
    try {
      HttpHeaders headers = createHeaders();
      HttpEntity<String> entity = new HttpEntity<>(headers);

      return restTemplate.exchange(apiUrl + endpoint, HttpMethod.GET, entity, JsonNode.class);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição GET: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição GET.", ex);
    }
  }

  private HttpHeaders createHeaders() throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(sicoobToken.getAccessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private JsonNode processResponse(ResponseEntity<String> response) throws IOException {
    if (response.getBody() == null) {
      throw new BilletException("A resposta da API está nula.");
    }
    return objectMapper.readTree(response.getBody());
  }

  public String getApiUrl() {
    return this.apiUrl;
  }
}
