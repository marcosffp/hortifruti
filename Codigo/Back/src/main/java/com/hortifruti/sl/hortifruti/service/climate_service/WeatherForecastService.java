package com.hortifruti.sl.hortifruti.service.climate_service;

import com.hortifruti.sl.hortifruti.config.client.OpenWeatherClient;
import com.hortifruti.sl.hortifruti.dto.climate_dto.WeatherForecastDTO;
import com.hortifruti.sl.hortifruti.dto.climate_dto.WeatherForecastDTO.DailyForecastDTO;
import com.hortifruti.sl.hortifruti.exception.WeatherApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherForecastService {
    
    private final OpenWeatherClient weatherClient;
    
    /**
     * Obtém a previsão do tempo para 5 dias
     */
    public WeatherForecastDTO getFiveDayForecast() throws WeatherApiException {
        Map<String, Object> rawData = weatherClient.fetch5DayForecast();
        return processWeatherData(rawData);
    }
    
    @SuppressWarnings("unchecked")
    private WeatherForecastDTO processWeatherData(Map<String, Object> rawData) {
        // Extrair informações da cidade
        Map<String, Object> city = (Map<String, Object>) rawData.get("city");
        String cityName = (String) city.get("name");
        String country = (String) city.get("country");
        
        // Extrair lista de previsões e agrupar por data
        List<Map<String, Object>> forecastList = (List<Map<String, Object>>) rawData.get("list");
        
        // Agrupar previsões por data
        Map<LocalDate, List<Map<String, Object>>> groupedByDate = forecastList.stream()
                .collect(Collectors.groupingBy(this::extractDate));
        
        // Processar cada dia
        List<DailyForecastDTO> dailyForecasts = groupedByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(5) // Garantir apenas 5 dias
                .map(entry -> processDailyForecast(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        
        return new WeatherForecastDTO(cityName, country, dailyForecasts);
    }
    
    @SuppressWarnings("unchecked")
    private DailyForecastDTO processDailyForecast(LocalDate date, List<Map<String, Object>> dayData) {
        // Calcular temperaturas min/max/média
        List<Double> temps = dayData.stream()
                .map(data -> {
                    Map<String, Object> main = (Map<String, Object>) data.get("main");
                    return ((Number) main.get("temp")).doubleValue();
                })
                .collect(Collectors.toList());
        
        // Calcular sensação térmica média
        List<Double> feelsLike = dayData.stream()
                .map(data -> {
                    Map<String, Object> main = (Map<String, Object>) data.get("main");
                    return ((Number) main.get("feels_like")).doubleValue();
                })
                .collect(Collectors.toList());
        
        double minTemp = temps.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double maxTemp = temps.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double avgTemp = temps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgFeelsLike = feelsLike.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Calcular umidade média
        double avgHumidity = dayData.stream()
                .mapToDouble(data -> {
                    Map<String, Object> main = (Map<String, Object>) data.get("main");
                    return ((Number) main.get("humidity")).doubleValue();
                })
                .average().orElse(0.0);
        
        // Calcular chuva total do dia
        double totalRainfall = dayData.stream()
                .mapToDouble(this::extractRainfall)
                .sum();
        
        // Calcular velocidade média do vento
        double avgWindSpeed = dayData.stream()
                .mapToDouble(data -> {
                    Map<String, Object> wind = (Map<String, Object>) data.get("wind");
                    return wind != null ? ((Number) wind.get("speed")).doubleValue() : 0.0;
                })
                .average().orElse(0.0);
        
        // Pegar descrição do tempo mais frequente
        Map<String, Long> weatherDescriptions = dayData.stream()
                .collect(Collectors.groupingBy(
                    data -> {
                        List<Map<String, Object>> weather = (List<Map<String, Object>>) data.get("weather");
                        return (String) weather.get(0).get("description");
                    },
                    Collectors.counting()
                ));
        
        String mainDescription = weatherDescriptions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Não disponível");
        
        // Pegar ícone mais frequente
        String mainIcon = dayData.stream()
                .collect(Collectors.groupingBy(
                    data -> {
                        List<Map<String, Object>> weather = (List<Map<String, Object>>) data.get("weather");
                        return (String) weather.get(0).get("icon");
                    },
                    Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("01d");
        
        return new DailyForecastDTO(
                date,
                Math.round(minTemp * 10.0) / 10.0, // Arredondar para 1 casa decimal
                Math.round(maxTemp * 10.0) / 10.0,
                Math.round(avgTemp * 10.0) / 10.0,
                Math.round(avgFeelsLike * 10.0) / 10.0, // NOVO: Sensação térmica média
                Math.round(avgHumidity * 10.0) / 10.0,
                Math.round(totalRainfall * 100.0) / 100.0, // Arredondar para 2 casas decimais
                Math.round(avgWindSpeed * 10.0) / 10.0,
                mainDescription,
                mainIcon
        );
    }
    
    private LocalDate extractDate(Map<String, Object> forecastData) {
        long timestamp = ((Number) forecastData.get("dt")).longValue();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()).toLocalDate();
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