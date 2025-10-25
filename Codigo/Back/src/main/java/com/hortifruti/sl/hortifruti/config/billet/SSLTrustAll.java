package com.hortifruti.sl.hortifruti.config.billet;

import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;
import com.hortifruti.sl.hortifruti.exception.BilletException;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SSLTrustAll {

  private final Base64FileDecoder base64FileDecoder;

  @Value("${password.pfx}")
  private String pfxPassword;

  @Value("${sicoob.domain}")
  private String sicoobDomain;

  @PostConstruct
  public void configureSSL() {
    try {
      System.out.println("[DEBUG] Configurando SSL para o domínio " + sicoobDomain);

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      base64FileDecoder.decodePfx(); 
      File pfxFile = base64FileDecoder.getPfxFile();
      if (pfxFile.exists()) {
        try (FileInputStream instream = new FileInputStream(pfxFile)) {
          keyStore.load(instream, pfxPassword.toCharArray());
        }
      } else {
        System.err.println("[WARNING] Arquivo PFX não encontrado");
        return;
      }

      KeyManagerFactory kmf =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keyStore, pfxPassword.toCharArray());

      TrustManagerFactory tmf =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init((KeyStore) null); 

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(
          kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

      HostnameVerifier hostnameVerifier =
          new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
              if (hostname.endsWith(sicoobDomain)) {
                return true;
              }
              return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session);
            }
          };

      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

    } catch (Exception e) {
      throw new BilletException("Erro ao configurar SSL: ", e);
    }
  }
}
