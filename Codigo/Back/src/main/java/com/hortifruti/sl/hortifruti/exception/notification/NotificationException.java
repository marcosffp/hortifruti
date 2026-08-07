package com.hortifruti.sl.hortifruti.exception.notification;

import com.hortifruti.sl.hortifruti.exception.DomainException;
import org.springframework.http.HttpStatus;

public class NotificationException extends DomainException {
  public NotificationException(String message) {
    super(message);
  }

  public NotificationException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getErrorTitle() {
    return "Erro de Notificação em Massa";
  }
}
