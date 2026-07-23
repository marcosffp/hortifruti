package com.hortifruti.sl.hortifruti.config.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BilletHttpClient {

  @Value("${sicoob.api.url}")
  private String apiUrl;

  @Qualifier("billetRestTemplate")
  private final RestTemplate restTemplate;

  private final SicoobToken sicoobToken;

  private final ObjectMapper objectMapper;

  @FunctionalInterface
  private interface RequestCall<T> {
    T get() throws IOException;
  }

  public JsonNode get(String endpoint) throws IOException {
    return executeWithRetry("GET", "GET", endpoint, () -> processResponse(doGet(endpoint)));
  }

  public JsonNode post(String endpoint, Object body) throws IOException {
    return executeWithRetry(
        "POST", "POST", endpoint, () -> processResponse(doPost(endpoint, body)));
  }

  public JsonNode postCancel(String endpoint, Object body) throws IOException {
    return executeWithRetry(
        "POST-cancel",
        "POST",
        endpoint,
        () -> {
          ResponseEntity<String> response = doPost(endpoint, body);
          if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
            return null;
          }
          return processResponse(response);
        });
  }

  public ResponseEntity<String> put(String endpoint, Object body) throws IOException {
    return executeWithRetry("PUT", "PUT", endpoint, () -> doPut(endpoint, body));
  }

  public ResponseEntity<String> delete(String endpoint) throws IOException {
    return executeWithRetry("DELETE", "DELETE", endpoint, () -> doDelete(endpoint));
  }

  public ResponseEntity<JsonNode> getWithResponse(String endpoint) throws IOException {
    return executeWithRetry("GET", "GET", endpoint, () -> toJsonResponse(doGet(endpoint)));
  }

  /**
   * Executa {@code call}, e caso a resposta seja 401, invalida o token e tenta uma única vez de
   * novo antes de embrulhar a falha em {@link BilletException}. {@code label} identifica a operação
   * nos logs/mensagens de retry (ex.: "POST-cancel"); {@code verb} é o termo usado nas mensagens de
   * falha na primeira tentativa (ex.: "POST", mesmo para {@code postCancel}).
   */
  private <T> T executeWithRetry(String label, String verb, String endpoint, RequestCall<T> call)
      throws IOException {
    long startedAt = System.currentTimeMillis();
    try {
      return call.get();
    } catch (HttpClientErrorException.Unauthorized ex) {
      sicoobToken.invalidateToken();
      long retryStartedAt = System.currentTimeMillis();
      try {
        return call.get();
      } catch (HttpClientErrorException | HttpServerErrorException retryEx) {
        logHttpFailure(label + " " + endpoint + " (retry)", retryEx, retryStartedAt);
        throw new BilletException(
            "Erro " + label + " após renovação de token. " + extractSicoobMessage(retryEx),
            retryEx);
      } catch (Exception retryEx) {
        throw new BilletException(
            "Erro inesperado " + label + " após renovação de token.", retryEx);
      }
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      logHttpFailure(label + " " + endpoint, ex, startedAt);
      throw new BilletException(
          "Erro ao realizar requisição " + verb + " ao Sicoob. " + extractSicoobMessage(ex), ex);
    } catch (Exception ex) {
      throw new BilletException("Erro inesperado ao realizar requisição " + verb + ".", ex);
    }
  }

  private ResponseEntity<String> doGet(String endpoint) throws IOException {
    long startedAt = System.currentTimeMillis();
    try {
      HttpEntity<String> entity = new HttpEntity<>(createHeaders());
      return restTemplate.exchange(apiUrl + endpoint, HttpMethod.GET, entity, String.class);
    } catch (ResourceAccessException ex) {
      logNetworkFailure("GET " + endpoint, ex, startedAt);
      throw ex;
    }
  }

  private ResponseEntity<String> doPost(String endpoint, Object body) throws IOException {
    long startedAt = System.currentTimeMillis();
    try {
      String jsonBody = objectMapper.writeValueAsString(body);
      HttpEntity<String> entity = new HttpEntity<>(jsonBody, createHeaders());
      return restTemplate.postForEntity(apiUrl + endpoint, entity, String.class);
    } catch (ResourceAccessException ex) {
      logNetworkFailure("POST " + endpoint, ex, startedAt);
      throw ex;
    }
  }

  private ResponseEntity<String> doPut(String endpoint, Object body) throws IOException {
    long startedAt = System.currentTimeMillis();
    try {
      String jsonBody = objectMapper.writeValueAsString(body);
      HttpEntity<String> entity = new HttpEntity<>(jsonBody, createHeaders());
      return restTemplate.exchange(apiUrl + endpoint, HttpMethod.PUT, entity, String.class);
    } catch (ResourceAccessException ex) {
      logNetworkFailure("PUT " + endpoint, ex, startedAt);
      throw ex;
    }
  }

  private ResponseEntity<String> doDelete(String endpoint) throws IOException {
    long startedAt = System.currentTimeMillis();
    try {
      HttpEntity<String> entity = new HttpEntity<>(createHeaders());
      return restTemplate.exchange(apiUrl + endpoint, HttpMethod.DELETE, entity, String.class);
    } catch (ResourceAccessException ex) {
      logNetworkFailure("DELETE " + endpoint, ex, startedAt);
      throw ex;
    }
  }

  /**
   * Extrai o motivo real do erro do corpo da resposta do Sicoob. Sem isso, quem chama {@link
   * #postCancel} / {@link #post} etc só via a mensagem genérica "Erro ao realizar requisição ...
   * ao Sicoob" — escondendo, por exemplo, que o boleto já estava baixado ou que o "nosso número"
   * informado é inválido.
   */
  private String extractSicoobMessage(HttpStatusCodeException ex) {
    String body = ex.getResponseBodyAsString();
    if (body == null || body.isBlank()) {
      return "Detalhe: " + ex.getMessage();
    }
    try {
      JsonNode node = objectMapper.readTree(body);
      JsonNode mensagens = node.get("mensagens");
      if (mensagens != null && mensagens.isArray() && !mensagens.isEmpty()) {
        JsonNode mensagem = mensagens.get(0).get("mensagem");
        if (mensagem != null) {
          return "Detalhe: " + mensagem.asText();
        }
      }
      JsonNode mensagem = node.get("mensagem");
      if (mensagem != null) {
        return "Detalhe: " + mensagem.asText();
      }
    } catch (Exception parseError) {
      // corpo não é JSON válido, cai no fallback abaixo
    }
    return "Detalhe: " + body;
  }

  private void logHttpFailure(String operation, HttpStatusCodeException ex, long startedAt) {
    long elapsedMs = System.currentTimeMillis() - startedAt;
    log.error(
        "[Sicoob][api] operacao={} falha HTTP {} elapsedMs={}",
        operation,
        ex.getStatusCode(),
        elapsedMs);
  }

  private void logNetworkFailure(String operation, ResourceAccessException ex, long startedAt) {
    long elapsedMs = System.currentTimeMillis() - startedAt;
    Throwable rootCause = ex.getCause();
    String causeClass =
        rootCause != null ? rootCause.getClass().getSimpleName() : ex.getClass().getSimpleName();
    log.error(
        "[Sicoob][api] operacao={} falha de rede/TLS - causaRaiz={} elapsedMs={}",
        operation,
        causeClass,
        elapsedMs);
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
