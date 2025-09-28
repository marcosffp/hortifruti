package com.hortifruti.sl.hortifruti.dto.climate_dto;

import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;


/**
 * DTO para retorno de recomendações de produtos
 */
public record ClimateProductRecommendationDTO(
    Long productId,
    String name,
    TemperatureCategory temperatureCategory,
    double score,
    String recommendationReason
) {
    
}