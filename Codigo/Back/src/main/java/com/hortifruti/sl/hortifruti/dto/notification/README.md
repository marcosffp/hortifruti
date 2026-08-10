# com.hortifruti.sl.hortifruti.dto.notification

DTOs do módulo de notificações, que integra com SendGrid (e-mail) e Ultramsg/WhatsApp para enviar
boletos, notas fiscais, extratos mensais e comunicados contábeis a clientes, individualmente ou em
massa.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BulkNotificationResponse.java` | record | Resposta de envio em massa: sucesso, mensagem, total enviado/falho e lista de destinatários que falharam. Expõe factories `success`, `failure` e `partial` para montar a resposta conforme o resultado. |
| `ClientDocumentsRequest.java` | record | Request para envio de documentos a um cliente específico: id do cliente, canal (`NotificationChannel`) e mensagem customizada. |
| `GenericFileRequest.java` | record | Arquivo genérico anexado a uma notificação: nome, tipo e conteúdo binário (`byte[]`). |
| `GenericFilesAccountingRequest.java` | record | Request de envio de arquivos contábeis com valores de cartão e dinheiro em caixa e mensagem customizada. |
| `NotificationResponse.java` | record | Resposta padrão de envio de notificação: sucesso, mensagem e status de envio por canal (e-mail/WhatsApp). Construtor auxiliar preenche os status como "N/A" e há factory `withStatuses`. |
