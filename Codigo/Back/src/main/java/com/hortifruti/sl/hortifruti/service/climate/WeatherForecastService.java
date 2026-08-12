package com.hortifruti.sl.hortifruti.service.climate;

import com.hortifruti.sl.hortifruti.config.climate.OpenWeatherClient;
import com.hortifruti.sl.hortifruti.dto.climate.WeatherForecastDTO;
import com.hortifruti.sl.hortifruti.dto.climate.WeatherForecastDTO.DailyForecastDTO;
import com.hortifruti.sl.hortifruti.exception.climate.WeatherApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherForecastService {

  private final OpenWeatherClient weatherClient;

  public WeatherForecastDTO getFiveDayForecast() throws WeatherApiException {
    Map<String, Object> rawData = weatherClient.fetch5DayForecast();
    return processWeatherData(rawData);
  }

  private WeatherForecastDTO processWeatherData(Map<String, Object> rawData)
      throws WeatherApiException {
    Map<String, Object> city = asMap(rawData.get("city"), "city");
    String cityName = asString(city, "name");
    String country = asString(city, "country");

    List<Map<String, Object>> forecastList = asMapList(rawData.get("list"), "list");

    // TreeMap já mantém as datas ordenadas, sem precisar de um sort separado depois.
    Map<LocalDate, List<Map<String, Object>>> groupedByDate = new TreeMap<>();
    for (Map<String, Object> forecastData : forecastList) {
      LocalDate date = extractDate(forecastData);
      groupedByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(forecastData);
    }

    List<DailyForecastDTO> dailyForecasts = new ArrayList<>();
    for (Map.Entry<LocalDate, List<Map<String, Object>>> entry : groupedByDate.entrySet()) {
      if (dailyForecasts.size() >= 5) {
        break;
      }
      dailyForecasts.add(processDailyForecast(entry.getKey(), entry.getValue()));
    }

    return new WeatherForecastDTO(cityName, country, dailyForecasts);
  }

  private DailyForecastDTO processDailyForecast(LocalDate date, List<Map<String, Object>> dayData)
      throws WeatherApiException {
    List<Double> temps = new ArrayList<>();
    List<Double> feelsLike = new ArrayList<>();
    List<Double> humidities = new ArrayList<>();
    List<Double> windSpeeds = new ArrayList<>();
    List<String> descriptions = new ArrayList<>();
    List<String> icons = new ArrayList<>();
    double totalRainfall = 0.0;

    for (Map<String, Object> data : dayData) {
      Map<String, Object> main = asMap(data.get("main"), "main");
      temps.add(asDouble(main, "temp"));
      feelsLike.add(asDouble(main, "feels_like"));
      humidities.add(asDouble(main, "humidity"));

      Map<String, Object> wind = optionalMap(data.get("wind"));
      windSpeeds.add(wind != null ? asDouble(wind, "speed") : 0.0);

      List<Map<String, Object>> weather = asMapList(data.get("weather"), "weather");
      if (weather.isEmpty()) {
        throw new WeatherApiException("Campo \"weather\" veio vazio na resposta da OpenWeather.");
      }
      Map<String, Object> mainWeather = asMap(weather.get(0), "weather[0]");
      descriptions.add(asString(mainWeather, "description"));
      icons.add(asString(mainWeather, "icon"));

      totalRainfall += extractRainfall(data);
    }

    double minTemp = temps.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    double maxTemp = temps.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    double avgTemp = temps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double avgFeelsLike = feelsLike.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double avgHumidity = humidities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double avgWindSpeed =
        windSpeeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

    String mainDescription = mostFrequent(descriptions).orElse("Não disponível");
    String mainIcon = mostFrequent(icons).orElse("01d");

    return new DailyForecastDTO(
        date,
        round1(minTemp),
        round1(maxTemp),
        round1(avgTemp),
        round1(avgFeelsLike),
        round1(avgHumidity),
        Math.round(totalRainfall * 100.0) / 100.0,
        round1(avgWindSpeed),
        mainDescription,
        mainIcon);
  }

  private double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private <T> Optional<T> mostFrequent(List<T> values) {
    return values.stream()
        .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
        .entrySet()
        .stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
  }

  private LocalDate extractDate(Map<String, Object> forecastData) throws WeatherApiException {
    long timestamp = asLong(forecastData, "dt");
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
        .toLocalDate();
  }

  private double extractRainfall(Map<String, Object> forecastData) throws WeatherApiException {
    Map<String, Object> rain = optionalMap(forecastData.get("rain"));
    if (rain != null && rain.containsKey("3h")) {
      return asDouble(rain, "3h");
    }
    return 0.0;
  }

  // Únicos pontos de cast não-verificado do arquivo — toda leitura de campo da resposta da
  // OpenWeather passa por um destes acessores, que valida o tipo e lança WeatherApiException (já
  // tratada com uma resposta de erro clara pelo GlobalExceptionHandler) em vez de deixar um campo
  // ausente/malformado estourar como NullPointerException/ClassCastException não tratado (500
  // genérico).
  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object value, String fieldName) throws WeatherApiException {
    if (!(value instanceof Map)) {
      throw new WeatherApiException(
          "Campo \""
              + fieldName
              + "\" ausente ou em formato inesperado na resposta da OpenWeather.");
    }
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> optionalMap(Object value) {
    return value instanceof Map ? (Map<String, Object>) value : null;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> asMapList(Object value, String fieldName)
      throws WeatherApiException {
    if (!(value instanceof List)) {
      throw new WeatherApiException(
          "Campo \""
              + fieldName
              + "\" ausente ou em formato inesperado na resposta da OpenWeather.");
    }
    return (List<Map<String, Object>>) value;
  }

  private String asString(Map<String, Object> map, String fieldName) throws WeatherApiException {
    Object value = map.get(fieldName);
    if (!(value instanceof String string)) {
      throw new WeatherApiException(
          "Campo \"" + fieldName + "\" ausente ou não é texto na resposta da OpenWeather.");
    }
    return string;
  }

  private double asDouble(Map<String, Object> map, String fieldName) throws WeatherApiException {
    Object value = map.get(fieldName);
    if (!(value instanceof Number number)) {
      throw new WeatherApiException(
          "Campo \"" + fieldName + "\" ausente ou não numérico na resposta da OpenWeather.");
    }
    return number.doubleValue();
  }

  private long asLong(Map<String, Object> map, String fieldName) throws WeatherApiException {
    Object value = map.get(fieldName);
    if (!(value instanceof Number number)) {
      throw new WeatherApiException(
          "Campo \"" + fieldName + "\" ausente ou não numérico na resposta da OpenWeather.");
    }
    return number.longValue();
  }
}
