package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "freight_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreightConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Vehicle Configurations
  private double kmPerLiterConsumption;
  private double fuelPrice;
  private double maintenanceCostPerKm;
  private double tireCostPerKm;
  private double depreciationCostPerKm;
  private double insuranceCostPerKm;

  // Delivery Person Configurations
  private double baseSalary;
  private double chargesPercentage;
  private double monthlyHoursWorked;
  private double administrativeCostsPercentage;

  // Margin Configurations
  private double marginPercentage;
  private double fixedFee;
}
