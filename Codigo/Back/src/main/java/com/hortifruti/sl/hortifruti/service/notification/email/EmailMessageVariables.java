package com.hortifruti.sl.hortifruti.service.notification.email;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Blocos de variáveis de template repetidos quase palavra-por-palavra entre {@code
 * NotificationService} e {@code BulkNotificationService} — cada um chamava {@code
 * emailTemplateService.processTemplate} com o mesmo conjunto de chaves montado de forma
 * independente, então uma correção (ex.: formato de data, regra do saudação) precisava ser
 * replicada nos dois lugares.
 */
public final class EmailMessageVariables {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private EmailMessageVariables() {}

  /**
   * CLIENT_NAME/CURRENT_DATE/GREETING/PERIOD_RANGE/SENDER_NAME/DEFAULT_MESSAGE/CUSTOM_MESSAGE —
   * usado pelos dois lugares que preenchem o template "client-documents"
   * (NotificationService.buildClientMessage e BulkNotificationService.buildClientMessage).
   */
  public static void putClientGreetingVariables(
      Map<String, String> variables, String clientName, String senderName, String customMessage) {
    variables.put("CLIENT_NAME", clientName);

    LocalDate today = EmailGreetingUtil.today();
    variables.put("CURRENT_DATE", today.format(DATE_FORMATTER));
    variables.put("GREETING", EmailGreetingUtil.timeOfDayGreeting());
    variables.put("PERIOD_RANGE", EmailGreetingUtil.weekPeriodLabel(today));
    variables.put("SENDER_NAME", senderName);

    variables.put("DEFAULT_MESSAGE", "true");
    variables.put("CUSTOM_MESSAGE", customMessage != null ? customMessage : "");
  }

  /**
   * CUSTOM_MESSAGE/DEFAULT_MESSAGE mutuamente exclusivos (um dos dois vira {@code "true"}, o outro
   * fica vazio) — usado por NotificationService.buildGenericFilesMessage e
   * BulkNotificationService.buildAccountingMessage.
   */
  public static void putCustomOrDefaultToggle(Map<String, String> variables, String customMessage) {
    boolean hasCustomMessage = customMessage != null && !customMessage.isEmpty();
    variables.put("CUSTOM_MESSAGE", hasCustomMessage ? customMessage : "");
    variables.put("DEFAULT_MESSAGE", hasCustomMessage ? "" : "true");
  }
}
