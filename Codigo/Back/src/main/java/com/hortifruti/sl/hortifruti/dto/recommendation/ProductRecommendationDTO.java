package com.hortifruti.sl.hortifruti.dto.recommendation;

import com.hortifruti.sl.hortifruti.model.climate_model.TemperatureCategory;


/**
 * DTO para retorno de recomendações de produtos
 */
public record ProductRecommendationDTO(
    Long productId,
    String name,
    TemperatureCategory temperatureCategory,
    double score,
    String recommendationReason
) {
    
}