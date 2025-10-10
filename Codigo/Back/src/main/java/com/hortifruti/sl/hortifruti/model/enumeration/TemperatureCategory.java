package com.hortifruti.sl.hortifruti.model.enumeration;

/** Enum representando as categorias de temperatura para recomendação de produtos */
public enum TemperatureCategory {
  CONGELANDO("Congelando", 0, 5),
  FRIO("Frio", 6, 14),
  AMENO("Ameno", 15, 24),
  QUENTE("Quente", 25, 50);

  private final String displayName;
  private final double minTemp;
  private final double maxTemp;

  TemperatureCategory(String displayName, double minTemp, double maxTemp) {
    this.displayName = displayName;
    this.minTemp = minTemp;
    this.maxTemp = maxTemp;
  }

  public String getDisplayName() {
    return displayName;
  }

  public double getMinTemp() {
    return minTemp;
  }

  public double getMaxTemp() {
    return maxTemp;
  }

  /**
   * Determina a categoria de temperatura baseada na temperatura média
   *
   * @param avgTemp temperatura média em Celsius
   * @return categoria de temperatura
   */
  public static TemperatureCategory fromTemperature(double avgTemp) {
    if (avgTemp <= 5) {
      return CONGELANDO;
    } else if (avgTemp <= 14) {
      return FRIO;
    } else if (avgTemp <= 24) {
      return AMENO;
    } else {
      return QUENTE;
    }
  }

  /**
   * Verifica se a temperatura está nesta categoria
   *
   * @param temperature temperatura a verificar
   * @return true se a temperatura está nesta categoria
   */
  public boolean contains(double temperature) {
    return temperature >= minTemp && temperature <= maxTemp;
  }
}
