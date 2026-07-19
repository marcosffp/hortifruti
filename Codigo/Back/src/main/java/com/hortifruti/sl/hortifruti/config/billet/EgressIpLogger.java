package com.hortifruti.sl.hortifruti.config.billet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Consulta um serviço de echo externo para descobrir com qual IP de saída a aplicação está
 * saindo para a internet no momento. Usado para diagnosticar se o Sicoob está bloqueando parte
 * das conexões por allowlist de IP (Railway pode variar o IP de saída entre tentativas).
 *
 * <p>Usa um {@link RestTemplate} comum, sem o mTLS do Sicoob — o objetivo aqui é só descobrir o
 * IP de saída, não falar com o Sicoob.
 */
@Slf4j
@Component
public class EgressIpLogger {

  private static final String IP_ECHO_URL = "https://api.ipify.org?format=text";

  private final RestTemplate restTemplate = new RestTemplate();

  /** Retorna o IP de saída atual, ou "desconhecido" se o serviço de echo falhar. */
  public String currentEgressIp() {
    try {
      String ip = restTemplate.getForObject(IP_ECHO_URL, String.class);
      return ip != null ? ip.trim() : "desconhecido";
    } catch (RestClientException ex) {
      log.warn(
          "[EgressIpLogger] falha ao consultar IP de saida via {}: {}",
          IP_ECHO_URL,
          ex.getMessage());
      return "desconhecido";
    }
  }
}
