package com.hortifruti.sl.hortifruti.service.product;

import com.hortifruti.sl.hortifruti.dto.product.FiscalProductResponse;
import com.hortifruti.sl.hortifruti.model.product.FiscalProduct;
import com.hortifruti.sl.hortifruti.repository.product.FiscalProductRepository;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de produtos fiscais cadastrados, usados para conciliação com notas fiscais. */
@Service
@AllArgsConstructor
public class FiscalProductService {

  private final FiscalProductRepository fiscalProductRepository;

  @Transactional(readOnly = true)
  public List<FiscalProductResponse> listAll() {
    return fiscalProductRepository.findAll().stream()
        .sorted(Comparator.comparing(FiscalProduct::getDescription))
        .map(
            product ->
                new FiscalProductResponse(
                    product.getCode(), product.getDescription(), product.getUnidadeComercial()))
        .toList();
  }
}
