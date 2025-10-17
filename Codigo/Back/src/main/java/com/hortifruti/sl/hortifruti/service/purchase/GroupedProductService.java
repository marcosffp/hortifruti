package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.UpdateGroupedProduct;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.purchase.GroupedProduct;
import com.hortifruti.sl.hortifruti.repository.purchase.ProductGrouperRepository;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GroupedProductService {

  private final ProductGrouperRepository repository;

  /** Atualiza um produto agrupado pelo ID. */
  public GroupedProductResponse updateGroupedProduct(Long id, UpdateGroupedProduct dto) {
    GroupedProduct groupedProduct =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "Produto agrupado com o ID " + id + " não encontrado."));

    groupedProduct.setName(dto.name());
    groupedProduct.setPrice(dto.price());
    groupedProduct.setQuantity(dto.quantity());
    groupedProduct.setTotalValue(dto.price().multiply(BigDecimal.valueOf(dto.quantity())));

    repository.save(groupedProduct);

    return new GroupedProductResponse(
        groupedProduct.getCode(),
        groupedProduct.getName(),
        groupedProduct.getPrice(),
        groupedProduct.getQuantity(),
        groupedProduct.getTotalValue());
  }

  /** Deleta um produto agrupado pelo ID. */
  public void deleteGroupedProduct(Long id) {
    if (!repository.existsById(id)) {
      throw new CombinedScoreException("Produto agrupado com o ID " + id + " não encontrado.");
    }
    repository.deleteById(id);
  }
}
