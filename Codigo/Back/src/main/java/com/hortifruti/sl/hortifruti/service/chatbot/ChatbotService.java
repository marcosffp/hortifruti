package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.service.notification.chatbot.ChatbotConversationHandler;
import com.hortifruti.sl.hortifruti.service.notification.chatbot.WhatsAppWebhookParser;
import com.hortifruti.sl.hortifruti.service.notification.chatbot.WhatsAppWebhookParser.IncomingMessage;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Ponto de entrada do webhook do WhatsApp. O parsing do payload fica em {@link
 * WhatsAppWebhookParser} e a máquina de estado da conversa em {@link ChatbotConversationHandler} —
 * esta classe só liga as duas pontas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

  private final WhatsAppWebhookParser webhookParser;
  private final ChatbotConversationHandler conversationHandler;

  /**
   * Processa mensagens recebidas do webhook do WhatsApp.
   *
   * <p>Extrai informações do payload, valida se é uma mensagem privada válida e encaminha para
   * processamento de comandos.
   *
   * <p>Detecta automaticamente mensagens manuais enviadas por atendentes e pausa o bot por 1 hora
   * para evitar conflitos.
   *
   * @param payload Dados recebidos do webhook contendo informações da mensagem
   */
  public void processIncomingMessage(Map<String, Object> payload) {
    try {
      Optional<IncomingMessage> incoming = webhookParser.parse(payload);
      if (incoming.isEmpty()) {
        return;
      }

      IncomingMessage message = incoming.get();
      if (message.fromBot()) {
        conversationHandler.handleBotEcho(message.phoneNumber());
        return;
      }

      conversationHandler.processCommand(message.phoneNumber(), message.body());

    } catch (Exception e) {
      log.error("Erro ao processar mensagem do webhook", e);
    }
  }
}
