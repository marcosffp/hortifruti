package com.hortifruti.sl.hortifruti.dto.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * DTO para representar dados climáticos de um dia específico
 * Usado para passar dados entre front-end e back-end quando um card de clima é clicado
 */
@Schema(description = "Dados climáticos de um dia específico para recomendações de produtos")
public record DayClimateDataDTO(
    
    @Schema(description = "Data do dia", example = "2025-09-27")
    LocalDate date,
    
    @Schema(description = "Temperatura mínima em Celsius", example = "18.5")
    double minTemp,
    
    @Schema(description = "Temperatura máxima em Celsius", example = "26.3")
    double maxTemp,
    
    @Schema(description = "Temperatura média em Celsius", example = "22.4")
    double avgTemp,
    
    @Schema(description = "Umidade relativa em porcentagem", example = "65.0")
    double humidity,
    
    @Schema(description = "Precipitação em mm", example = "2.5")
    double rainfall,
    
    @Schema(description = "Velocidade do vento em km/h", example = "12.8")
    double windSpeed,
    
    @Schema(description = "Descrição do clima", example = "Parcialmente nublado")
    String weatherDescription,
    
    @Schema(description = "Ícone do clima", example = "02d")
    String weatherIcon
) {
    
    /**
     * Construtor de conveniência para criar a partir do DailyForecastDTO existente
     */
    public static DayClimateDataDTO fromDailyForecast(
            com.hortifruti.sl.hortifruti.dto.climate_dto.WeatherForecastDTO.DailyForecastDTO dailyForecast) {
        return new DayClimateDataDTO(
            dailyForecast.date(),
            dailyForecast.minTemp(),
            dailyForecast.maxTemp(),
            dailyForecast.avgTemp(),
            dailyForecast.humidity(),
            dailyForecast.rainfall(),
            dailyForecast.windSpeed(),
            dailyForecast.weatherDescription(),
            dailyForecast.weatherIcon()
        );
    }
    
    /**
     * Construtor simplificado com apenas dados essenciais
     */
    public DayClimateDataDTO(LocalDate date, double avgTemp, double minTemp, double maxTemp) {
        this(date, minTemp, maxTemp, avgTemp, 0.0, 0.0, 0.0, "N/A", "01d");
    }
}