package com.hortifruti.sl.hortifruti.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceAlreadyCancelledException;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FocusNfeApiClient {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${focus.nfe.token}")
  private String focusNfeToken;

  @Value("${focus.nfe.api.url}")
  private String focusNfeApiUrl;

  private static final String URL_BASE_POST = "/v2/nfe?ref=";

  @Qualifier("focusNfeRestTemplate")
  private final RestTemplate restTemplate;

  public String sendRequest(String ref, String payload) {
    try {
      String url = focusNfeApiUrl + URL_BASE_POST + ref;

      HttpHeaders headers = createHeaders();
      HttpEntity<String> entity = new HttpEntity<>(payload, headers);

      ResponseEntity<String> response =
          executeWithNetworkRetry(
              "POST " + url,
              () -> restTemplate.exchange(url, HttpMethod.POST, entity, String.class));

      return response.getBody();
    } catch (Exception e) {
      throw new InvoiceException("Erro ao comunicar com a Focus NFe: " + e.getMessage(), e);
    }
  }

  public String sendGetRequest(String ref, int completa) {
    try {
      String url = focusNfeApiUrl + "/v2/nfe/" + ref + "?completa=" + completa;

      HttpHeaders headers = createHeaders();
      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<String> response =
          executeWithNetworkRetry(
              "GET " + url, () -> restTemplate.exchange(url, HttpMethod.GET, entity, String.class));

      return response.getBody();
    } catch (Exception e) {
      throw new InvoiceException("Erro ao consultar a NFe com referência: " + ref, e);
    }
  }

  public String cancelInvoice(String ref, String justificativa) {
    try {
      String url = focusNfeApiUrl + "/v2/nfe/" + ref;

      if (justificativa == null || justificativa.length() < 15 || justificativa.length() > 255) {
        throw new IllegalArgumentException(
            "A justificativa deve conter entre 15 e 255 caracteres.");
      }

      HttpHeaders headers = createHeaders();
      headers.set("Content-Type", "application/json");

      Map<String, String> body = new HashMap<>();
      body.put("justificativa", justificativa);

      HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

      ResponseEntity<String> response =
          executeWithNetworkRetry(
              "DELETE " + url,
              () -> restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class));

      return response.getBody();
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (HttpStatusCodeException e) {
      String mensagem = extractFocusNfeMessage(e);
      if ("already_processed".equals(extractFocusNfeCode(e))) {
        throw new InvoiceAlreadyCancelledException(
            "NF-e com referência " + ref + " já estava cancelada: " + mensagem, e);
      }
      throw new InvoiceException(
          "Erro ao cancelar a NF-e com referência " + ref + ": " + mensagem, e);
    } catch (Exception e) {
      throw new InvoiceException("Erro ao cancelar a NF-e com referência: " + ref, e);
    }
  }

  /**
   * Download de arquivo binário (XML/DANFE) já emitido, pelo path relativo retornado pela Focus NFe
   * na consulta da nota (ex: {@code caminho_xml_nota_fiscal}/{@code caminho_danfe}). Usado por
   * {@code FiscalNoteXmlStorageService}/{@code DanfeXmlService} — não trata exceção aqui, cada
   * chamador decide como reagir a uma falha de download (melhor esforço vs. erro fatal).
   */
  public byte[] downloadFile(String filePath, MediaType mediaType) {
    String url = focusNfeApiUrl + filePath;

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(mediaType, MediaType.ALL));

    ResponseEntity<byte[]> response =
        restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

    return response.getBody();
  }

  /**
   * A Focus NFe retorna o motivo do erro no corpo JSON (ex: {@code {"codigo": "already_processed",
   * "mensagem": "A nota fiscal foi cancelada"}}) — sem extrair esse campo, o operador só via uma
   * mensagem genérica de falha e não conseguia saber, por exemplo, que a NF já havia sido cancelada
   * anteriormente.
   */
  private String extractFocusNfeMessage(HttpStatusCodeException e) {
    String body = e.getResponseBodyAsString();
    if (body == null || body.isBlank()) {
      return e.getMessage();
    }
    try {
      JsonNode node = objectMapper.readTree(body);
      if (node.has("mensagem")) {
        return node.get("mensagem").asText();
      }
    } catch (Exception parseError) {
      // corpo não é JSON válido, cai no fallback abaixo
    }
    return body;
  }

  /**
   * Extrai o campo {@code codigo} do corpo de erro da Focus NFe (ex: {@code already_processed}).
   */
  private String extractFocusNfeCode(HttpStatusCodeException e) {
    String body = e.getResponseBodyAsString();
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(body);
      if (node.has("codigo")) {
        return node.get("codigo").asText();
      }
    } catch (Exception parseError) {
      // corpo não é JSON válido
    }
    return null;
  }

  /**
   * A Focus NFe usa um token estático (Basic Auth), sem OAuth para invalidar/renovar como BB/Sicoob
   * — não faz sentido replicar o retry-em-401 deles aqui. O que falta é o mais básico: uma segunda
   * tentativa em falha de rede transitória ({@link ResourceAccessException}, ex.: timeout, reset de
   * conexão) antes de desistir. Erros HTTP retornados pela própria Focus NFe (4xx/5xx, {@link
   * HttpStatusCodeException}) não são retentados — não são transitórios, e repetir poderia duplicar
   * o efeito de uma emissão de NF-e.
   */
  private <T> ResponseEntity<T> executeWithNetworkRetry(
      String operation, Supplier<ResponseEntity<T>> call) {
    try {
      return call.get();
    } catch (ResourceAccessException ex) {
      log.warn(
          "[FocusNFe] Falha de rede em {}, tentando novamente uma vez: {}",
          operation,
          ex.getMessage());
      return call.get();
    }
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");
    headers.set("Authorization", createBasicAuthHeader());
    return headers;
  }

  private String createBasicAuthHeader() {
    if (focusNfeToken == null || focusNfeToken.trim().isEmpty()) {
      throw new RuntimeException("Token do Focus NFe não configurado");
    }

    String credentials = focusNfeToken.trim() + ":";
    String encodedCredentials =
        Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

    return "Basic " + encodedCredentials;
  }
}
