package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal kmPerLiterConsumption;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal fuelPrice;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal maintenanceCostPerKm;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal tireCostPerKm;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal depreciationCostPerKm;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal insuranceCostPerKm;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal baseSalary;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal chargesPercentage;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal monthlyHoursWorked;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal administrativeCostsPercentage;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal marginPercentage;

  @NotNull
  @Column(nullable = false, precision = 12, scale = 4)
  private BigDecimal fixedFee;
}
