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

  // Enum para tipos de documento
  private enum DocumentType {
    CPF,
    CNPJ,
    UNKNOWN
  }

  // Enum para tipos de ajuste de final de semana
  private enum WeekendAdjustment {
    NONE,
    PREVIOUS_FRIDAY, // Volta para sexta anterior
    NEXT_FRIDAY // Avança para próxima sexta
  }

  // Classe interna para definir uma regra
  private static class DueDateRule {
    private final int daysToAdd;
    private final WeekendAdjustment weekendAdjustment;

    public DueDateRule(int daysToAdd, WeekendAdjustment weekendAdjustment) {
      this.daysToAdd = daysToAdd;
      this.weekendAdjustment = weekendAdjustment;
    }
  }

  // Mapa de regras por primeiro nome do cliente (CNPJ) - EXATAMENTE como está no banco
  private static final Map<String, DueDateRule> CNPJ_RULES_BY_NAME = new HashMap<>();

  // Regra padrão para CPF
  private static final DueDateRule CPF_DEFAULT_RULE =
      new DueDateRule(15, WeekendAdjustment.PREVIOUS_FRIDAY);

  // Regra padrão para CNPJ (quando não encontrar regra específica)
  private static final DueDateRule CNPJ_DEFAULT_RULE = new DueDateRule(15, WeekendAdjustment.NONE);

  // Regra default geral (quando não for CPF nem CNPJ)
  private static final DueDateRule DEFAULT_RULE = new DueDateRule(20, WeekendAdjustment.NONE);

  static {
    // Configuração das regras específicas por nome (CNPJ)
    // IMPORTANTE: Os nomes devem estar EXATAMENTE como aparecem no banco de dados
    CNPJ_RULES_BY_NAME.put("LLINEA", new DueDateRule(20, WeekendAdjustment.PREVIOUS_FRIDAY));
    CNPJ_RULES_BY_NAME.put("APTA", new DueDateRule(15, WeekendAdjustment.PREVIOUS_FRIDAY));
    CNPJ_RULES_BY_NAME.put("INDUSTRIA", new DueDateRule(20, WeekendAdjustment.NEXT_FRIDAY));

    // === ADICIONE NOVAS REGRAS AQUI ===
    // Exemplo:
    // CNPJ_RULES_BY_NAME.put("EMPRESA", new DueDateRule(30, WeekendAdjustment.PREVIOUS_FRIDAY));
  }

  /**
   * Método principal para calcular a data de vencimento.
   *
   * @param client Cliente para o qual calcular o vencimento
   * @param confirmedAt Data de confirmação base
   * @return Data de vencimento calculada
   */
  public static LocalDate calculate(Client client, LocalDate confirmedAt) {
    if (client == null || confirmedAt == null) {
      return confirmedAt;
    }

    // Identifica o tipo de documento
    DocumentType documentType = getDocumentType(client.getDocument());

    // Obtém a regra aplicável
    DueDateRule rule = getApplicableRule(client, documentType);

    // Calcula a data base
    LocalDate baseDueDate = confirmedAt.plusDays(rule.daysToAdd);

    // Aplica o ajuste de final de semana
    return applyWeekendAdjustment(baseDueDate, rule.weekendAdjustment);
  }

  /** Identifica o tipo de documento (CPF ou CNPJ). */
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

  /** Remove caracteres especiais do documento (pontos, hífens, barras). */
  private static String cleanDocument(String document) {
    if (document == null) {
      return null;
    }
    return document.replaceAll("[.\\-/\\s]", "");
  }

  /** Obtém a regra aplicável para o cliente baseado no tipo de documento e nome. */
  private static DueDateRule getApplicableRule(Client client, DocumentType documentType) {
    switch (documentType) {
      case CPF:
        return CPF_DEFAULT_RULE;

      case CNPJ:
        String firstName = extractFirstName(client.getClientName());
        // Busca a regra específica, senão usa a regra padrão de CNPJ
        return CNPJ_RULES_BY_NAME.getOrDefault(firstName, CNPJ_DEFAULT_RULE);

      default:
        // Caso não seja CPF nem CNPJ, usa a regra default geral
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

  /** Aplica o ajuste de final de semana conforme a estratégia definida. */
  private static LocalDate applyWeekendAdjustment(LocalDate date, WeekendAdjustment adjustment) {
    switch (adjustment) {
      case PREVIOUS_FRIDAY:
        return adjustToPreviousFriday(date);

      case NEXT_FRIDAY:
        return adjustToNextFriday(date);

      case NONE:
      default:
        return date;
    }
  }

  /** Ajusta a data para a sexta-feira anterior se cair em final de semana. */
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

  /** Ajusta a data para a próxima sexta-feira. */
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
