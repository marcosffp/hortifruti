package com.hortifruti.sl.hortifruti.controller.climate;

import com.hortifruti.sl.hortifruti.dto.climate.WeatherForecastDTO;
import com.hortifruti.sl.hortifruti.exception.WeatherApiException;
import com.hortifruti.sl.hortifruti.service.climate.WeatherForecastService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherForecastController {

  private final WeatherForecastService weatherForecastService;

  @GetMapping("/forecast/5days")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Previsão obtida com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor"),
        @ApiResponse(
            responseCode = "503",
            description = "Serviço de previsão do tempo indisponível")
      })
  public ResponseEntity<WeatherForecastDTO> getFiveDayForecast() {
    try {
      WeatherForecastDTO forecast = weatherForecastService.getFiveDayForecast();
      return ResponseEntity.ok(forecast);
    } catch (WeatherApiException e) {
      return ResponseEntity.internalServerError().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
