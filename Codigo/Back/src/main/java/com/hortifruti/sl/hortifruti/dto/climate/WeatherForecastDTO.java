package com.hortifruti.sl.hortifruti.dto.climate;

import java.time.LocalDate;
import java.util.List;

/** DTO para retorno da previsão do tempo de 5 dias */
public record WeatherForecastDTO(
    String city, String country, List<DailyForecastDTO> dailyForecasts) {

  public record DailyForecastDTO(
      LocalDate date,
      double minTemp,
      double maxTemp,
      double avgTemp,
      double avgFeelsLike,
      double humidity,
      double rainfall,
      double windSpeed,
      String weatherDescription,
      String weatherIcon) {}
}
