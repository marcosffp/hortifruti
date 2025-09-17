package com.hortifruti.sl.hortifruti.rule;

import com.hortifruti.sl.hortifruti.model.climate_model.WeatherSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class HotWeatherRule implements DemandRule {
    @Override
    public Set<String> inferDemandTags(List<WeatherSnapshot> window) {
        double avg = window.stream()
                .filter(w -> w.getAvgTemp() != null)
                .mapToDouble(WeatherSnapshot::getAvgTemp)
                .average()
                .orElse(0);
        
        if (avg >= 28.0) {
            return Set.of("REFRESCANTE");
        }
        
        return Set.of();
    }

    @Override
    public String getName() {
        return "HotWeatherRule";
    }
}
