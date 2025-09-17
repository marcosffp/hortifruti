package com.hortifruti.sl.hortifruti.rule;

import com.hortifruti.sl.hortifruti.model.climate_model.WeatherSnapshot;

import java.util.List;
import java.util.Set;

public interface DemandRule {
    /**
     * Retorna tags de demanda esperada (ex.: "REFRESCANTE", "SOPA") para um intervalo de previsão.
     */
    Set<String> inferDemandTags(List<WeatherSnapshot> window);
    
    String getName();
}
