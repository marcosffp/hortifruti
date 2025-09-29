package com.hortifruti.sl.hortifruti.model.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tags para classificar a qualidade das recomendações de produtos
 * Usado para facilitar o design dos cards no front-end
 */
@Schema(description = "Tag de qualidade da recomendação de produto")
public enum RecommendationTag {
    
    @Schema(description = "Produto altamente recomendado para as condições atuais")
    BOM("Bom", "Produto ideal para as condições atuais"),
    
    @Schema(description = "Produto adequado mas não ideal para as condições atuais")
    MEDIO("Médio", "Produto adequado para as condições atuais"),
    
    @Schema(description = "Produto não recomendado para as condições atuais")
    RUIM("Ruim", "Produto não ideal para as condições atuais");
    
    private final String displayName;
    private final String description;
    
    RecommendationTag(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determina a tag baseada na pontuação da recomendação
     * @param score pontuação da recomendação (0-25)
     * @return tag correspondente
     */
    public static RecommendationTag fromScore(double score) {
        if (score >= 18.0) {
            return BOM;
        } else if (score >= 8.0) {
            return MEDIO;
        } else {
            return RUIM;
        }
    }
}