package com.hortifruti.sl.hortifruti.repository.climate_repository;

import com.hortifruti.sl.hortifruti.model.climate_model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByNameIgnoreCase(String name);
}
