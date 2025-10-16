package com.hortifruti.sl.hortifruti.config.billet;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hortifruti.sl.hortifruti.config.Base64FileDecoder;

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
            
            // Carrega o keystore com o certificado PFX
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            base64FileDecoder.decodePfx(); // Garante que o arquivo PFX está decodificado
            File pfxFile = base64FileDecoder.getPfxFile();
            if (pfxFile.exists()) {
                try (FileInputStream instream = new FileInputStream(pfxFile)) {
                    keyStore.load(instream, pfxPassword.toCharArray());
                    System.out.println("[DEBUG] Certificado PFX carregado com sucesso");
                }
            } else {
                System.err.println("[WARNING] Arquivo PFX não encontrado");
                return;
            }

            // Configura o gerenciador de chaves
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, pfxPassword.toCharArray());
            
            // Configura o gerenciador de confiança padrão para validar servidores
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore)null); // Usa o keystore padrão do sistema
            
            // Cria um SSLContext personalizado
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());
            
            // Configura um hostname verifier que só desativa verificação para o domínio Sicoob
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // Se for o domínio Sicoob, aceita sem verificar
                    if (hostname.endsWith(sicoobDomain)) {
                        return true;
                    }
                    // Para outros domínios, usa o verificador padrão
                    return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session);
                }
            };
            
            // Configura uma fábrica de sockets SSL personalizada
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

        } catch (Exception e) {
            System.err.println("Erro ao configurar SSL: " + e.getMessage());
        }
    }
}