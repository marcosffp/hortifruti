package com.hortifruti.sl.hortifruti.repository.product;

import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalProductRepository extends JpaRepository<FiscalProduct, Long> {

  Optional<FiscalProduct> findByCode(String code);
}
