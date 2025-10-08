package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.ClimateProduct;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para operações com produtos
 */
@Repository
public interface ProductRepository extends JpaRepository<ClimateProduct, Long> {
    
    /**
     * Busca produtos por categoria de temperatura
     */
    List<ClimateProduct> findByTemperatureCategory(TemperatureCategory category);
    
    /**
     * Busca produtos por nome (busca parcial)
     */
    List<ClimateProduct> findByNameContainingIgnoreCase(String name);
    

}