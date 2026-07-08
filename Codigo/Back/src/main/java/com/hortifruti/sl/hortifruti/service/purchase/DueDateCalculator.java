package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.Client;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe responsável por calcular a data de vencimento baseada em regras de negócio por cliente.
 * Facilita a adição de novas regras e mantém o código organizado e extensível.
 */
public class DueDateCalculator {

  private enum DocumentType {
    CPF,
    CNPJ,
    UNKNOWN
  }

  private enum WeekendAdjustment {
    NONE,
    PREVIOUS_FRIDAY,
    PREVIOUS_THURSDAY,
    NEXT_FRIDAY
  }

  private static class DueDateRule {
    private final int daysToAdd;
    private final WeekendAdjustment weekendAdjustment;
    private final boolean businessDays; // true = dias úteis, false = dias corridos

    public DueDateRule(int daysToAdd, WeekendAdjustment weekendAdjustment) {
      this(daysToAdd, weekendAdjustment, false);
    }

    public DueDateRule(int daysToAdd, WeekendAdjustment weekendAdjustment, boolean businessDays) {
      this.daysToAdd = daysToAdd;
      this.weekendAdjustment = weekendAdjustment;
      this.businessDays = businessDays;
    }
  }

  // Mapa de regras por primeiro nome do cliente (CNPJ) - EXATAMENTE como está no banco
  private static final Map<String, DueDateRule> CNPJ_RULES_BY_NAME = new HashMap<>();

  private static final DueDateRule CPF_DEFAULT_RULE =
      new DueDateRule(15, WeekendAdjustment.PREVIOUS_FRIDAY);

  // Regra padrão para CNPJ (quando não encontrar regra específica)
  private static final DueDateRule CNPJ_DEFAULT_RULE = new DueDateRule(15, WeekendAdjustment.NONE);

  // Regra default geral (quando não for CPF nem CNPJ)
  private static final DueDateRule DEFAULT_RULE = new DueDateRule(20, WeekendAdjustment.NONE);

  static {
    // IMPORTANTE: Os nomes devem estar EXATAMENTE como aparecem no banco de dados
    CNPJ_RULES_BY_NAME.put("LLINEA", new DueDateRule(20, WeekendAdjustment.PREVIOUS_THURSDAY));
    CNPJ_RULES_BY_NAME.put("APTA", new DueDateRule(15, WeekendAdjustment.PREVIOUS_FRIDAY));
    CNPJ_RULES_BY_NAME.put("INDUSTRIA", new DueDateRule(20, WeekendAdjustment.NEXT_FRIDAY));
    CNPJ_RULES_BY_NAME.put(
        "ROCA", new DueDateRule(15, WeekendAdjustment.NONE, true)); // 15 dias úteis

    // === ADICIONE NOVAS REGRAS AQUI ===
    // Exemplo dias corridos:  CNPJ_RULES_BY_NAME.put("EMPRESA", new DueDateRule(30,
    // WeekendAdjustment.PREVIOUS_FRIDAY));
    // Exemplo dias úteis:     CNPJ_RULES_BY_NAME.put("EMPRESA", new DueDateRule(30,
    // WeekendAdjustment.NONE, true));
  }

  public static LocalDate calculate(Client client, LocalDate confirmedAt) {
    if (client == null || confirmedAt == null) {
      return confirmedAt;
    }

    DocumentType documentType = getDocumentType(client.getDocument());

    DueDateRule rule = getApplicableRule(client, documentType);

    LocalDate baseDueDate =
        rule.businessDays
            ? addBusinessDays(confirmedAt, rule.daysToAdd)
            : confirmedAt.plusDays(rule.daysToAdd);

    // Aplica o ajuste de final de semana (apenas para dias corridos)
    return applyWeekendAdjustment(baseDueDate, rule.weekendAdjustment);
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

  private static DueDateRule getApplicableRule(Client client, DocumentType documentType) {
    switch (documentType) {
      case CPF:
        return CPF_DEFAULT_RULE;

      case CNPJ:
        String firstName = extractFirstName(client.getClientName());
        return CNPJ_RULES_BY_NAME.getOrDefault(firstName, CNPJ_DEFAULT_RULE);

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
