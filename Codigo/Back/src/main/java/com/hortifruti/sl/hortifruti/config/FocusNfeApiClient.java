package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class FocusNfeApiClient {

  @Value("${focus.nfe.token}")
  private String focusNfeToken;

  @Value("${focus.nfe.api.url}")
  private String focusNfeApiUrl;

  private final String URL_BASE_POST = "/v2/nfe?ref=";

  private final RestTemplate restTemplate = new RestTemplate();

  public String sendRequest(String ref, String payload) {
    try {
      String url = focusNfeApiUrl + URL_BASE_POST + ref;

      HttpHeaders headers = createHeaders();
      HttpEntity<String> entity = new HttpEntity<>(payload, headers);

      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

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
          restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

      return response.getBody();
    } catch (Exception e) {
      throw new InvoiceException("Erro ao consultar a NFe com referência: " + ref, e);
    }
  }

  /**
   * Lista notas fiscais por CPF/CNPJ do destinatário
   * 
   * Endpoint correto da API Focus NFe para listar NFe's:
   * GET /v2/nfes?cnpj_destinatario=XXXXX
   * 
   * Documentação: https://focusnfe.com.br/doc/
   * 
   * @param cpfCnpj CPF ou CNPJ do destinatário (apenas números)
   * @return JSON com a lista de notas fiscais
   */
  public String listInvoicesByDocument(String cpfCnpj) {
    try {
      // API Focus NFe: GET /v2/nfes?cnpj_destinatario=XXXXX (com 's' no final!)
      // O parâmetro funciona tanto para CPF quanto CNPJ
      String url = focusNfeApiUrl + "/v2/nfes?cnpj_destinatario=" + cpfCnpj;
      
      System.out.println("========================================");
      System.out.println("FocusNfeApiClient - Buscando notas fiscais");
      System.out.println("URL: " + url);
      System.out.println("CPF/CNPJ: " + cpfCnpj);

      HttpHeaders headers = createHeaders();
      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
      
      String responseBody = response.getBody();
      System.out.println("Status: " + response.getStatusCode());
      System.out.println("Response Body: " + (responseBody != null ? responseBody : "NULL"));
      System.out.println("Response Length: " + (responseBody != null ? responseBody.length() : 0));
      System.out.println("========================================");

      return responseBody;
    } catch (Exception e) {
      System.err.println("ERRO ao listar NFe's por CPF/CNPJ: " + cpfCnpj);
      System.err.println("Mensagem: " + e.getMessage());
      e.printStackTrace();
      throw new InvoiceException("Erro ao listar NFe's por CPF/CNPJ: " + cpfCnpj, e);
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
          restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);

      return response.getBody();
    } catch (Exception e) {
      throw new InvoiceException("Erro ao cancelar a NF-e com referência: " + ref, e);
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
