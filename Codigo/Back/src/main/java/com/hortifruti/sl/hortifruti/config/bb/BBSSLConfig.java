package com.hortifruti.sl.hortifruti.config.bb;

import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.config.ssl.MtlsConnectionSettings;
import com.hortifruti.sl.hortifruti.config.ssl.MtlsRestTemplateFactory;
import com.hortifruti.sl.hortifruti.exception.bb.BBApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * A API Extratos do BB exige mTLS. Reaproveita o mesmo certificado e-CNPJ (DOCUMENT_PFX/
 * PASSWORD_PFX) ja usado para o Sicoob em {@link Base64FileDecoder} — nao criamos headers padrao
 * aqui porque o token (form-urlencoded) e o extrato (json) precisam de Content-Type diferentes por
 * requisicao.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BBSSLConfig {

  private final MtlsRestTemplateFactory mtlsRestTemplateFactory;

  @Bean(name = "bbRestTemplate")
  public RestTemplate bbRestTemplate() {
    return mtlsRestTemplateFactory.create(
        MtlsConnectionSettings.builder()
            .maxConnTotal(10)
            .maxConnPerRoute(5)
            .missingCertificateException(
                () -> new BBApiException("Certificado mTLS do BB (arquivo PFX) não encontrado."))
            .exceptionWrapper(
                (message, cause) ->
                    new BBApiException(
                        "Erro ao configurar SSL (mTLS) para o BB: " + message, cause))
            .build());
  }
}
