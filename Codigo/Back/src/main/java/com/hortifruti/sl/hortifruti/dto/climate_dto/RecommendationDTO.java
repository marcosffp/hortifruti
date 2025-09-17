package com.hortifruti.sl.hortifruti.dto.climate_dto;

import com.hortifruti.sl.hortifruti.model.climate_model.RecommendationLevel;
import java.util.Set;

/**
 * DTO para recomendação de produto
 * @param product Nome do produto
 * @param reason Motivo da recomendação (clima e temporada)
 * @param action Ação sugerida (ex: "Aumentar estoque")
 * @param tags Tags do produto relacionadas ao clima
 * @param level Nível de prioridade (HIGH/MEDIUM/LOW)
 * @param score Score interno para ordenação
 */
public record RecommendationDTO(
        String product,
        String reason,
        String action,
        Set<String> tags,
        RecommendationLevel level,
        Integer score
) {}
