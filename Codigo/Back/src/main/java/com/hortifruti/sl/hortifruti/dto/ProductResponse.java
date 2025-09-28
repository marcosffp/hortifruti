package com.hortifruti.sl.hortifruti.dto;

import java.util.List;

import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;

/**
 * DTO para resposta de produtos
 */
public record ProductResponse(
    Long id,
    String name,
    TemperatureCategory temperatureCategory,
    List<Month> peakSalesMonths,
    List<Month> lowSalesMonths,
    String temperatureCategoryDisplay,
    String peakSalesDisplay,
    String lowSalesDisplay
) {
    
    /**
     * Construtor que calcula automaticamente os displays
     */
    public ProductResponse(Long id, String name, TemperatureCategory temperatureCategory, 
                          List<Month> peakSalesMonths, List<Month> lowSalesMonths) {
        this(
            id, 
            name, 
            temperatureCategory, 
            peakSalesMonths, 
            lowSalesMonths,
            temperatureCategory.getDisplayName(),
            formatMonthsList(peakSalesMonths),
            formatMonthsList(lowSalesMonths)
        );
    }
    
    private static String formatMonthsList(List<Month> months) {
        if (months == null || months.isEmpty()) {
            return "Nenhum";
        }
        return months.stream()
                .map(Month::getDisplayName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Nenhum");
    }
}