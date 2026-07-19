package com.hortifruti.sl.hortifruti.config.billet;

import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.config.LoggingX509KeyManager;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Arrays;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Diagnostico de mTLS em producao (Railway): se os logs de {@link LoggingX509KeyManager} e do
 * catch de {@code ResourceAccessException} em {@code SicoobToken}/{@code BilletHttpClient} nao
 * forem suficientes para identificar a causa de uma falha de handshake, dá pra ligar o log
 * nativo de SSL da JVM setando, so no servico do Railway e so durante a janela de diagnostico:
 *
 * <pre>{@code
 * JAVA_TOOL_OPTIONS=-Djavax.net.debug=ssl:handshake
 * }</pre>
 *
 * É muito verboso (loga bytes do handshake) — REMOVER essa variável assim que o diagnóstico
 * terminar, não deixar ligada em produção.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BilletSSLConfig {

  private final Base64FileDecoder base64FileDecoder;

  @Value("${password.pfx}")
  private String pfxPassword;

  @Bean(name = "billetRestTemplate")
  public RestTemplate billetRestTemplate() {
    try {
      base64FileDecoder.decodePfx();
      File pfxFile = base64FileDecoder.getPfxFile();
      if (!pfxFile.exists() || !pfxFile.isFile()) {
        throw new BilletException("Arquivo PFX não encontrado no caminho especificado");
      }

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (FileInputStream instream = new FileInputStream(pfxFile)) {
        keyStore.load(instream, pfxPassword.toCharArray());
      }

      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, pfxPassword.toCharArray());

      KeyManager[] keyManagers = LoggingX509KeyManager.wrap(keyManagerFactory.getKeyManagers(), "Sicoob");

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
              .setMaxConnTotal(20)
              .setMaxConnPerRoute(10)
              .build();

      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectionRequestTimeout(Timeout.ofSeconds(30))
              .setResponseTimeout(Timeout.ofSeconds(30))
              .setConnectionKeepAlive(Timeout.ofMinutes(2))
              .build();

      CloseableHttpClient httpClient =
          HttpClients.custom()
              .setConnectionManager(connectionManager)
              .setDefaultHeaders(
                  Arrays.asList(
                      new BasicHeader("Accept", "application/json"),
                      new BasicHeader("Content-Type", "application/json"),
                      new BasicHeader("User-Agent", "Hortifruti-SL/1.0")))
              .setDefaultRequestConfig(requestConfig)
              .build();

      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
      factory.setHttpClient(httpClient);

      return new RestTemplate(factory);

    } catch (BilletException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new BilletException("Erro ao configurar SSL para o Sicoob: " + e.getMessage(), e);
    }
  }
}
