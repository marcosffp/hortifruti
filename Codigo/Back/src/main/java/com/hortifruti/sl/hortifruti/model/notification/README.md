# com.hortifruti.sl.hortifruti.model.notification

Enums usados na configuração de envio de notificações (e-mail via SendGrid e WhatsApp via Ultramsg). Não há entidades JPA nesta pasta.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `NotificationChannel.java` | Enum | Canal de envio configurado: `EMAIL`, `WHATSAPP`, `BOTH`. |
| `NotificationType.java` | Enum | Tipo de notificação a disparar: `EMAIL_ONLY`, `WHATSAPP_ONLY`, `BOTH`. |
| `NotificationRecipient.java` | Enum | Destinatário da notificação: `ACCOUNTING`, `CLIENT`, `MANAGER`. |
