# com.hortifruti.sl.hortifruti.service.notification.whatsapp

Canal exclusivamente de envio (outbound) via WhatsApp através da API Ultramsg — não há
processamento de mensagens recebidas do cliente. Cobre montagem do texto das mensagens
(independente dos templates de e-mail) e o envio em si (texto e documentos, PDF ou XML).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `WhatsAppMessageBuilder.java` | `@Service` | Monta o texto das mensagens de WhatsApp por finalidade: extratos mensais, arquivos genéricos com resumo financeiro, documentos de cliente, boleto (PDF), XML de NF-e e mensagem genérica; incorpora mensagem customizada opcional onde aplicável. |
| `WhatsAppService.java` | `@Service` | Integração com a API Ultramsg: normaliza número de telefone brasileiro para formato internacional (`+55DDNNNNNNNNN`, cobrindo variações de 8 a 11+ dígitos), envia mensagem de texto, documento único (PDF/XML, como base64 inline — não depende de URL pública) ou múltiplos documentos em sequência (delay de 2s entre eles para evitar rate limit). |
