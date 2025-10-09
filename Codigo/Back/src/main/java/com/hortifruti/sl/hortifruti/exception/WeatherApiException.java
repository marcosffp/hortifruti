package com.hortifruti.sl.hortifruti.exception;

/**
 * Exceção específica para erros na API do OpenWeather
 */
public class WeatherApiException extends Exception {
    
    public WeatherApiException(String message) {
        super(message);
    }
    
    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
