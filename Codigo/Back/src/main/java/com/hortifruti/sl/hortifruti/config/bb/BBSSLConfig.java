package com.hortifruti.sl.hortifruti.config.bb;

import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.BBApiException;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * A API Extratos do BB exige mTLS. Reaproveita o mesmo certificado e-CNPJ (DOCUMENT_PFX/
 * PASSWORD_PFX) ja usado para o Sicoob em {@link Base64FileDecoder} — nao criamos headers padrao
 * aqui porque o token (form-urlencoded) e o extrato (json) precisam de Content-Type diferentes por
 * requisicao.
 */
@Configuration
@RequiredArgsConstructor
public class BBSSLConfig {

  private final Base64FileDecoder base64FileDecoder;

  @Value("${password.pfx}")
  private String pfxPassword;

  @Bean(name = "bbRestTemplate")
  public RestTemplate bbRestTemplate() {
    try {
      base64FileDecoder.decodePfx();
      File pfxFile = base64FileDecoder.getPfxFile();
      if (pfxFile == null || !pfxFile.isFile()) {
        throw new BBApiException("Certificado mTLS do BB (arquivo PFX) não encontrado.");
      }

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (FileInputStream instream = new FileInputStream(pfxFile)) {
        keyStore.load(instream, pfxPassword.toCharArray());
      }

      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, pfxPassword.toCharArray());

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

      TlsSocketStrategy tlsSocketStrategy =
          ClientTlsStrategyBuilder.create()
              .setSslContext(sslContext)
              .setTlsVersions(TLS.V_1_2, TLS.V_1_3)
              .buildClassic();

      TlsConfig tlsConfig = TlsConfig.custom().setHandshakeTimeout(Timeout.ofSeconds(30)).build();

      PoolingHttpClientConnectionManager connectionManager =
          PoolingHttpClientConnectionManagerBuilder.create()
              .setTlsSocketStrategy(tlsSocketStrategy)
              .setDefaultTlsConfig(tlsConfig)
              .setMaxConnTotal(10)
              .setMaxConnPerRoute(5)
              .build();

      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectionRequestTimeout(Timeout.ofSeconds(30))
              .setResponseTimeout(Timeout.ofSeconds(30))
              .build();

      CloseableHttpClient httpClient =
          HttpClients.custom()
              .setConnectionManager(connectionManager)
              .setDefaultRequestConfig(requestConfig)
              .build();

      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
      factory.setHttpClient(httpClient);

      return new RestTemplate(factory);

    } catch (BBApiException e) {
      throw e;
    } catch (Exception e) {
      throw new BBApiException("Erro ao configurar SSL (mTLS) para o BB: " + e.getMessage(), e);
    }
  }
}
