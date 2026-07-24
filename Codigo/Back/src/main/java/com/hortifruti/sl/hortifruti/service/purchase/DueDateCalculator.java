package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.service.purchase.ClientBusinessRules.ClientRule;
import com.hortifruti.sl.hortifruti.service.purchase.ClientBusinessRules.WeekendAdjustment;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Classe responsável por calcular a data de vencimento baseada em regras de negócio por cliente.
 * As regras específicas por cliente ficam centralizadas em {@link ClientBusinessRules}.
 */
public class DueDateCalculator {

  private enum DocumentType {
    CPF,
    CNPJ,
    UNKNOWN
  }

  private static final ClientRule CPF_DEFAULT_RULE =
      new ClientRule(15, WeekendAdjustment.PREVIOUS_FRIDAY, false, null);

  // Regra default geral (quando não for CPF nem CNPJ)
  private static final ClientRule DEFAULT_RULE =
      new ClientRule(20, WeekendAdjustment.NONE, false, null);

  public static LocalDate calculate(Client client, LocalDate confirmedAt) {
    if (client == null || confirmedAt == null) {
      return confirmedAt;
    }

    DocumentType documentType = getDocumentType(client.getDocument());

    ClientRule rule = getApplicableRule(client, documentType);

    LocalDate baseDueDate =
        rule.isDueDateBusinessDays()
            ? addBusinessDays(confirmedAt, rule.getDueDateDaysToAdd())
            : confirmedAt.plusDays(rule.getDueDateDaysToAdd());

    // Aplica o ajuste de final de semana (apenas para dias corridos)
    return applyWeekendAdjustment(baseDueDate, rule.getDueDateWeekendAdjustment());
  }

  private static DocumentType getDocumentType(String document) {
    String cleanDoc = cleanDocument(document);

    if (cleanDoc == null) {
      return DocumentType.UNKNOWN;
    }

    if (cleanDoc.length() == 11) {
      return DocumentType.CPF;
    } else if (cleanDoc.length() == 14) {
      return DocumentType.CNPJ;
    }

    return DocumentType.UNKNOWN;
  }

  private static String cleanDocument(String document) {
    if (document == null) {
      return null;
    }
    return document.replaceAll("[.\\-/\\s]", "");
  }

  private static ClientRule getApplicableRule(Client client, DocumentType documentType) {
    switch (documentType) {
      case CPF:
        return CPF_DEFAULT_RULE;

      case CNPJ:
        String firstName = extractFirstName(client.getClientName());
        return ClientBusinessRules.getRuleForCnpjClient(firstName);

      default:
        return DEFAULT_RULE;
    }
  }

  /**
   * Extrai o primeiro nome do cliente EXATAMENTE como está no banco. Mantém acentos, capitalização
   * e caracteres especiais.
   */
  private static String extractFirstName(String fullName) {
    if (fullName == null || fullName.trim().isEmpty()) {
      return "";
    }

    String[] parts = fullName.trim().split("\\s+");
    return parts.length > 0 ? parts[0] : "";
  }

  /**
   * Soma dias úteis (pula sábados, domingos e feriados nacionais) a partir de uma data base. O dia
   * de início (confirmedAt) não é contado — começa a contar a partir do dia seguinte.
   */
  private static LocalDate addBusinessDays(LocalDate startDate, int businessDays) {
    LocalDate date = startDate;
    int added = 0;

    while (added < businessDays) {
      date = date.plusDays(1);
      if (!isNonBusinessDay(date)) {
        added++;
      }
    }

    return date;
  }

  /**
   * Aplica o ajuste de final de semana conforme a estratégia definida e, em seguida, garante que a
   * data final não caia em final de semana nem em feriado nacional.
   */
  private static LocalDate applyWeekendAdjustment(LocalDate date, WeekendAdjustment adjustment) {
    switch (adjustment) {
      case PREVIOUS_FRIDAY:
        return skipNonBusinessDays(adjustToPreviousFriday(date), -1);

      case PREVIOUS_THURSDAY:
        return skipNonBusinessDays(adjustToPreviousThursday(date), -1);

      case NEXT_FRIDAY:
        return skipNonBusinessDays(adjustToNextFriday(date), 1);

      case NONE:
      default:
        return skipNonBusinessDays(date, 1);
    }
  }

  /**
   * Anda em direção a datas passadas (direction = -1) ou futuras (direction = 1) até encontrar um
   * dia que não seja final de semana nem feriado nacional.
   */
  private static LocalDate skipNonBusinessDays(LocalDate date, int direction) {
    LocalDate result = date;
    while (isNonBusinessDay(result)) {
      result = result.plusDays(direction);
    }
    return result;
  }

  private static boolean isNonBusinessDay(LocalDate date) {
    return isWeekend(date) || BrazilianHolidays.isHoliday(date);
  }

  private static boolean isWeekend(LocalDate date) {
    DayOfWeek dow = date.getDayOfWeek();
    return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
  }

  private static LocalDate adjustToPreviousFriday(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();

    switch (dayOfWeek) {
      case SATURDAY:
        return date.minusDays(1);
      case SUNDAY:
        return date.minusDays(2);
      default:
        return date;
    }
  }

  /**
   * Ajusta a data para a quinta-feira anterior se cair em final de semana. Sábado → volta 2 dias
   * (quinta). Domingo → volta 3 dias (quinta).
   */
  private static LocalDate adjustToPreviousThursday(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();

    switch (dayOfWeek) {
      case SATURDAY:
        return date.minusDays(2);
      case SUNDAY:
        return date.minusDays(3);
      default:
        return date;
    }
  }

  private static LocalDate adjustToNextFriday(LocalDate date) {
    if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
      return date;
    }

    int currentDayValue = date.getDayOfWeek().getValue();
    int fridayValue = DayOfWeek.FRIDAY.getValue();

    int daysUntilFriday =
        currentDayValue < fridayValue
            ? fridayValue - currentDayValue
            : 7 - currentDayValue + fridayValue;

    return date.plusDays(daysUntilFriday);
  }
}
