# com.hortifruti.sl.hortifruti.service.notification.email

Envio de e-mail com suporte a múltiplos provedores plugáveis (SendGrid, Gmail SMTP, Gmail API),
templates HTML com variáveis e utilitário de saudação/data usados nos corpos de e-mail.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `EmailGreetingUtil.java` | classe utilitária (`final`) | Data local de Brasília, saudação por horário do dia (Bom dia/Boa tarde/Boa noite) e rótulo de período de 7 dias (ex.: "06 à 12 de julho"), usados nos templates de e-mail para clientes. |
| `EmailSender.java` | interface | Contrato comum para qualquer provedor de e-mail (nome do provedor, envio simples, envio com anexos); novos provedores implementam esta interface e são plugados via `email.provider`. |
| `EmailService.java` | `@Service` | Fachada de envio de e-mail; resolve o `EmailSender` ativo a partir de `email.provider` (`sendgrid`/`gmail`/`gmail-api`, default `sendgrid` com warning se valor não reconhecido), sem expor detalhes do provedor aos chamadores. |
| `EmailTemplateService.java` | `@Service` | Carrega templates HTML de `classpath:templates/email/` (preferindo variante `-clean`), substitui variáveis `{{VAR}}` e blocos condicionais `{{#VAR}}...{{/VAR}}`, com mensagem de fallback genérica em caso de falha de leitura. |
| `GmailApiEmailSender.java` | `@Component` (`EmailSender`) | Envia e-mail via Gmail API (REST/HTTPS), reaproveitando o fluxo OAuth do backup no Google Drive (`CredentialManager`); não depende de porta SMTP, propaga link de autorização quando a conta Google ainda não foi autorizada. |
| `GmailSmtpEmailSender.java` | `@Component` (`EmailSender`) | Envia e-mail via SMTP do Gmail (`GMAIL`/`GMAIL_PASSWORD`), com `JavaMailSenderImpl` construído lazily (double-checked locking) e SSL implícito na porta 465 ou STARTTLS nas demais. |
| `SendGridEmailSender.java` | `@Component` (`EmailSender`) | Envia e-mail via API do SendGrid (`sendgrid.api.key`/`sendgrid.from.email`), com anexos em base64 e logo inline embutido do classpath. |
