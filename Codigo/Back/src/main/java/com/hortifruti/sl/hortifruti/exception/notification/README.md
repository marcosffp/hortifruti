# com.hortifruti.sl.hortifruti.exception.notification

Exceção do módulo de notificações (SendGrid/e-mail e Ultramsg/WhatsApp).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `NotificationException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas no envio de notificações em massa (e-mail ou WhatsApp), incluindo erros de integração com SendGrid/Ultramsg. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
