package com.hortifruti.sl.hortifruti.service.climate_service;

import com.hortifruti.sl.hortifruti.config.ClimateRuleConfig;
import com.hortifruti.sl.hortifruti.dto.climate_dto.CategorizedRecommendationsDTO;
import com.hortifruti.sl.hortifruti.dto.climate_dto.RecommendationDTO;
import com.hortifruti.sl.hortifruti.model.climate_model.Month;
import com.hortifruti.sl.hortifruti.model.climate_model.Product;
import com.hortifruti.sl.hortifruti.model.climate_model.RecommendationLevel;
import com.hortifruti.sl.hortifruti.model.climate_model.WeatherSnapshot;
import com.hortifruti.sl.hortifruti.repository.climate_repository.ProductRepository;
import com.hortifruti.sl.hortifruti.repository.climate_repository.WeatherSnapshotRepository;
import com.hortifruti.sl.hortifruti.rule.DemandRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final WeatherSnapshotRepository weatherRepository;
    private final ProductRepository productRepository;
    private final List<DemandRule> rules;
    private final ClimateRuleConfig climateConfig;
    
    // Constante para ações
    private static final String ACTION_INCREASE = "Aumentar estoque";
    private static final String ACTION_MONITOR = "Monitorar demanda";
    private static final String ACTION_MAINTAIN = "Manter estoque atual";
    
    // Constantes para padrões de formatação
    private static final String REASON_FORMAT = "Clima: %.1f°C, chuva=%.0fmm; temporada=%s";
    
    // Constantes para parâmetros de cálculo
    private static final int SEASON_BONUS = 8;  // Bônus por estar na temporada

    public CategorizedRecommendationsDTO getCategorizedRecommendations() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.plusDays(1);
        LocalDate end = today.plusDays(7);
        
        List<RecommendationDTO> allRecommendations = recommendForPeriod(start, end);
        
        return categorizeRecommendations(allRecommendations);
    }
    
    public List<RecommendationDTO> recommendForPeriod(LocalDate start, LocalDate end) {
        List<WeatherSnapshot> period = weatherRepository.findByDateBetween(start, end);

        // 1) Obter tags de demanda a partir das regras (baseado no clima)
        Set<String> demandTags = rules.stream()
                .flatMap(r -> r.inferDemandTags(period).stream())
                .collect(Collectors.toSet());

        // 2) Buscar produtos com tags compatíveis com o clima
        List<Product> candidates = productRepository.findAll().stream()
                .filter(p -> !Collections.disjoint(p.getTags(), demandTags))
                .toList();

        List<RecommendationDTO> recommendations = new ArrayList<>();
        
        double avgTemp = calculateAverageTemperature(period);
        double rainSum = calculateTotalRainfall(period);

        for (Product product : candidates) {
            int quantity = calculateRecommendedQuantityWithSeason(product, demandTags);
            boolean inSeason = Month.isInSeason(product.getSeasons());
            int score = calculateRecommendationScore(quantity, inSeason);
            RecommendationLevel level = determineRecommendationLevel(score);
            String action = determineAction(level);
            
            String seasonInfo = inSeason ? "Na temporada" : "Fora de temporada";
            String reason = String.format(
                    REASON_FORMAT, avgTemp, rainSum, seasonInfo
            );

            // Filtrar apenas as tags que são relevantes para o clima atual
            Set<String> relevantTags = product.getTags().stream()
                    .filter(demandTags::contains)
                    .collect(Collectors.toSet());

            recommendations.add(new RecommendationDTO(
                    product.getName(), reason, action, relevantTags, level, score
            ));
        }
        
        // Ordenar por score desc
        recommendations.sort(Comparator.comparing(RecommendationDTO::score).reversed());
        
        return recommendations;
    }
    
    public RecommendationDTO recommendForProduct(Long productId) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.plusDays(1);
        LocalDate end = today.plusDays(7);
        
        List<WeatherSnapshot> week = weatherRepository.findByDateBetween(start, end);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        
        // Obter tags de demanda a partir das regras (baseado no clima)
        Set<String> demandTags = rules.stream()
                .flatMap(r -> r.inferDemandTags(week).stream())
                .collect(Collectors.toSet());
        
        // Verificar se o produto tem tags compatíveis
        boolean isCompatible = !Collections.disjoint(product.getTags(), demandTags);
        
        double avgTemp = calculateAverageTemperature(week);
        double rainSum = calculateTotalRainfall(week);   
        
        // Calcular sugestão baseada no clima e temporada
        int quantity = 0;
        String action = ACTION_INCREASE;
        int score = 0;
        RecommendationLevel level = RecommendationLevel.LOW;
        
        if (isCompatible) {
            quantity = calculateRecommendedQuantityWithSeason(product, demandTags);
            boolean inSeason = Month.isInSeason(product.getSeasons());
            score = calculateRecommendationScore(quantity, inSeason);
            level = determineRecommendationLevel(score);
            action = determineAction(level);
        }
        
        boolean inSeason = Month.isInSeason(product.getSeasons());
        String seasonInfo = inSeason ? "Na temporada" : "Fora de temporada";
        String reason = String.format(
                REASON_FORMAT, avgTemp, rainSum, seasonInfo
        );
        
        // Filtrar apenas as tags que são relevantes para o clima atual
        Set<String> relevantTags = product.getTags().stream()
                .filter(demandTags::contains)
                .collect(Collectors.toSet());
        
        return new RecommendationDTO(
                product.getName(), reason, action, relevantTags, level, score
        );
    }
    
    // Métodos auxiliares para melhorar a legibilidade e manutenção
    
    private double calculateAverageTemperature(List<WeatherSnapshot> weatherData) {
        return weatherData.stream()
                .filter(w -> w.getAvgTemp() != null)
                .mapToDouble(WeatherSnapshot::getAvgTemp)
                .average()
                .orElse(Double.NaN);
    }
    
    private double calculateTotalRainfall(List<WeatherSnapshot> weatherData) {
        return weatherData.stream()
                .mapToDouble(w -> Optional.ofNullable(w.getTotalRainMm()).orElse(0.0))
                .sum();
    }
    
    /**
     * Calcula a quantidade recomendada baseada no clima e temporada do produto
     */
    private int calculateRecommendedQuantityWithSeason(Product product, Set<String> demandTags) {
        int suggestion = calculateQuantityBasedOnClimate(product, demandTags);
        
        // Bônus se o produto está na temporada
        if (Month.isInSeason(product.getSeasons())) {
            suggestion += SEASON_BONUS;
        }
        
        return Math.max(0, suggestion);
    }
    
    /**
     * Calcula score da recomendação baseado na quantidade sugerida e temporada
     */
    private int calculateRecommendationScore(int suggestedExtra, boolean inSeason) {
        int score = suggestedExtra;
        
        // Bônus adicional para temporada no score
        if (inSeason) {
            score += 10;
        }
        
        return score;
    }
    
    /**
     * Determina o nível da recomendação baseado no score
     */
    private RecommendationLevel determineRecommendationLevel(int score) {
        if (score >= 25) {
            return RecommendationLevel.HIGH;
        } else if (score >= 15) {
            return RecommendationLevel.MEDIUM;
        } else {
            return RecommendationLevel.LOW;
        }
    }
    
    /**
     * Determina a ação baseada no nível da recomendação
     */
    private String determineAction(RecommendationLevel level) {
        return switch (level) {
            case HIGH -> ACTION_INCREASE;
            case MEDIUM -> ACTION_MONITOR;
            case LOW -> ACTION_MAINTAIN;
        };
    }
    
    /**
     * Calcula a quantidade recomendada baseada apenas no clima e tags do produto
     */
    private int calculateQuantityBasedOnClimate(Product product, Set<String> demandTags) {
        int suggestion = 0;
        
        // Usar configuração para calcular sugestões
        if (climateConfig.getTags() != null) {
            for (String tag : demandTags) {
                if (product.getTags().contains(tag)) {
                    ClimateRuleConfig.TagConfig tagConfig = climateConfig.getTags().get(tag);
                    if (tagConfig != null) {
                        suggestion += tagConfig.getBaseValue();
                    }
                }
            }
        }
        
        return Math.max(0, suggestion);
    }
    
    /**
     * Categoriza as recomendações por nível de prioridade
     * Seleciona as melhores de cada categoria para exibir
     */
    private CategorizedRecommendationsDTO categorizeRecommendations(List<RecommendationDTO> recommendations) {
        // Agrupar por nível
        Map<RecommendationLevel, List<RecommendationDTO>> grouped = recommendations.stream()
                .collect(Collectors.groupingBy(RecommendationDTO::level));
        
        // Selecionar as melhores de cada nível (ordenadas por score)
        List<RecommendationDTO> highPriority = selectTopRecommendations(
                grouped.getOrDefault(RecommendationLevel.HIGH, Collections.emptyList()), 3);
        List<RecommendationDTO> mediumPriority = selectTopRecommendations(
                grouped.getOrDefault(RecommendationLevel.MEDIUM, Collections.emptyList()), 2);
        List<RecommendationDTO> lowPriority = selectTopRecommendations(
                grouped.getOrDefault(RecommendationLevel.LOW, Collections.emptyList()), 2);
        
        // Calcular resumo
        CategorizedRecommendationsDTO.RecommendationSummaryDTO summary = 
                new CategorizedRecommendationsDTO.RecommendationSummaryDTO(
                        recommendations.size(),
                        grouped.getOrDefault(RecommendationLevel.HIGH, Collections.emptyList()).size(),
                        grouped.getOrDefault(RecommendationLevel.MEDIUM, Collections.emptyList()).size(),
                        grouped.getOrDefault(RecommendationLevel.LOW, Collections.emptyList()).size()
                );
        
        return new CategorizedRecommendationsDTO(highPriority, mediumPriority, lowPriority, summary);
    }
    
    /**
     * Seleciona as top N recomendações ordenadas por score (descendente)
     */
    private List<RecommendationDTO> selectTopRecommendations(List<RecommendationDTO> recommendations, int count) {
        return recommendations.stream()
                .sorted(Comparator.comparing(RecommendationDTO::score).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }
}
