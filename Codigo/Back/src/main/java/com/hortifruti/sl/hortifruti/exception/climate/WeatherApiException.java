package com.hortifruti.sl.hortifruti.exception.climate;

public class WeatherApiException extends Exception {

  public WeatherApiException(String message) {
    super(message);
  }

  public WeatherApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
