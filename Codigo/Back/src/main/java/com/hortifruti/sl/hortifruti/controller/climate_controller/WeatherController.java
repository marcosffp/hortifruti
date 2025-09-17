package com.hortifruti.sl.hortifruti.controller.climate_controller;

import com.hortifruti.sl.hortifruti.exception.WeatherApiException;
import com.hortifruti.sl.hortifruti.model.climate_model.WeatherSnapshot;
import com.hortifruti.sl.hortifruti.repository.climate_repository.WeatherSnapshotRepository;
import com.hortifruti.sl.hortifruti.service.climate_service.WeatherIngestionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {
    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);
    private final WeatherIngestionService service;
    private final WeatherSnapshotRepository repository;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh() {
        try {
            List<WeatherSnapshot> result = service.refreshForecast();
            return ResponseEntity.ok(result);
        } catch (WeatherApiException e) {
            logger.error("Error refreshing weather forecast: {}", e.getMessage(), e);
            return ResponseEntity.status(503).body("Error accessing weather API: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error refreshing weather forecast: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error refreshing weather forecast: " + e.getMessage());
        }
    }

    @GetMapping("/week")
    public ResponseEntity<?> week() {
        try {
            // Previsão para os próximos 5 dias (limitação da API gratuita)
            LocalDate start = LocalDate.now().plusDays(1);
            LocalDate end = LocalDate.now().plusDays(5);
            
            List<WeatherSnapshot> results = repository.findByDateBetween(start, end);
            
            if (results.isEmpty()) {
                // Se não houver dados, atualizar do serviço externo
                service.refreshForecast();
                results = repository.findByDateBetween(start, end);
            }
            
            if (results.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error fetching weather data: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error fetching weather data: " + e.getMessage());
        }
    }
}
