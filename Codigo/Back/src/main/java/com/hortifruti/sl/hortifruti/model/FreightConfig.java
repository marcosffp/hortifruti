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

  private double kmPerLiterConsumption;
  private double fuelPrice;
  private double maintenanceCostPerKm;
  private double tireCostPerKm;
  private double depreciationCostPerKm;
  private double insuranceCostPerKm;

  private double baseSalary;
  private double chargesPercentage;
  private double monthlyHoursWorked;
  private double administrativeCostsPercentage;

  private double marginPercentage;
  private double fixedFee;
}
