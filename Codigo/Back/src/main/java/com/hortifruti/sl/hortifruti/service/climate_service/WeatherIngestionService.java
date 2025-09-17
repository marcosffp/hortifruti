package com.hortifruti.sl.hortifruti.service.climate_service;

import com.hortifruti.sl.client.OpenWeatherClient;
import com.hortifruti.sl.hortifruti.exception.WeatherApiException;
import com.hortifruti.sl.hortifruti.model.climate_model.WeatherSnapshot;
import com.hortifruti.sl.hortifruti.repository.climate_repository.WeatherSnapshotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeatherIngestionService {
    private final OpenWeatherClient client;
    private final WeatherSnapshotRepository repository;

    @Transactional
    public List<WeatherSnapshot> refreshForecast() throws WeatherApiException {
        Map<String, Object> raw = client.fetch5DayForecast();
        List<Map<String, Object>> list = (List<Map<String, Object>>) raw.get("list");

        // Agrupar por data (yyyy-MM-dd)
        Map<LocalDate, List<Map<String, Object>>> byDay = list.stream()
                .collect(Collectors.groupingBy(
                        it -> LocalDate.parse(((String) it.get("dt_txt")).substring(0, 10))
                ));

        List<WeatherSnapshot> results = new ArrayList<>();
        
        for (Map.Entry<LocalDate, List<Map<String, Object>>> entry : byDay.entrySet()) {
            LocalDate date = entry.getKey();
            List<Map<String, Object>> blocks = entry.getValue();

            double avgTemp = blocks.stream()
                    .map(b -> (Map<String, Object>) b.get("main"))
                    .mapToDouble(m -> ((Number) m.get("temp")).doubleValue())
                    .average().orElse(Double.NaN);

            double rain = blocks.stream()
                    .map(b -> (Map<String, Object>) b.get("rain"))
                    .filter(Objects::nonNull)
                    .mapToDouble(r -> ((Number) r.getOrDefault("3h", 0)).doubleValue())
                    .sum();

            WeatherSnapshot snapshot = repository.findByDate(date).orElseGet(WeatherSnapshot::new);
            snapshot.setDate(date);
            snapshot.setAvgTemp(Double.isNaN(avgTemp) ? null : avgTemp);
            snapshot.setTotalRainMm(rain);
            
            snapshot = repository.save(snapshot);
            results.add(snapshot);
        }
        
        return results;
    }
}
