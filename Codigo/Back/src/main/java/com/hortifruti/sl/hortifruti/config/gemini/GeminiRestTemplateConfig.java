package com.hortifruti.sl.hortifruti.config.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Bean próprio (em vez do {@code genericRestTemplate} compartilhado) porque a extração de nota
 * precisa de um timeout configurável via {@code gemini.timeout.ms} — a chamada envia uma imagem e
 * pode legitimamente demorar mais que as integrações que só trocam JSON pequeno.
 */
@Configuration
public class GeminiRestTemplateConfig {

  @Bean(name = "geminiRestTemplate")
  public RestTemplate geminiRestTemplate(@Value("${gemini.timeout.ms}") int timeoutMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(timeoutMs);
    factory.setReadTimeout(timeoutMs);
    return new RestTemplate(factory);
  }
}
