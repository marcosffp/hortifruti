package com.hortifruti.sl.hortifruti.service.freight;

import com.hortifruti.sl.hortifruti.dto.freight.FreightCalculationRequest;
import com.hortifruti.sl.hortifruti.dto.freight.FreightConfigDTO;
import com.hortifruti.sl.hortifruti.exception.freight.FreightException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FreightService {

  private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal SIXTY = BigDecimal.valueOf(60);

  private final FreightPropertiesService freightPropertiesService;

  public BigDecimal calculateFreight(FreightCalculationRequest request) {
    FreightConfigDTO freightConfig = freightPropertiesService.getFreightConfig();

    BigDecimal operationalCostPerKm = calculateOperationalCostPerKm(freightConfig);
    BigDecimal costPerMinute = calculateCostPerMinute(freightConfig);
    BigDecimal totalVariableCost =
        calculateTotalVariableCost(request, operationalCostPerKm, costPerMinute);
    BigDecimal finalFreight = applyMarginAndFixedFee(totalVariableCost, freightConfig);

    return finalFreight.setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal calculateOperationalCostPerKm(FreightConfigDTO freightConfig) {
    BigDecimal fuelCostPerKm =
        freightConfig.fuelPrice().divide(freightConfig.kmPerLiterConsumption(), MATH_CONTEXT);

    return fuelCostPerKm
        .add(freightConfig.maintenanceCostPerKm())
        .add(freightConfig.tireCostPerKm())
        .add(freightConfig.depreciationCostPerKm())
        .add(freightConfig.insuranceCostPerKm());
  }

  private BigDecimal calculateCostPerMinute(FreightConfigDTO freightConfig) {
    BigDecimal chargesFactor =
        BigDecimal.ONE.add(freightConfig.chargesPercentage().divide(ONE_HUNDRED, MATH_CONTEXT));
    BigDecimal monthlyDeliveryPersonCost = freightConfig.baseSalary().multiply(chargesFactor);

    BigDecimal hourlyDeliveryPersonCost =
        monthlyDeliveryPersonCost.divide(freightConfig.monthlyHoursWorked(), MATH_CONTEXT);

    BigDecimal administrativeFactor =
        BigDecimal.ONE.add(
            freightConfig.administrativeCostsPercentage().divide(ONE_HUNDRED, MATH_CONTEXT));
    BigDecimal finalHourlyCost = hourlyDeliveryPersonCost.multiply(administrativeFactor);

    return finalHourlyCost.divide(SIXTY, MATH_CONTEXT);
  }

  private BigDecimal calculateTotalVariableCost(
      FreightCalculationRequest request,
      BigDecimal operationalCostPerKm,
      BigDecimal costPerMinute) {
    BigDecimal distanceKm = parseDistance(request.distanceKm());
    int estimatedTimeMinutes = parseEstimatedTime(request.estimatedTimeMinutes());

    return operationalCostPerKm
        .multiply(distanceKm)
        .add(costPerMinute.multiply(BigDecimal.valueOf(estimatedTimeMinutes)));
  }

  private BigDecimal applyMarginAndFixedFee(
      BigDecimal totalVariableCost, FreightConfigDTO freightConfig) {
    BigDecimal margin =
        totalVariableCost.multiply(
            freightConfig.marginPercentage().divide(ONE_HUNDRED, MATH_CONTEXT));
    return totalVariableCost.add(margin).add(freightConfig.fixedFee());
  }

  private BigDecimal parseDistance(String distanceKm) {
    try {
      return new BigDecimal(distanceKm);
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
