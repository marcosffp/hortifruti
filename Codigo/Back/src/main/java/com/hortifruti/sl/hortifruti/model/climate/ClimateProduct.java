package com.hortifruti.sl.hortifruti.model.climate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClimateProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false, length = 100)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "temperature_category", nullable = false)
  private TemperatureCategory temperatureCategory;

  @Convert(converter = MonthListConverter.class)
  @Column(name = "peak_sales_months", length = 500)
  private List<Month> peakSalesMonths;

  @Convert(converter = MonthListConverter.class)
  @Column(name = "low_sales_months", length = 500)
  private List<Month> lowSalesMonths;

  public ClimateProduct(
      String name,
      TemperatureCategory temperatureCategory,
      List<Month> peakSalesMonths,
      List<Month> lowSalesMonths) {
    this.name = name;
    this.temperatureCategory = temperatureCategory;
    this.peakSalesMonths = peakSalesMonths;
    this.lowSalesMonths = lowSalesMonths;
  }
}
