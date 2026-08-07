# com.hortifruti.sl.hortifruti.service.notification

Envio de notificações (documentos, extratos, avisos) a clientes e à contabilidade, por e-mail
e/ou WhatsApp, com montagem de mensagem a partir de templates. Não implementa os provedores em si
(SendGrid, Gmail, Ultramsg) — delega para os subpacotes `email/` e `whatsapp/`.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BulkNotificationService.java` | `@Service` | Envia um lote de arquivos para múltiplos clientes (ou para a contabilidade) de uma vez, validando canal/telefone/e-mail por destinatário; propaga imediatamente erro de autorização do Gmail (afeta todos os envios igualmente) em vez de tentar cliente a cliente; retorna resumo de sucesso/falha por destinatário. |
| `NotificationCoordinator.java` | `@Service` | Ponto único de envio: decide se dispara e-mail, WhatsApp ou ambos conforme `NotificationChannel`, monta a mensagem de WhatsApp por tipo (`MONTHLY_STATEMENTS`, `GENERIC_FILES`, `CLIENT_DOCUMENTS`, `GENERIC`) via `WhatsAppMessageBuilder`, e consolida o status (OK/FALHA/N-A) de cada canal na resposta. |
| `NotificationService.java` | `@Service` | Casos de uso pontuais: envio de arquivos genéricos para a contabilidade (com resumo financeiro cartão/dinheiro) e envio de documentos para um cliente específico, ambos montando o corpo do e-mail via `EmailTemplateService`. |

`StatementSelectionService.java` (seleção do melhor extrato bancário por período) foi movida para `service.finance.transaction` — é lógica de extrato/transação, não de notificação; não pertencia a este pacote.
