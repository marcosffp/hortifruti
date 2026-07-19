package com.hortifruti.sl.hortifruti.service.notification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Saudação e período (data local de Brasília) usados no corpo dos emails para clientes. */
final class EmailGreetingUtil {

  private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
  private static final DateTimeFormatter DAY_MONTH =
      DateTimeFormatter.ofPattern("dd 'de' MMMM", PT_BR);
  private static final DateTimeFormatter DAY_ONLY = DateTimeFormatter.ofPattern("dd", PT_BR);

  private EmailGreetingUtil() {}

  static LocalDate today() {
    return ZonedDateTime.now(BRAZIL_ZONE).toLocalDate();
  }

  static String timeOfDayGreeting() {
    int hour = ZonedDateTime.now(BRAZIL_ZONE).getHour();
    if (hour < 12) {
      return "Bom dia";
    }
    if (hour < 18) {
      return "Boa tarde";
    }
    return "Boa noite";
  }

  /** Período de 7 dias terminando em {@code end} (inclusive). Ex: "06 à 12 de julho". */
  static String weekPeriodLabel(LocalDate end) {
    LocalDate start = end.minusDays(6);
    if (start.getMonth() == end.getMonth() && start.getYear() == end.getYear()) {
      return start.format(DAY_ONLY) + " à " + end.format(DAY_MONTH);
    }
    return start.format(DAY_MONTH) + " à " + end.format(DAY_MONTH);
  }
}
