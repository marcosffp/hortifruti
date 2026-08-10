package com.hortifruti.sl.hortifruti.dto.climate;

import com.hortifruti.sl.hortifruti.model.climate.Month;
import com.hortifruti.sl.hortifruti.model.climate.TemperatureCategory;
import java.util.List;

public record ProductResponse(
    Long id,
    String name,
    TemperatureCategory temperatureCategory,
    List<Month> peakSalesMonths,
    List<Month> lowSalesMonths,
    String temperatureCategoryDisplay,
    String peakSalesDisplay,
    String lowSalesDisplay) {}
