package com.hortifruti.sl.hortifruti.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
public class SicoobSSLConfig {

  @Value("${sicoob.pfx.path}")
  private String pfxPath;

  @Value("${sicoob.pfx.password}")
  private String pfxPassword;

  @Bean(name = "sicoobRestTemplate")
  public RestTemplate sicoobRestTemplate() {
    try {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (FileInputStream instream = new FileInputStream(new File(pfxPath))) {
        keyStore.load(instream, pfxPassword.toCharArray());
      }

      SSLContext sslContext = SSLContexts.custom()
          .loadKeyMaterial(keyStore, pfxPassword.toCharArray())
          .build();

      SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

      PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
          .setSSLSocketFactory(socketFactory)
          .build();

      CloseableHttpClient httpClient = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .build();

      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
      factory.setHttpClient(httpClient);
      factory.setConnectTimeout(30000);
      factory.setConnectionRequestTimeout(30000);

      return new RestTemplate(factory);

    } catch (Exception e) {
      throw new RuntimeException("Erro ao configurar SSL para o Sicoob: " + e.getMessage(), e);
    }
  }
}