package com.hortifruti.sl.hortifruti.rule;

import com.hortifruti.sl.hortifruti.model.climate_model.WeatherSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class RainyColdRule implements DemandRule {
    @Override
    public Set<String> inferDemandTags(List<WeatherSnapshot> window) {
        double avgTemp = window.stream()
                .filter(w -> w.getAvgTemp() != null)
                .mapToDouble(WeatherSnapshot::getAvgTemp)
                .average()
                .orElse(999);
        
        double rainSum = window.stream()
                .mapToDouble(w -> w.getTotalRainMm() == null ? 0 : w.getTotalRainMm())
                .sum();

        if (avgTemp <= 20.0 || rainSum >= 15.0) {
            return Set.of("SOPA", "RAIZ");
        }
        
        return Set.of();
    }

    @Override
    public String getName() {
        return "RainyColdRule";
    }
}
