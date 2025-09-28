package com.hortifruti.sl.hortifruti.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "freight")
public class FreightProperties {
  private VehicleConfig vehicle;
  private DeliveryPersonConfig deliveryPerson;
  private MarginConfig margin;

  @Getter
  @Setter
  public static class VehicleConfig {
    private double kmPerLiterConsumption;
    private double fuelPrice;
    private double maintenanceCostPerKm;
    private double tireCostPerKm;
    private double depreciationCostPerKm;
    private double insuranceCostPerKm;
  }

  @Getter
  @Setter
  public static class DeliveryPersonConfig {
    private double baseSalary;
    private double chargesPercentage;
    private double monthlyHoursWorked;
    private double administrativeCostsPercentage;
  }

  @Getter
  @Setter
  public static class MarginConfig {
    private double marginPercentage;
    private double fixedFee;
  }
}
