package com.hortifruti.sl.hortifruti.model.climate_model;

/**
 * Níveis de prioridade das recomendações
 */
public enum RecommendationLevel {
    HIGH("Alta", "Aumentar compra significativamente"),
    MEDIUM("Média", "Ajustar estoque moderadamente"),
    LOW("Baixa", "Diminuir compra ou manter estoque mínimo");
    
    private final String displayName;
    private final String description;
    
    RecommendationLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}