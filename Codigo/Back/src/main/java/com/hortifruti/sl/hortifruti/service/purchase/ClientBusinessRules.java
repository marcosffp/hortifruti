package com.hortifruti.sl.hortifruti.service.purchase;

import java.util.HashMap;
import java.util.Map;

/**
 * Cadastro único de regras de negócio por cliente (identificado pelo primeiro nome, EXATAMENTE como
 * está no banco). Centraliza aqui qualquer regra hoje espalhada por caso especial de cliente (ex.:
 * vencimento, texto de nota fiscal) para evitar que cada regra seja reimplementada de forma
 * independente em serviços diferentes.
 */
public class ClientBusinessRules {

  public enum WeekendAdjustment {
    NONE,
    PREVIOUS_FRIDAY,
    PREVIOUS_THURSDAY,
    NEXT_FRIDAY
  }

  public static class ClientRule {
    private final int dueDateDaysToAdd;
    private final WeekendAdjustment dueDateWeekendAdjustment;
    private final boolean dueDateBusinessDays; // true = dias úteis, false = dias corridos
    private final String invoiceNoteTemplate; // null = usa o texto padrão da nota

    public ClientRule(
        int dueDateDaysToAdd,
        WeekendAdjustment dueDateWeekendAdjustment,
        boolean dueDateBusinessDays,
        String invoiceNoteTemplate) {
      this.dueDateDaysToAdd = dueDateDaysToAdd;
      this.dueDateWeekendAdjustment = dueDateWeekendAdjustment;
      this.dueDateBusinessDays = dueDateBusinessDays;
      this.invoiceNoteTemplate = invoiceNoteTemplate;
    }

    public int getDueDateDaysToAdd() {
      return dueDateDaysToAdd;
    }

    public WeekendAdjustment getDueDateWeekendAdjustment() {
      return dueDateWeekendAdjustment;
    }

    public boolean isDueDateBusinessDays() {
      return dueDateBusinessDays;
    }

    /** Monta o texto da nota fiscal para este cliente, ou {@code null} se deve usar o padrão. */
    public String buildInvoiceNoteText(String dadosAdicionais) {
      if (invoiceNoteTemplate == null) {
        return null;
      }
      return invoiceNoteTemplate.replace("{dadosAdicionais}", String.valueOf(dadosAdicionais));
    }
  }

  // Regra padrão para CNPJ (quando não encontrar regra específica para o cliente)
  public static final ClientRule CNPJ_DEFAULT_RULE =
      new ClientRule(15, WeekendAdjustment.NONE, false, null);

  // Mapa de regras por primeiro nome do cliente (CNPJ) - EXATAMENTE como está no banco
  private static final Map<String, ClientRule> RULES_BY_NAME = new HashMap<>();

  static {
    // IMPORTANTE: Os nomes devem estar EXATAMENTE como aparecem no banco de dados
    RULES_BY_NAME.put(
        "LLINEA",
        new ClientRule(
            20, WeekendAdjustment.PREVIOUS_THURSDAY, false, "Numerações AF: {dadosAdicionais}"));
    RULES_BY_NAME.put("APTA", new ClientRule(15, WeekendAdjustment.PREVIOUS_FRIDAY, false, null));
    RULES_BY_NAME.put("INDUSTRIA", new ClientRule(20, WeekendAdjustment.NEXT_FRIDAY, false, null));
    RULES_BY_NAME.put(
        "ROCA", new ClientRule(15, WeekendAdjustment.NONE, true, null)); // 15 dias úteis

    // === ADICIONE NOVAS REGRAS AQUI (vencimento e/ou texto da nota) ===
    // Exemplo dias corridos:  RULES_BY_NAME.put("EMPRESA", new ClientRule(30,
    // WeekendAdjustment.PREVIOUS_FRIDAY, false, null));
    // Exemplo dias úteis:     RULES_BY_NAME.put("EMPRESA", new ClientRule(30,
    // WeekendAdjustment.NONE, true, null));
  }

  private ClientBusinessRules() {}

  /**
   * Busca a regra cadastrada para o cliente pelo primeiro nome (comparação case-insensitive, já que
   * os dois usos anteriores desse caso especial comparavam de formas diferentes: um exato, outro
   * via nome em maiúsculas). Retorna {@link #CNPJ_DEFAULT_RULE} se não houver regra específica.
   */
  public static ClientRule getRuleForCnpjClient(String firstName) {
    if (firstName == null) {
      return CNPJ_DEFAULT_RULE;
    }
    return RULES_BY_NAME.getOrDefault(firstName.trim().toUpperCase(), CNPJ_DEFAULT_RULE);
  }
}
