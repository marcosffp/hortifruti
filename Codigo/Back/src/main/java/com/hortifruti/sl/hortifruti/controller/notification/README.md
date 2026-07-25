# com.hortifruti.sl.hortifruti.controller.notification

Endpoints de envio de notificações e documentos por email/WhatsApp (contabilidade, clientes individuais e em massa), além de endpoints de teste/disparo manual relacionados a alertas de armazenamento e boletos vencidos.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `NotificationController.java` | `@RestController` (`/api/notifications`) | `POST /api/notifications/accounting/generic-files` envia arquivos genéricos (opcionais) e valores de cartão/dinheiro para a contabilidade, somente por email; `POST /api/notifications/client/documents` envia documentos a um cliente específico por email e/ou WhatsApp (`channel`: EMAIL/WHATSAPP/BOTH); `POST /api/notifications/test/database-storage-alert` dispara email de teste com o tamanho atual do banco; `POST /api/notifications/overdue/check` força verificação manual de `CombinedScore`s vencidos e notificação; `POST /api/notifications/send-bulk` envia múltiplos arquivos para múltiplos clientes por múltiplos canais. |
