package com.hortifruti.sl.hortifruti.config;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Envolve o X509KeyManager real para logar, a cada handshake mTLS, quais issuers o servidor pediu
 * e qual alias (se algum) foi escolhido. Usado para diagnosticar por que o certificado de cliente
 * as vezes nao e enviado (alias == null) em runtimes especificos — ver BBSSLConfig/BilletSSLConfig.
 */
@Slf4j
public class LoggingX509KeyManager extends X509ExtendedKeyManager {

  private final X509ExtendedKeyManager delegate;
  private final String label;

  public LoggingX509KeyManager(X509ExtendedKeyManager delegate, String label) {
    this.delegate = delegate;
    this.label = label;
  }

  /** Envolve cada X509ExtendedKeyManager da lista com logging; demais tipos passam intactos. */
  public static KeyManager[] wrap(KeyManager[] keyManagers, String label) {
    KeyManager[] wrapped = new KeyManager[keyManagers.length];
    for (int i = 0; i < keyManagers.length; i++) {
      wrapped[i] =
          keyManagers[i] instanceof X509ExtendedKeyManager x509
              ? new LoggingX509KeyManager(x509, label)
              : keyManagers[i];
    }
    return wrapped;
  }

  @Override
  public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
    String alias = delegate.chooseClientAlias(keyTypes, issuers, socket);
    String peer = socket != null && socket.getInetAddress() != null
        ? socket.getInetAddress() + ":" + socket.getPort()
        : "n/a";
    logDecision(keyTypes, issuers, alias, peer);
    return alias;
  }

  @Override
  public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
    String alias = delegate.chooseEngineClientAlias(keyTypes, issuers, engine);
    String peer = engine != null ? engine.getPeerHost() + ":" + engine.getPeerPort() : "n/a";
    logDecision(keyTypes, issuers, alias, peer);
    return alias;
  }

  private void logDecision(String[] keyTypes, Principal[] issuers, String alias, String peer) {
    String issuerNames =
        issuers == null || issuers.length == 0
            ? "(nenhum solicitado pelo servidor)"
            : Arrays.stream(issuers).map(Principal::getName).reduce((a, b) -> a + " | " + b).orElse("");
    if (alias == null) {
      log.error(
          "[mTLS:{}] peer={} keyTypes={} issuersSolicitados=[{}] -> NENHUM alias selecionado; "
              + "o certificado de cliente NAO sera enviado nesta conexao.",
          label,
          peer,
          Arrays.toString(keyTypes),
          issuerNames);
    } else {
      X509Certificate[] chain = delegate.getCertificateChain(alias);
      String subject = chain != null && chain.length > 0 ? chain[0].getSubjectX500Principal().getName() : "?";
      int chainLength = chain != null ? chain.length : 0;
      log.info(
          "[mTLS:{}] peer={} alias escolhido='{}' chainLength={} subject='{}' issuersSolicitados=[{}]",
          label,
          peer,
          alias,
          chainLength,
          subject,
          issuerNames);
    }
  }

  @Override
  public String[] getClientAliases(String keyType, Principal[] issuers) {
    return delegate.getClientAliases(keyType, issuers);
  }

  @Override
  public String[] getServerAliases(String keyType, Principal[] issuers) {
    return delegate.getServerAliases(keyType, issuers);
  }

  @Override
  public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
    return delegate.chooseServerAlias(keyType, issuers, socket);
  }

  @Override
  public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
    return delegate.chooseEngineServerAlias(keyType, issuers, engine);
  }

  @Override
  public X509Certificate[] getCertificateChain(String alias) {
    return delegate.getCertificateChain(alias);
  }

  @Override
  public PrivateKey getPrivateKey(String alias) {
    return delegate.getPrivateKey(alias);
  }
}
