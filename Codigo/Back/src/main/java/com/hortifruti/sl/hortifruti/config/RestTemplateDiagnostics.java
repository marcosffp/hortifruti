package com.hortifruti.sl.hortifruti.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Diagnostico temporario de mTLS (ver diagnostico-mtls.md): confirma se o {@link RestTemplate}
 * usado no momento da chamada real e o mesmo {@code CloseableHttpClient} (mesmo SSLContext/
 * KeyManager customizado) construido nos beans {@code bbRestTemplate}/{@code billetRestTemplate}.
 * Remover depois que a causa da EC2 (chamada real nao passando pelo SSLContext configurado) for
 * confirmada e corrigida.
 */
@Slf4j
public final class RestTemplateDiagnostics {

  private RestTemplateDiagnostics() {}

  public static void logIdentity(String label, RestTemplate restTemplate) {
    ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
    Object httpClient =
        requestFactory instanceof HttpComponentsClientHttpRequestFactory factory
            ? factory.getHttpClient()
            : null;
    log.info(
        "[mTLS:diag] {} restTemplate={} requestFactory={} httpClient={}",
        label,
        System.identityHashCode(restTemplate),
        requestFactory.getClass().getName(),
        httpClient != null ? System.identityHashCode(httpClient) : "n/a (nao e HttpComponentsClientHttpRequestFactory)");
  }
}
