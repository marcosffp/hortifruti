package com.hortifruti.sl.hortifruti.exception;

import org.springframework.http.HttpStatus;

/**
 * Base para exceções de domínio cujo tratamento em {@link GlobalExceptionHandler} é só "status HTTP
 * fixo + título fixo + mensagem própria no corpo" — o que cobria a grande maioria dos ~25 handlers
 * antes duplicados quase palavra-por-palavra ali. Exceções que precisam de lógica própria no
 * handler (extrair campo de validação, decidir por causa/conteúdo, expor campo extra como {@code
 * retryAfter}) continuam com {@code @ExceptionHandler} dedicado — não foram migradas aqui. {@code
 * WeatherApiException} também fica de fora de propósito: é checked ({@code extends Exception}), não
 * {@code RuntimeException}, e essa distinção é intencional (ver {@code
 * exception/climate/README.md}).
 */
public abstract class DomainException extends RuntimeException {

  protected DomainException(String message) {
    super(message);
  }

  protected DomainException(String message, Throwable cause) {
    super(message, cause);
  }

  public abstract HttpStatus getHttpStatus();

  public abstract String getErrorTitle();

  /**
   * {@code true} loga em ERROR com stacktrace completo (falha externa grave, ex.: infra de
   * storage/backup). Por padrão {@code false}.
   */
  public boolean logStackTrace() {
    return false;
  }

  /**
   * {@code true} loga em ERROR (só mensagem, sem stacktrace) em vez de WARN — usado por falhas de
   * integrações externas (BB, Sicoob, Focus NFe/Gemini) que merecem mais atenção que um erro de
   * validação comum, mas cujo detalhe já é resumido na própria mensagem. Ignorado se {@link
   * #logStackTrace()} for {@code true}.
   */
  public boolean isSevere() {
    return false;
  }
}
