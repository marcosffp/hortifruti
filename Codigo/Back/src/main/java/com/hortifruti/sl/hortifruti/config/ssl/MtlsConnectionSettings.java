package com.hortifruti.sl.hortifruti.config.ssl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.Builder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.util.Timeout;

/**
 * Parâmetros que variam entre os clientes mTLS (BB, Sicoob/Billet), consumidos por {@link
 * MtlsRestTemplateFactory} para montar o {@link org.springframework.web.client.RestTemplate}.
 */
@Builder
public class MtlsConnectionSettings {

  private final int maxConnTotal;
  private final int maxConnPerRoute;
  private final Timeout connectionKeepAlive;

  @Builder.Default private final List<Header> defaultHeaders = List.of();

  private final Supplier<RuntimeException> missingCertificateException;
  private final BiFunction<String, Throwable, RuntimeException> exceptionWrapper;

  public int getMaxConnTotal() {
    return maxConnTotal;
  }

  public int getMaxConnPerRoute() {
    return maxConnPerRoute;
  }

  public Timeout getConnectionKeepAlive() {
    return connectionKeepAlive;
  }

  public List<Header> getDefaultHeaders() {
    return defaultHeaders;
  }

  public RuntimeException missingCertificateException() {
    return missingCertificateException.get();
  }

  public RuntimeException wrapException(String message, Throwable cause) {
    return exceptionWrapper.apply(message, cause);
  }
}
