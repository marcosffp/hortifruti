package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.ClimateProduct;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ClimateProduct, Long> {

  List<ClimateProduct> findByTemperatureCategory(TemperatureCategory category);

  List<ClimateProduct> findByNameContainingIgnoreCase(String name);
}
