# src/main/resources/templates/email

Templates HTML de e-mails transacionais, processados por `EmailTemplateService` (substituição de placeholders `{{PLACEHOLDER}}` pelo conteúdo real) e enviados via SendGrid ou Gmail (SMTP/API), conforme `email.provider` em `application.properties`.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `client-documents-clean.html` | Template HTML de e-mail | E-mail de envio de documentos de clientes (boletos/notas) em um período. Placeholders: `{{GREETING}}`, `{{PERIOD_RANGE}}`, `{{CUSTOM_MESSAGE}}`, `{{SENDER_NAME}}`, `{{CURRENT_DATE}}`. |
| `database-management.html` | Template HTML de e-mail | Alerta de armazenamento do banco de dados atingindo um limite (usado pelo endpoint `/scheduler/check-database-storage`). Placeholder: `{{STORAGE_PERCENTAGE}}`. |
| `generic-files-clean.html` | Template HTML de e-mail | E-mail de envio de arquivos genéricos (ex.: relatório de caixa/cartão), com contagem de arquivos e valores. Placeholders: `{{FILES_COUNT}}`, `{{CASH_VALUE}}`, `{{CARD_VALUE}}`, `{{CUSTOM_MESSAGE}}`. |
| `login-lockout-alert.html` | Template HTML de e-mail | Alerta de segurança enviado quando `LoginProtectionService` ativa um lockout de conta ou IP. Placeholders: `{{IDENTIFIER_TYPE}}`, `{{IDENTIFIER}}`, `{{USERNAME}}`, `{{IP}}`, `{{USER_AGENT}}`, `{{TIMESTAMP}}`, `{{ATTEMPT_COUNT}}`, `{{LOCKOUT_LEVEL}}`, `{{LOCKOUT_DURATION}}`. |
| `overdue-management-clean.html` | Template HTML de e-mail | Relatório de boletos/cobranças em atraso, com lista de clientes inadimplentes. Placeholders: `{{REPORT_DATE}}`, `{{TOTAL_CLIENTS}}`, `{{TOTAL_OVERDUE_BOLETOS}}`, `{{TOTAL_OVERDUE_AMOUNT}}`, `{{CLIENT_ROWS}}` (linhas de tabela geradas dinamicamente). |
