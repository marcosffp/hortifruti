package com.hortifruti.sl.hortifruti.rule;

import com.hortifruti.sl.hortifruti.config.ClimateRuleConfig;
import com.hortifruti.sl.hortifruti.model.climate_model.WeatherSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ConfigurableClimateRule implements DemandRule {
    
    private final ClimateRuleConfig config;
    
    @Override
    public Set<String> inferDemandTags(List<WeatherSnapshot> window) {
        Set<String> activeTags = new HashSet<>();
        
        if (config.getTags() == null) {
            return activeTags;
        }
        
        // Calcular estatísticas do período
        double avgTemp = calculateAverageTemperature(window);
        double totalRain = calculateTotalRainfall(window);
        
        // Verificar cada tag configurada
        for (var entry : config.getTags().entrySet()) {
            String tag = entry.getKey();
            ClimateRuleConfig.TagConfig tagConfig = entry.getValue();
            
            if (tagConfig.getRules() != null && evaluateRules(tagConfig.getRules(), avgTemp, totalRain)) {
                activeTags.add(tag);
            }
        }
        
        return activeTags;
    }
    
    private boolean evaluateRules(List<ClimateRuleConfig.RuleCondition> rules, double avgTemp, double totalRain) {
        // Se qualquer regra for verdadeira, a tag é ativada (OR logic)
        return rules.stream().anyMatch(rule -> evaluateRule(rule, avgTemp, totalRain));
    }
    
    private boolean evaluateRule(ClimateRuleConfig.RuleCondition rule, double avgTemp, double totalRain) {
        double valueToCheck = switch (rule.getType().toLowerCase()) {
            case "temperature" -> avgTemp;
            case "rainfall" -> totalRain;
            default -> 0.0;
        };
        
        return switch (rule.getCondition().toLowerCase()) {
            case "gte" -> valueToCheck >= rule.getValue();
            case "lte" -> valueToCheck <= rule.getValue();
            case "gt" -> valueToCheck > rule.getValue();
            case "lt" -> valueToCheck < rule.getValue();
            case "eq" -> Math.abs(valueToCheck - rule.getValue()) < 0.01;
            case "between" -> valueToCheck >= rule.getMin() && valueToCheck <= rule.getMax();
            default -> false;
        };
    }
    
    private double calculateAverageTemperature(List<WeatherSnapshot> window) {
        return window.stream()
                .filter(w -> w.getAvgTemp() != null)
                .mapToDouble(WeatherSnapshot::getAvgTemp)
                .average()
                .orElse(0.0);
    }
    
    private double calculateTotalRainfall(List<WeatherSnapshot> window) {
        return window.stream()
                .mapToDouble(w -> w.getTotalRainMm() != null ? w.getTotalRainMm() : 0.0)
                .sum();
    }
    
    @Override
    public String getName() {
        return "ConfigurableClimateRule";
    }
}