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

  @Qualifier("billetRestTemplate")
  private final RestTemplate restTemplate;

  private final SicoobToken sicoobToken;

  private final ObjectMapper objectMapper;

  public JsonNode get(String endpoint) throws IOException {
    try {
      return processResponse(doGet(endpoint));
    } catch (HttpClientErrorException.Unauthorized ex) {
      sicoobToken.invalidateToken();
      try {
        return processResponse(doGet(endpoint));
      } catch (HttpClientErrorException | HttpServerErrorException retryEx) {
        throw new BilletException(
            "Erro GET após renovação de token: " + retryEx.getResponseBodyAsString(), retryEx);
      } catch (Exception retryEx) {
        throw new BilletException("Erro inesperado GET após renovação de token.", retryEx);
      }
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição GET: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição GET.", ex);
    }
  }

  public JsonNode post(String endpoint, Object body) throws IOException {
    try {
      return processResponse(doPost(endpoint, body));
    } catch (HttpClientErrorException.Unauthorized ex) {
      sicoobToken.invalidateToken();
      try {
        return processResponse(doPost(endpoint, body));
      } catch (HttpClientErrorException | HttpServerErrorException retryEx) {
        throw new BilletException(
            "Erro POST após renovação de token: " + retryEx.getResponseBodyAsString(), retryEx);
      } catch (Exception retryEx) {
        throw new BilletException("Erro inesperado POST após renovação de token.", retryEx);
      }
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      System.err.println("Erro HTTP - Status: " + ex.getStatusCode());
      System.err.println("Resposta do servidor: " + ex.getResponseBodyAsString());
      throw new BilletException(
          "Erro ao realizar requisição POST: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição POST.", ex);
    }
  }

  public JsonNode postCancel(String endpoint, Object body) throws IOException {
    try {
      ResponseEntity<String> response = doPost(endpoint, body);
      if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
        return null;
      }
      return processResponse(response);
    } catch (HttpClientErrorException.Unauthorized ex) {
      sicoobToken.invalidateToken();
      try {
        ResponseEntity<String> response = doPost(endpoint, body);
        if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
          return null;
        }
        return processResponse(response);
      } catch (HttpClientErrorException | HttpServerErrorException retryEx) {
        throw new BilletException(
            "Erro POST-cancel após renovação de token: " + retryEx.getResponseBodyAsString(),
            retryEx);
      } catch (Exception retryEx) {
        throw new BilletException("Erro inesperado POST-cancel após renovação de token.", retryEx);
      }
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição POST: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição POST.", ex);
    }
  }

  public ResponseEntity<String> put(String endpoint, Object body) throws IOException {
    try {
      return doPut(endpoint, body);
    } catch (HttpClientErrorException.Unauthorized ex) {
      sicoobToken.invalidateToken();
      try {
        return doPut(endpoint, body);
      } catch (HttpClientErrorException | HttpServerErrorException retryEx) {
        throw new BilletException(
            "Erro PUT após renovação de token: " + retryEx.getResponseBodyAsString(), retryEx);
      } catch (Exception retryEx) {
        throw new BilletException("Erro inesperado PUT após renovação de token.", retryEx);
      }
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição PUT: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição PUT.", ex);
    }
  }

  public ResponseEntity<String> delete(String endpoint) throws IOException {
    try {
      return doDelete(endpoint);
    } catch (HttpClientErrorException.Unauthorized ex) {
      sicoobToken.invalidateToken();
      try {
        return doDelete(endpoint);
      } catch (HttpClientErrorException | HttpServerErrorException retryEx) {
        throw new BilletException(
            "Erro DELETE após renovação de token: " + retryEx.getResponseBodyAsString(), retryEx);
      } catch (Exception retryEx) {
        throw new BilletException("Erro inesperado DELETE após renovação de token.", retryEx);
      }
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição DELETE: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição DELETE.", ex);
    }
  }

  public ResponseEntity<JsonNode> getWithResponse(String endpoint) throws IOException {
    try {
      return toJsonResponse(doGet(endpoint));
    } catch (HttpClientErrorException.Unauthorized ex) {
      sicoobToken.invalidateToken();
      try {
        return toJsonResponse(doGet(endpoint));
      } catch (HttpClientErrorException | HttpServerErrorException retryEx) {
        throw new BilletException(
            "Erro GET após renovação de token: " + retryEx.getResponseBodyAsString(), retryEx);
      } catch (Exception retryEx) {
        throw new BilletException("Erro inesperado GET após renovação de token.", retryEx);
      }
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw new BilletException(
          "Erro ao realizar requisição GET: " + ex.getResponseBodyAsString(), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição GET.", ex);
    }
  }

  private ResponseEntity<String> doGet(String endpoint) throws IOException {
    HttpEntity<String> entity = new HttpEntity<>(createHeaders());
    return restTemplate.exchange(apiUrl + endpoint, HttpMethod.GET, entity, String.class);
  }

  private ResponseEntity<String> doPost(String endpoint, Object body) throws IOException {
    String jsonBody = objectMapper.writeValueAsString(body);
    HttpEntity<String> entity = new HttpEntity<>(jsonBody, createHeaders());
    return restTemplate.postForEntity(apiUrl + endpoint, entity, String.class);
  }

  private ResponseEntity<String> doPut(String endpoint, Object body) throws IOException {
    String jsonBody = objectMapper.writeValueAsString(body);
    HttpEntity<String> entity = new HttpEntity<>(jsonBody, createHeaders());
    return restTemplate.exchange(apiUrl + endpoint, HttpMethod.PUT, entity, String.class);
  }

  private ResponseEntity<String> doDelete(String endpoint) throws IOException {
    HttpEntity<String> entity = new HttpEntity<>(createHeaders());
    return restTemplate.exchange(apiUrl + endpoint, HttpMethod.DELETE, entity, String.class);
  }

  private HttpHeaders createHeaders() throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(sicoobToken.getAccessToken());
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", "application/json");
    return headers;
  }

  private JsonNode processResponse(ResponseEntity<String> response) throws IOException {
    if (response.getBody() == null) {
      throw new BilletException("A resposta da API está nula.");
    }
    return objectMapper.readTree(response.getBody());
  }

  private ResponseEntity<JsonNode> toJsonResponse(ResponseEntity<String> stringResponse)
      throws IOException {
    if (stringResponse.getBody() == null) {
      throw new BilletException("A resposta da API está nula.");
    }
    JsonNode jsonNode = objectMapper.readTree(stringResponse.getBody());
    return ResponseEntity.status(stringResponse.getStatusCode())
        .headers(stringResponse.getHeaders())
        .body(jsonNode);
  }

  public String getApiUrl() {
    return this.apiUrl;
  }
}
