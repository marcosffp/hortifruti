package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
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

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal kmPerLiterConsumption;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal fuelPrice;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal maintenanceCostPerKm;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal tireCostPerKm;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal depreciationCostPerKm;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal insuranceCostPerKm;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal baseSalary;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal chargesPercentage;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal monthlyHoursWorked;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal administrativeCostsPercentage;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal marginPercentage;

  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal fixedFee;
}
