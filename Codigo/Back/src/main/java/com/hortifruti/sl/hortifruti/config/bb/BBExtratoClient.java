package com.hortifruti.sl.hortifruti.config.bb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.exception.BBApiException;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Cliente da API Extratos v2 do BB. Agencia e conta sao configuracao de servidor (nunca vem do
 * chamador) para que este cliente jamais possa ser usado para consultar outra conta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BBExtratoClient {

  @Value("${bb.ambiente}")
  private String ambiente;

  @Value("${bb.app-key}")
  private String appKey;

  @Value("${bb.agencia}")
  private String agencia;

  @Value("${bb.conta}")
  private String conta;

  @Value("${bb.mciteste:}")
  private String mciteste;

  @Qualifier("bbRestTemplate")
  private final RestTemplate restTemplate;

  private final BBToken bbToken;
  private final ObjectMapper objectMapper;

  private static final int MAX_PAGINAS = 40;

  public JsonNode getExtratoPage(int pagina, String dataInicio, String dataFim) {
    try {
      return doGet(pagina, dataInicio, dataFim, bbToken.getAccessToken());
    } catch (HttpClientErrorException.Unauthorized ex) {
      bbToken.invalidateToken();
      return doGet(pagina, dataInicio, dataFim, bbToken.getAccessToken());
    }
  }

  /**
   * Busca todos os lançamentos do período (dataInicio/dataFim no formato DDMMAAAA), percorrendo
   * todas as páginas via {@code numeroPaginaProximo} — diferente de {@link BBSaldoService}, que só
   * consulta o dia de hoje e por isso nunca precisa de mais de uma página na prática.
   */
  public List<JsonNode> getExtratoPeriodo(String dataInicio, String dataFim) {
    List<JsonNode> lancamentos = new ArrayList<>();
    int pagina = 1;
    while (pagina <= MAX_PAGINAS) {
      JsonNode body = getExtratoPage(pagina, dataInicio, dataFim);
      body.path("listaLancamento").forEach(lancamentos::add);

      int proxima = body.path("numeroPaginaProximo").asInt(0);
      if (proxima == 0 || proxima == pagina) {
        break;
      }
      pagina = proxima;
    }
    return lancamentos;
  }

  private JsonNode doGet(int pagina, String dataInicio, String dataFim, String token) {
    String url = buildUrl(pagina, dataInicio, dataFim);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    if ("hml".equalsIgnoreCase(ambiente)) {
      headers.set("x-br-com-bb-ipa-mciteste", mciteste);
    }

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      if (response.getBody() == null) {
        throw new BBApiException("Resposta vazia da API de Extratos do BB.");
      }
      return objectMapper.readTree(response.getBody());
    } catch (HttpClientErrorException.NotFound ex) {
      log.error(
          "Conta nao encontrada ou sem lancamentos na API do BB: {}", ex.getResponseBodyAsString());
      throw new BBApiException("Não foi possível localizar lançamentos para a conta configurada.");
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      log.error(
          "Falha ao consultar extrato do BB ({}): {}",
          ex.getStatusCode(),
          ex.getResponseBodyAsString(),
          ex);
      throw new BBApiException("Falha ao consultar o extrato na API do BB.");
    } catch (BBApiException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Erro inesperado ao consultar extrato do BB.", ex);
      throw new BBApiException("Erro inesperado ao consultar o extrato na API do BB.");
    }
  }

  private String buildUrl(int pagina, String dataInicio, String dataFim) {
    String baseUrl = BBEndpoints.extratoBaseUrl(ambiente);
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
                baseUrl
                    + "/conta-corrente/agencia/"
                    + stripLeadingZeros(agencia)
                    + "/conta/"
                    + stripLeadingZeros(conta))
            .queryParam("gw-dev-app-key", appKey)
            .queryParam("numeroPaginaSolicitacao", pagina)
            .queryParam("quantidadeRegistroPaginaSolicitacao", 120);

    if (dataInicio != null && dataFim != null) {
      builder
          .queryParam("dataInicioSolicitacao", dataInicio)
          .queryParam("dataFimSolicitacao", dataFim);
    }

    return builder.build(true).toUriString();
  }

  static String stripLeadingZeros(String value) {
    String stripped = value.replaceFirst("^0+", "");
    return stripped.isEmpty() ? "0" : stripped;
  }
}
