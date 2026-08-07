package com.hortifruti.sl.hortifruti.config.ssl;

import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.RequestConfig.Builder;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Base compartilhada para os clientes HTTP mTLS do BB e do Sicoob/Billet: ambos reaproveitam o
 * mesmo certificado e-CNPJ (DOCUMENT_PFX/PASSWORD_PFX, ver {@link Base64FileDecoder}), a mesma
 * config TLS 1.2/1.3 e o mesmo padrão de timeout — só o pool de conexões, os headers padrão e o
 * tipo de exceção lançada variam por integração.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MtlsRestTemplateFactory {

  private final Base64FileDecoder base64FileDecoder;

  @Value("${password.pfx}")
  private String pfxPassword;

  public RestTemplate create(MtlsConnectionSettings settings) {
    try {
      base64FileDecoder.decodePfx();
      File pfxFile = base64FileDecoder.getPfxFile();
      if (pfxFile == null || !pfxFile.isFile()) {
        throw settings.missingCertificateException();
      }

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (FileInputStream instream = new FileInputStream(pfxFile)) {
        keyStore.load(instream, pfxPassword.toCharArray());
      }

      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, pfxPassword.toCharArray());

      KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, null, null);

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
              .setMaxConnTotal(settings.getMaxConnTotal())
              .setMaxConnPerRoute(settings.getMaxConnPerRoute())
              .build();

      Builder requestConfigBuilder =
          RequestConfig.custom()
              .setConnectionRequestTimeout(Timeout.ofSeconds(30))
              .setResponseTimeout(Timeout.ofSeconds(30));
      if (settings.getConnectionKeepAlive() != null) {
        requestConfigBuilder.setConnectionKeepAlive(settings.getConnectionKeepAlive());
      }

      CloseableHttpClient httpClient =
          HttpClients.custom()
              .setConnectionManager(connectionManager)
              .setDefaultHeaders(settings.getDefaultHeaders())
              .setDefaultRequestConfig(requestConfigBuilder.build())
              .build();

      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
      factory.setHttpClient(httpClient);

      return new RestTemplate(factory);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      // O detalhe original (caminho de arquivo, mensagem de biblioteca TLS) fica só no log do
      // servidor — embuti-lo na mensagem da exceção de negócio a exporia crua ao cliente da API
      // via GlobalExceptionHandler, que devolve ex.getMessage() diretamente.
      log.error("Falha ao configurar conexão mTLS: {}", e.getMessage(), e);
      throw settings.wrapException(
          "Falha ao configurar a conexão segura (mTLS). Consulte os logs do servidor para detalhes.",
          e);
    }
  }
}
