# com.hortifruti.sl.hortifruti.dto.notification

DTOs do módulo de notificações, que integra com SendGrid (e-mail) e Ultramsg/WhatsApp para enviar
boletos, notas fiscais, extratos mensais e comunicados contábeis a clientes, individualmente ou em
massa.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `AccountingNotificationRequest.java` | record | Request de notificação contábil: tipo de notificação (`NotificationType`), mês/ano, mensagem customizada, arquivos adicionais (`GenericFileRequest`) e valores de débito/crédito/caixa a informar. |
| `BulkNotificationRequest.java` | record | Request de envio de notificação em massa: lista de ids de clientes, canais de envio (`NotificationChannel`), tipo de destino, mensagem customizada, vencimento e valor do boleto. |
| `BulkNotificationResponse.java` | record | Resposta de envio em massa: sucesso, mensagem, total enviado/falho e lista de destinatários que falharam. Expõe factories `success`, `failure` e `partial` para montar a resposta conforme o resultado. |
| `ClientDocumentsRequest.java` | record | Request para envio de documentos a um cliente específico: id do cliente, canal (`NotificationChannel`) e mensagem customizada. |
| `ClientNotificationRequest.java` | record | Request de notificação a um cliente com arquivos anexos: id do cliente, tipo de notificação, lista de arquivos (`GenericFileRequest`), mensagem customizada e flags para incluir boleto/nota fiscal. |
| `GenericFileRequest.java` | record | Arquivo genérico anexado a uma notificação: nome, tipo e conteúdo binário (`byte[]`). |
| `GenericFilesAccountingRequest.java` | record | Request de envio de arquivos contábeis com valores de cartão e dinheiro em caixa e mensagem customizada. |
| `MonthlyStatementsRequest.java` | record | Request de envio de extratos mensais: mês, ano, canal de notificação e mensagem customizada. |
| `NotificationResponse.java` | record | Resposta padrão de envio de notificação: sucesso, mensagem e status de envio por canal (e-mail/WhatsApp). Construtor auxiliar preenche os status como "N/A" e há factory `withStatuses`. |
