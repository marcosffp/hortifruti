package com.hortifruti.sl.hortifruti.controller.climate;

import com.hortifruti.sl.hortifruti.dto.climate.WeatherForecastDTO;
import com.hortifruti.sl.hortifruti.exception.WeatherApiException;
import com.hortifruti.sl.hortifruti.service.climate.WeatherForecastService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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
      log.info("Solicitando previsão do tempo para 5 dias");
      WeatherForecastDTO forecast = weatherForecastService.getFiveDayForecast();
      log.info("Previsão obtida com sucesso para cidade: {}", forecast.city());
      return ResponseEntity.ok(forecast);
    } catch (WeatherApiException e) {
      log.error("Erro ao obter previsão do tempo: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    } catch (Exception e) {
      log.error("Erro interno ao processar previsão do tempo: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
