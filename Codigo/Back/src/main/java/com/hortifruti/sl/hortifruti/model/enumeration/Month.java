package com.hortifruti.sl.hortifruti.model.enumeration;

/** Enum representando os meses do ano para sistema de temporadas */
public enum Month {
  JANEIRO(1, "Janeiro"),
  FEVEREIRO(2, "Fevereiro"),
  MARCO(3, "Março"),
  ABRIL(4, "Abril"),
  MAIO(5, "Maio"),
  JUNHO(6, "Junho"),
  JULHO(7, "Julho"),
  AGOSTO(8, "Agosto"),
  SETEMBRO(9, "Setembro"),
  OUTUBRO(10, "Outubro"),
  NOVEMBRO(11, "Novembro"),
  DEZEMBRO(12, "Dezembro");

  private final int number;
  private final String displayName;

  Month(int number, String displayName) {
    this.number = number;
    this.displayName = displayName;
  }

  public int getNumber() {
    return number;
  }

  public String getDisplayName() {
    return displayName;
  }

  /** Obtém o mês atual baseado no mês do sistema */
  public static Month getCurrentMonth() {
    int currentMonth = java.time.LocalDate.now().getMonthValue();
    for (Month month : values()) {
      if (month.getNumber() == currentMonth) {
        return month;
      }
    }
    return JANEIRO; // fallback
  }

  /** Verifica se o mês está na temporada (lista de meses) */
  public static boolean isInSeason(java.util.Set<Month> seasons) {
    return seasons.contains(getCurrentMonth());
  }

  @Override
  public String toString() {
    return displayName;
  }
}
