package com.hortifruti.sl.hortifruti.model;

import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.util.MonthListConverter;
import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClimateProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

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
