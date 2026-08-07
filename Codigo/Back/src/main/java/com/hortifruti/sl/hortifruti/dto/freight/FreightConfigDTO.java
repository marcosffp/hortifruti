package com.hortifruti.sl.hortifruti.dto.freight;

import java.math.BigDecimal;

public record FreightConfigDTO(
    BigDecimal kmPerLiterConsumption,
    BigDecimal fuelPrice,
    BigDecimal maintenanceCostPerKm,
    BigDecimal tireCostPerKm,
    BigDecimal depreciationCostPerKm,
    BigDecimal insuranceCostPerKm,
    BigDecimal baseSalary,
    BigDecimal chargesPercentage,
    BigDecimal monthlyHoursWorked,
    BigDecimal administrativeCostsPercentage,
    BigDecimal marginPercentage,
    BigDecimal fixedFee) {}
