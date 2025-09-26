package com.hortifruti.sl.hortifruti.service.freight;

import com.hortifruti.sl.hortifruti.dto.freight.FreightCalculationRequest;
import com.hortifruti.sl.hortifruti.dto.freight.FreightConfigDTO;
import com.hortifruti.sl.hortifruti.exception.FreightException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FreightService {

  private final FreightPropertiesService freightPropertiesService;

  protected double calculateFreight(FreightCalculationRequest request) {
    FreightConfigDTO freightConfig = freightPropertiesService.getFreightConfig();

    double operationalCostPerKm = calculateOperationalCostPerKm(freightConfig);
    double costPerMinute = calculateCostPerMinute(freightConfig);
    double totalVariableCost =
        calculateTotalVariableCost(request, operationalCostPerKm, costPerMinute);
    double finalFreight = applyMarginAndFixedFee(totalVariableCost, freightConfig);

    return roundToTwoDecimalPlaces(finalFreight);
  }

  private double calculateOperationalCostPerKm(FreightConfigDTO freightConfig) {
    double fuelCostPerKm = freightConfig.fuelPrice() / freightConfig.kmPerLiterConsumption();

    return fuelCostPerKm
        + freightConfig.maintenanceCostPerKm()
        + freightConfig.tireCostPerKm()
        + freightConfig.depreciationCostPerKm()
        + freightConfig.insuranceCostPerKm();
  }

  private double calculateCostPerMinute(FreightConfigDTO freightConfig) {
    double monthlyDeliveryPersonCost =
        freightConfig.baseSalary() * (1 + freightConfig.chargesPercentage() / 100);

    double hourlyDeliveryPersonCost =
        monthlyDeliveryPersonCost / freightConfig.monthlyHoursWorked();

    double finalHourlyCost =
        hourlyDeliveryPersonCost * (1 + freightConfig.administrativeCostsPercentage() / 100);

    return finalHourlyCost / 60;
  }

  private double calculateTotalVariableCost(
      FreightCalculationRequest request, double operationalCostPerKm, double costPerMinute) {
    double distanceKm = parseDistance(request.distanceKm());
    int estimatedTimeMinutes = parseEstimatedTime(request.estimatedTimeMinutes());

    return (operationalCostPerKm * distanceKm) + (costPerMinute * estimatedTimeMinutes);
  }

  private double applyMarginAndFixedFee(double totalVariableCost, FreightConfigDTO freightConfig) {
    double margin = totalVariableCost * (freightConfig.marginPercentage() / 100);
    return totalVariableCost + margin + freightConfig.fixedFee();
  }

  private double roundToTwoDecimalPlaces(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private double parseDistance(String distanceKm) {
    try {
      return Double.parseDouble(distanceKm);
    } catch (NumberFormatException e) {
      throw new FreightException("Formato inválido para distância: " + distanceKm);
    }
  }

  private int parseEstimatedTime(String estimatedTimeMinutes) {
    try {
      return Integer.parseInt(estimatedTimeMinutes.replaceAll("[^0-9]", ""));
    } catch (NumberFormatException e) {
      throw new FreightException("Formato inválido para tempo estimado: " + estimatedTimeMinutes);
    }
  }
}
