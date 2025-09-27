package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.Product;
import com.hortifruti.sl.hortifruti.model.climate_model.TemperatureCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para operações com produtos
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * Busca produtos por categoria de temperatura
     */
    List<Product> findByTemperatureCategory(TemperatureCategory category);
    

    
    /**
     * Busca produtos por nome (busca parcial)
     */
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);
    

}