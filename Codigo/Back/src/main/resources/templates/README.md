# src/main/resources/templates

Diretório de templates usados pela aplicação. Não contém arquivos diretamente — todos os templates atuais são de e-mail e ficam no subpacote abaixo.

## Subpacotes

- `email/` — templates HTML de e-mails transacionais (alertas de segurança, relatórios de boletos pendentes, documentos de clientes) processados por `EmailTemplateService` (ver `email/README.md`).
