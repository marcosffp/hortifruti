package com.hortifruti.sl.hortifruti.config.sicoob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.billet.SicoobToken;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoEnvelope;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoResponse;
import com.hortifruti.sl.hortifruti.exception.SicoobExtratoException;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Cliente da API Conta Corrente v4 do Sicoob (extrato por mês/ano, ver documentacao_sicoob_api.md).
 * Reaproveita o token e o RestTemplate mTLS já usados para boleto ({@link SicoobToken}, bean
 * "billetRestTemplate"), pois é o mesmo certificado e-CNPJ. Diferente das chamadas de boleto, esta
 * API exige o header {@code client_id} na própria requisição (não só na obtenção do token).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SicoobExtratoClient {

  private static final String BASE_PATH = "/conta-corrente/v4";

  @Value("${sicoob.api.url}")
  private String apiUrl;

  @Value("${sicoob.client.id}")
  private String clientId;

  @Qualifier("billetRestTemplate")
  private final RestTemplate restTemplate;

  private final SicoobToken sicoobToken;

  private final ObjectMapper objectMapper;

  public SicoobExtratoResponse getExtrato(
      int mes, int ano, long numeroContaCorrente, Integer diaInicial, Integer diaFinal) {
    try {
      return doGet(
          mes, ano, numeroContaCorrente, diaInicial, diaFinal, sicoobToken.getAccessToken());
    } catch (HttpClientErrorException.Unauthorized ex) {
      sicoobToken.invalidateToken();
      return doGet(
          mes, ano, numeroContaCorrente, diaInicial, diaFinal, sicoobToken.getAccessToken());
    }
  }

  private SicoobExtratoResponse doGet(
      int mes,
      int ano,
      long numeroContaCorrente,
      Integer diaInicial,
      Integer diaFinal,
      String token) {
    String url = buildUrl(mes, ano, numeroContaCorrente, diaInicial, diaFinal);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.set("client_id", clientId);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      if (response.getBody() == null) {
        throw new SicoobExtratoException("Resposta vazia da API de Extrato do Sicoob.");
      }
      SicoobExtratoEnvelope envelope =
          objectMapper.readValue(response.getBody(), SicoobExtratoEnvelope.class);
      SicoobExtratoResponse extrato = envelope.resultado();
      if (extrato == null) {
        throw new SicoobExtratoException(
            "Resposta da API de Extrato do Sicoob sem campo 'resultado': " + response.getBody());
      }
      return extrato;
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      log.error(
          "[Sicoob][extrato] falha ao consultar extrato ({}): {}",
          ex.getStatusCode(),
          ex.getResponseBodyAsString(),
          ex);
      throw new SicoobExtratoException("Falha ao consultar o extrato na API do Sicoob.", ex);
    } catch (ResourceAccessException ex) {
      log.error("[Sicoob][extrato] falha de rede/TLS ao consultar extrato.", ex);
      throw new SicoobExtratoException("Erro de rede/TLS ao consultar o extrato do Sicoob.", ex);
    } catch (SicoobExtratoException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("[Sicoob][extrato] erro inesperado ao consultar extrato.", ex);
      throw new SicoobExtratoException("Erro inesperado ao consultar o extrato do Sicoob.", ex);
    }
  }

  private String buildUrl(
      int mes, int ano, long numeroContaCorrente, Integer diaInicial, Integer diaFinal) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(apiUrl + BASE_PATH + "/extrato/" + mes + "/" + ano)
            .queryParam("numeroContaCorrente", numeroContaCorrente);

    if (diaInicial != null) {
      builder.queryParam("diaInicial", diaInicial);
    }
    if (diaFinal != null) {
      builder.queryParam("diaFinal", diaFinal);
    }

    return builder.build(true).toUriString();
  }
}
