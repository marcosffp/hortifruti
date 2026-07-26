# com.hortifruti.sl.hortifruti.service.chatbot

Atendimento automatizado via WhatsApp (webhook UltraMsg): máquina de estado de conversa (menu, consulta de boletos por CPF/CNPJ, consulta de nota fiscal por número, encaminhamento para atendimento humano) com sessões persistidas em banco. Integra com `service.billet` (emissão/2ª via de boleto), `service.invoice` (consulta/DANFE) e `service.notification.whatsapp` (envio de mensagens).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ChatbotService.java` | `@Service` | Ponto de entrada do webhook: extrai a mensagem via `WhatsAppWebhookParser` e encaminha para `ChatbotConversationHandler` (ou trata eco de mensagem enviada pelo próprio bot). |
| `WhatsAppWebhookParser.java` | `@Component` | Interpreta o payload bruto do UltraMsg: filtra mensagens privadas de texto (`@c.us`, tipo `chat`), extrai telefone e detecta se a mensagem foi enviada pelo bot/atendente (`fromMe`). |
| `ChatbotConversationHandler.java` | `@Component` | Máquina de estado da conversa: processa comandos (`menu`, seleção numérica 1-4), consulta boletos pendentes de um cliente por documento, consulta nota fiscal por número (varre todas as refs com nota emitida até achar o número informado) e baixa/envia o DANFE. Detecta resposta manual de atendente (fora do threshold de 10s do bot) e pausa o bot por 1 hora. |
| `ChatbotMessageTemplates.java` | classe utilitária (`final`, métodos estáticos) | Constrói todo o texto das mensagens enviadas (menu, prompts, erros, resumo de cobranças pendentes), isolando o "copy" da lógica de estados. |
| `ChatSessionService.java` | `@Service` | CRUD e transições de `ChatSession` (status, contexto, cliente associado, pausa/despausa do bot, fechamento de sessão). |
| `ChatSessionCleanupService.java` | `@Service` | Job agendado (`@Scheduled` a cada 5 min) que remove pausas expiradas e volta sessões para o status `MENU`. |
