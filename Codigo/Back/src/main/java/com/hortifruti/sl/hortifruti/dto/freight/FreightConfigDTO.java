package com.hortifruti.sl.hortifruti.dto.freight;

public record FreightConfigDTO(
    Double kmPerLiterConsumption,
    Double fuelPrice,
    Double maintenanceCostPerKm,
    Double tireCostPerKm,
    Double depreciationCostPerKm,
    Double insuranceCostPerKm,
    // Delivery Person Configurations
    Double baseSalary,
    Double chargesPercentage,
    Double monthlyHoursWorked,
    Double administrativeCostsPercentage,
    // Margin Configurations
    Double marginPercentage,
    Double fixedFee) {}
