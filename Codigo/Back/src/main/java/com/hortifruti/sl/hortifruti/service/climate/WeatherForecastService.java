package com.hortifruti.sl.hortifruti.service.climate;

import com.hortifruti.sl.hortifruti.config.climate.OpenWeatherClient;
import com.hortifruti.sl.hortifruti.dto.climate.WeatherForecastDTO;
import com.hortifruti.sl.hortifruti.dto.climate.WeatherForecastDTO.DailyForecastDTO;
import com.hortifruti.sl.hortifruti.exception.WeatherApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class WeatherForecastService {

  @Autowired private final OpenWeatherClient weatherClient;

  /** Obtém a previsão do tempo para 5 dias */
  public WeatherForecastDTO getFiveDayForecast() throws WeatherApiException {
    Map<String, Object> rawData = weatherClient.fetch5DayForecast();
    return processWeatherData(rawData);
  }

  @SuppressWarnings("unchecked")
  private WeatherForecastDTO processWeatherData(Map<String, Object> rawData) {
    Map<String, Object> city = (Map<String, Object>) rawData.get("city");
    String cityName = (String) city.get("name");
    String country = (String) city.get("country");

    List<Map<String, Object>> forecastList = (List<Map<String, Object>>) rawData.get("list");

    Map<LocalDate, List<Map<String, Object>>> groupedByDate =
        forecastList.stream().collect(Collectors.groupingBy(this::extractDate));

    List<DailyForecastDTO> dailyForecasts =
        groupedByDate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .limit(5) 
            .map(entry -> processDailyForecast(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    return new WeatherForecastDTO(cityName, country, dailyForecasts);
  }

  @SuppressWarnings("unchecked")
  private DailyForecastDTO processDailyForecast(LocalDate date, List<Map<String, Object>> dayData) {
    List<Double> temps =
        dayData.stream()
            .map(
                data -> {
                  Map<String, Object> main = (Map<String, Object>) data.get("main");
                  return ((Number) main.get("temp")).doubleValue();
                })
            .collect(Collectors.toList());

    List<Double> feelsLike =
        dayData.stream()
            .map(
                data -> {
                  Map<String, Object> main = (Map<String, Object>) data.get("main");
                  return ((Number) main.get("feels_like")).doubleValue();
                })
            .collect(Collectors.toList());

    double minTemp = temps.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    double maxTemp = temps.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    double avgTemp = temps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double avgFeelsLike = feelsLike.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

    double avgHumidity =
        dayData.stream()
            .mapToDouble(
                data -> {
                  Map<String, Object> main = (Map<String, Object>) data.get("main");
                  return ((Number) main.get("humidity")).doubleValue();
                })
            .average()
            .orElse(0.0);

    double totalRainfall = dayData.stream().mapToDouble(this::extractRainfall).sum();

    double avgWindSpeed =
        dayData.stream()
            .mapToDouble(
                data -> {
                  Map<String, Object> wind = (Map<String, Object>) data.get("wind");
                  return wind != null ? ((Number) wind.get("speed")).doubleValue() : 0.0;
                })
            .average()
            .orElse(0.0);

    Map<String, Long> weatherDescriptions =
        dayData.stream()
            .collect(
                Collectors.groupingBy(
                    data -> {
                      List<Map<String, Object>> weather =
                          (List<Map<String, Object>>) data.get("weather");
                      return (String) weather.get(0).get("description");
                    },
                    Collectors.counting()));

    String mainDescription =
        weatherDescriptions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Não disponível");

    String mainIcon =
        dayData.stream()
            .collect(
                Collectors.groupingBy(
                    data -> {
                      List<Map<String, Object>> weather =
                          (List<Map<String, Object>>) data.get("weather");
                      return (String) weather.get(0).get("icon");
                    },
                    Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("01d");

    return new DailyForecastDTO(
        date,
        Math.round(minTemp * 10.0) / 10.0, 
        Math.round(maxTemp * 10.0) / 10.0,
        Math.round(avgTemp * 10.0) / 10.0,
        Math.round(avgFeelsLike * 10.0) / 10.0,
        Math.round(avgHumidity * 10.0) / 10.0,
        Math.round(totalRainfall * 100.0) / 100.0,
        Math.round(avgWindSpeed * 10.0) / 10.0,
        mainDescription,
        mainIcon);
  }

  private LocalDate extractDate(Map<String, Object> forecastData) {
    long timestamp = ((Number) forecastData.get("dt")).longValue();
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
        .toLocalDate();
  }

  @SuppressWarnings("unchecked")
  private double extractRainfall(Map<String, Object> forecastData) {
    Map<String, Object> rain = (Map<String, Object>) forecastData.get("rain");
    if (rain != null && rain.containsKey("3h")) {
      return ((Number) rain.get("3h")).doubleValue();
    }
    return 0.0;
  }
}
