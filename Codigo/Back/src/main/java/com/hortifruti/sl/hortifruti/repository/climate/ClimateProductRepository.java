package com.hortifruti.sl.hortifruti.repository.climate;

import com.hortifruti.sl.hortifruti.model.climate.ClimateProduct;
import com.hortifruti.sl.hortifruti.model.climate.TemperatureCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClimateProductRepository extends JpaRepository<ClimateProduct, Long> {

  List<ClimateProduct> findByTemperatureCategory(TemperatureCategory category);

  List<ClimateProduct> findByNameContainingIgnoreCase(String name);
}
