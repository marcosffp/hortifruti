package com.hortifruti.sl.hortifruti.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP único para toda a integração com a Focus NFe: {@link FocusNfeApiClient} (emissão,
 * consulta e cancelamento de NF-e) e o download de XML/DANFE em {@code
 * FiscalNoteXmlStorageService}/{@code DanfeXmlService}. Antes, cada um desses pontos usava um
 * cliente e timeout próprios (RestTemplate compartilhado com domínios não relacionados via {@code
 * genericRestTemplate}, WebClient com 60s de timeout, WebClient com 100s de timeout) — qualquer
 * ajuste precisava ser replicado em 3 lugares, e já haviam divergido sem motivo.
 *
 * <p>O timeout de leitura usa o maior dos valores anteriores (download de DANFE em PDF, o mais
 * lento dos três), aplicado uniformemente: folga extra numa chamada JSON rápida não tem custo real.
 */
@Configuration
public class FocusNfeRestTemplateConfig {

  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int READ_TIMEOUT_MS = 100_000;

  @Bean(name = "focusNfeRestTemplate")
  public RestTemplate focusNfeRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    factory.setReadTimeout(READ_TIMEOUT_MS);
    return new RestTemplate(factory);
  }
}
