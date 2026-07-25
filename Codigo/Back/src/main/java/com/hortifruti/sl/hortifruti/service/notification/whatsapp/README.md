# com.hortifruti.sl.hortifruti.service.notification.whatsapp

Envio de mensagens e documentos via WhatsApp através da API Ultramsg, e montagem do texto das
mensagens (independente dos templates de e-mail).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `WhatsAppMessageBuilder.java` | `@Service` | Monta o texto das mensagens de WhatsApp por finalidade: extratos mensais, arquivos genéricos com resumo financeiro, documentos de cliente e mensagem genérica; incorpora mensagem customizada opcional em todas. |
| `WhatsAppService.java` | `@Service` | Integração com a API Ultramsg: normaliza número de telefone brasileiro para formato internacional (`+55DDNNNNNNNNN`, cobrindo variações de 8 a 11+ dígitos), envia mensagem de texto, documento único ou múltiplos documentos em sequência (delay de 2s entre eles para evitar rate limit). |
