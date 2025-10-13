package com.hortifruti.sl.hortifruti.config.billet;

import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.BilletException;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLSession;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import java.security.cert.X509Certificate;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class BilletSSLConfig {

 private final Base64FileDecoder base64FileDecoder;

  @Value("${password.pfx}")
  private String pfxPassword;

  @Bean(name = "billetRestTemplate")
  public RestTemplate billetRestTemplate() {
    try {
      base64FileDecoder.decodePfx(); // Garante que o arquivo PFX está decodificado
      File pfxFile = base64FileDecoder.getPfxFile();
      if (!pfxFile.exists() || !pfxFile.isFile()) {
        throw new BilletException("Arquivo PFX não encontrado no caminho especificado");
      }
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (FileInputStream instream = new FileInputStream(pfxFile)) {
        keyStore.load(instream, pfxPassword.toCharArray());
        System.out.println("[DEBUG] Certificado PFX carregado com sucesso!");
      }

      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init((KeyStore)null);
      
      SSLContext sslContext =
          SSLContexts.custom()
            .loadKeyMaterial(keyStore, pfxPassword.toCharArray())
            .loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) {
                    try {
                        String subjectName = chain[0].getSubjectX500Principal().getName();
                        return subjectName.contains("sicoob.com.br");
                    } catch (Exception e) {
                        return false;
                    }
                }
            })
            .build();

      SSLConnectionSocketFactory socketFactory = 
          new SSLConnectionSocketFactory(
              sslContext,
              new String[]{"TLSv1.2"}, 
              null, 
              new HostnameVerifier() {
                  @Override
                  public boolean verify(String hostname, SSLSession session) {
                      return hostname.endsWith("sicoob.com.br");
                  }
              });
      PoolingHttpClientConnectionManager connectionManager =
          PoolingHttpClientConnectionManagerBuilder.create()
              .setSSLSocketFactory(socketFactory)
              .build();

      CloseableHttpClient httpClient =
          HttpClients.custom()
              .setConnectionManager(connectionManager)
              .setDefaultHeaders(Arrays.asList(
                  new BasicHeader("Accept", "application/json")
              ))
              .build();

      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
      factory.setHttpClient(httpClient);
      factory.setConnectTimeout(30000); 
      factory.setConnectionRequestTimeout(30000); 
      factory.setReadTimeout(30000); 

      return new RestTemplate(factory);

    } catch (BilletException e) {
      throw e; 
    } catch (Exception e) {
      e.printStackTrace();
      throw new BilletException("Erro ao configurar SSL para o Sicoob: " + e.getMessage(), e);
    }
  }
}
