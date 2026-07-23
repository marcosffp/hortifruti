package com.hortifruti.sl.hortifruti.config.billet;

import com.hortifruti.sl.hortifruti.config.ssl.MtlsConnectionSettings;
import com.hortifruti.sl.hortifruti.config.ssl.MtlsRestTemplateFactory;
import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BilletSSLConfig {

  private final MtlsRestTemplateFactory mtlsRestTemplateFactory;

  @Bean(name = "billetRestTemplate")
  public RestTemplate billetRestTemplate() {
    return mtlsRestTemplateFactory.create(
        MtlsConnectionSettings.builder()
            .maxConnTotal(20)
            .maxConnPerRoute(10)
            .connectionKeepAlive(Timeout.ofMinutes(2))
            .defaultHeaders(
                List.of(
                    new BasicHeader("Accept", "application/json"),
                    new BasicHeader("Content-Type", "application/json"),
                    new BasicHeader("User-Agent", "Hortifruti-SL/1.0")))
            .missingCertificateException(
                () -> new BilletException("Arquivo PFX não encontrado no caminho especificado"))
            .exceptionWrapper(
                (message, cause) ->
                    new BilletException("Erro ao configurar SSL para o Sicoob: " + message, cause))
            .build());
  }
}
