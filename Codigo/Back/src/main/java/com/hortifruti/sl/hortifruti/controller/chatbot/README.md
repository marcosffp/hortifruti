# com.hortifruti.sl.hortifruti.controller.chatbot

Endpoints do chatbot de WhatsApp: webhook de recebimento de mensagens via UltraMsg API e endpoints auxiliares de verificação/teste.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `ChatbotController.java` | `@RestController` (`/chatbot`) | `POST /chatbot/webhook` recebe mensagens do WhatsApp via UltraMsg, validando um token compartilhado (query param `token`) comparado em tempo constante com `chatbot.webhook.secret` antes de processar (`ChatbotService.processIncomingMessage`); `GET /chatbot/webhook` endpoint de verificação/health do webhook; `POST /chatbot/test` simula uma mensagem via `phoneNumber`/`message` para testes manuais; `POST /chatbot/test-json` simula uma mensagem com payload JSON completo, similar ao enviado pela UltraMsg. |
