package com.hortifruti.sl.hortifruti.dto.climate_dto;

import java.util.List;

/**
 * DTO para resposta categorizada das recomendações
 * Contém as melhores, médias e piores recomendações organizadas
 */
public record CategorizedRecommendationsDTO(
        List<RecommendationDTO> highPriority,
        List<RecommendationDTO> mediumPriority,
        List<RecommendationDTO> lowPriority,
        RecommendationSummaryDTO summary
) {
    
    /**
     * Resumo estatístico das recomendações
     */
    public record RecommendationSummaryDTO(
            int totalRecommendations,
            int highPriorityCount,
            int mediumPriorityCount,
            int lowPriorityCount
    ) {}
}