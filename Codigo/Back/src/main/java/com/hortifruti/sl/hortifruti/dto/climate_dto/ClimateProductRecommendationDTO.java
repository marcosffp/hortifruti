package com.hortifruti.sl.hortifruti.dto.climate_dto;

import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.model.enumeration.RecommendationTag;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para retorno de recomendações de produtos baseadas no clima
 */
@Schema(description = "Recomendação de produto baseada em clima e sazonalidade")
public record ClimateProductRecommendationDTO(
    
    @Schema(description = "ID do produto", example = "1")
    Long productId,
    
    @Schema(description = "Nome do produto", example = "Tomate")
    String name,
    
    @Schema(description = "Categoria de temperatura ideal do produto")
    TemperatureCategory temperatureCategory,
    
    @Schema(description = "Pontuação da recomendação (0-25)", example = "18.5")
    double score,
    
    @Schema(description = "Tag de qualidade da recomendação")
    RecommendationTag tag
) {
    
}