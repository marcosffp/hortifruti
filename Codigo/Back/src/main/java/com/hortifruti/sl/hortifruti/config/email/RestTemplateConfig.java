package com.hortifruti.sl.hortifruti.config.email;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  private static final int CONNECT_TIMEOUT_MS = 30000;
  private static final int READ_TIMEOUT_MS = 30000;

  // Nome do bean deliberadamente diferente de "restTemplate": ha outros beans RestTemplate
  // (bbRestTemplate, billetRestTemplate) com SSLContext/certificado mTLS configurado. Se o
  // @Qualifier de algum consumidor for perdido (ex: lombok.config ausente no build), o
  // Spring cai no fallback de autowiring por nome do parametro/campo — e um bean chamado
  // "restTemplate" bateria por acidente com campos "private final RestTemplate restTemplate"
  // que deveriam receber o bean com mTLS. Ja aconteceu em producao (ver diagnostico-mtls.md).
  @Bean(name = "genericRestTemplate")
  public RestTemplate genericRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    factory.setReadTimeout(READ_TIMEOUT_MS);
    return new RestTemplate(factory);
  }
}
