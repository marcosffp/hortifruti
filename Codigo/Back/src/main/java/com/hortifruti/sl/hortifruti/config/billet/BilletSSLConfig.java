package com.hortifruti.sl.hortifruti.config.billet;

import com.hortifruti.sl.hortifruti.exception.BilletException;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
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

@Configuration
public class BilletSSLConfig {

  @Value("${sicoob.pfx.path}")
  private String pfxPath;

  @Value("${sicoob.pfx.password}")
  private String pfxPassword;

  @Bean(name = "billetRestTemplate")
  public RestTemplate billetRestTemplate() {
    try {
      // Valida o caminho do arquivo PFX
      File pfxFile = new File(pfxPath);
      if (!pfxFile.exists() || !pfxFile.isFile()) {
        throw new BilletException("Arquivo PFX não encontrado no caminho especificado: " + pfxPath);
      }

      // Carrega o KeyStore do arquivo PFX
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (FileInputStream instream = new FileInputStream(pfxFile)) {
        keyStore.load(instream, pfxPassword.toCharArray());
      }

      // Configura o SSLContext com o KeyStore
      SSLContext sslContext =
          SSLContexts.custom().loadKeyMaterial(keyStore, pfxPassword.toCharArray()).build();

      // Configura o socket SSL
      SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

      // Configura o gerenciador de conexões com suporte a SSL
      PoolingHttpClientConnectionManager connectionManager =
          PoolingHttpClientConnectionManagerBuilder.create()
              .setSSLSocketFactory(socketFactory)
              .build();

      // Cria o cliente HTTP com o gerenciador de conexões
      CloseableHttpClient httpClient =
          HttpClients.custom().setConnectionManager(connectionManager).build();

      // Configura o RestTemplate com o cliente HTTP
      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
      factory.setHttpClient(httpClient);
      factory.setConnectTimeout(30000); // Timeout de conexão
      factory.setConnectionRequestTimeout(30000); // Timeout de requisição

      return new RestTemplate(factory);

    } catch (BilletException e) {
      throw e; // Relança exceções específicas do Sicoob
    } catch (Exception e) {
      throw new BilletException("Erro ao configurar SSL para o Sicoob: " + e.getMessage(), e);
    }
  }
}
