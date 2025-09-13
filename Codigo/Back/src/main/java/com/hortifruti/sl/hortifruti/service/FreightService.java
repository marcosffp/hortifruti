package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.config.FreightProperties;
import com.hortifruti.sl.hortifruti.dto.FreightCalculationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FreightService {

  private final FreightProperties freightProperties;

  public double calculateFreight(FreightCalculationRequest request) {
    // 1. Calculate operational cost per km
    double fuelCostPerKm =
        freightProperties.getVehicle().getFuelPrice()
            / freightProperties.getVehicle().getKmPerLiterConsumption();

    double operationalCostPerKm =
        fuelCostPerKm
            + freightProperties.getVehicle().getMaintenanceCostPerKm()
            + freightProperties.getVehicle().getTireCostPerKm()
            + freightProperties.getVehicle().getDepreciationCostPerKm()
            + freightProperties.getVehicle().getInsuranceCostPerKm();

    // 2. Calculate delivery person cost per minute
    double monthlyDeliveryPersonCost =
        freightProperties.getDeliveryPerson().getBaseSalary()
            * (1 + freightProperties.getDeliveryPerson().getChargesPercentage() / 100);

    double hourlyDeliveryPersonCost =
        monthlyDeliveryPersonCost / freightProperties.getDeliveryPerson().getMonthlyHoursWorked();

    double finalHourlyCost =
        hourlyDeliveryPersonCost
            * (1 + freightProperties.getDeliveryPerson().getAdministrativeCostsPercentage() / 100);

    double costPerMinute = finalHourlyCost / 60;

    // 3. Calculate total variable cost
    double distanceKm = Double.parseDouble(request.distanceKm());
    int estimatedTimeMinutes =
        Integer.parseInt(request.estimatedTimeMinutes().replaceAll("[^0-9]", ""));
    double totalVariableCost =
        (operationalCostPerKm * distanceKm) + (costPerMinute * estimatedTimeMinutes);

    // 4. Apply margin and fixed fee
    double margin = totalVariableCost * (freightProperties.getMargin().getMarginPercentage() / 100);

    double finalFreight = totalVariableCost + margin + freightProperties.getMargin().getFixedFee();

    return Math.round(finalFreight * 100.0) / 100.0; // Round to 2 decimal places
  }
}
