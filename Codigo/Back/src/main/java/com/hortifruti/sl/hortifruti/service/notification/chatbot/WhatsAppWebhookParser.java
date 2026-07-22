package com.hortifruti.sl.hortifruti.service.notification.chatbot;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Interpreta o payload bruto do webhook do UltraMsg (WhatsApp), isolando o parsing do formato
 * externo da máquina de estado da conversa em {@code ChatbotConversationHandler}.
 */
@Component
public class WhatsAppWebhookParser {

  public record IncomingMessage(String phoneNumber, String body, boolean fromBot) {}

  /**
   * Extrai a mensagem recebida do webhook, ou {@code Optional.empty()} se o payload não representa
   * uma mensagem de texto privada válida (ex: mensagem de grupo, notificação de status).
   */
  public Optional<IncomingMessage> parse(Map<String, Object> payload) {
    Object dataObj = payload.get("data");
    if (!(dataObj instanceof Map)) {
      return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) dataObj;
    String from = (String) data.getOrDefault("from", "");
    String to = (String) data.getOrDefault("to", "");

    if (!from.endsWith("@c.us")) {
      return Optional.empty();
    }

    String messageType = extractMessageTypeUltraMsg(data);
    if (!"chat".equals(messageType)) {
      return Optional.empty();
    }

    boolean isFromMe = detectIfMessageIsFromBot(data);
    String phoneNumber = extractPhoneFromJid(isFromMe ? to : from);
    String messageBody = extractMessageBodyUltraMsg(data);

    return Optional.of(new IncomingMessage(phoneNumber, messageBody, isFromMe));
  }

  /**
   * Extrai o número de telefone do JID do WhatsApp.
   *
   * <p>Remove o sufixo @c.us do identificador retornando apenas os dígitos.
   *
   * @param jid Identificador completo do WhatsApp (ex: 559999999999@c.us)
   * @return Número de telefone extraído
   */
  private String extractPhoneFromJid(String jid) {
    if (jid == null) return "";
    int at = jid.indexOf("@");
    if (at > 0) {
      return jid.substring(0, at);
    }
    return jid;
  }

  private String extractMessageBodyUltraMsg(Map<String, Object> data) {
    return (String) data.getOrDefault("body", "");
  }

  private String extractMessageTypeUltraMsg(Map<String, Object> data) {
    return (String) data.getOrDefault("type", "chat");
  }

  /**
   * Detecta se a mensagem foi enviada pelo próprio bot/atendente ou pelo cliente.
   *
   * <p>No payload do UltraMsg, mensagens enviadas pelo número conectado (bot ou atendente manual)
   * têm o campo "fromMe" = true. Mensagens recebidas de clientes têm "fromMe" = false ou ausente.
   *
   * @param data Mapa de dados contendo informações da mensagem
   * @return true se a mensagem foi enviada pelo bot/atendente, false se foi do cliente
   */
  private boolean detectIfMessageIsFromBot(Map<String, Object> data) {
    Object fromMeObj = data.get("fromMe");

    if (fromMeObj instanceof Boolean) {
      return (Boolean) fromMeObj;
    }

    if (fromMeObj instanceof String) {
      String fromMeStr = (String) fromMeObj;
      return "true".equalsIgnoreCase(fromMeStr) || "1".equals(fromMeStr);
    }

    Object fromMe2Obj = data.get("from_me");
    if (fromMe2Obj instanceof Boolean) {
      return (Boolean) fromMe2Obj;
    }

    if (fromMe2Obj instanceof String) {
      String fromMe2Str = (String) fromMe2Obj;
      return "true".equalsIgnoreCase(fromMe2Str) || "1".equals(fromMe2Str);
    }

    return false;
  }
}
