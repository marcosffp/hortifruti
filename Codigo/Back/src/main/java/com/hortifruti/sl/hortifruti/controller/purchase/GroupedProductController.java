package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.UpdateGroupedProduct;
import com.hortifruti.sl.hortifruti.service.purchase.GroupedProductService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/grouped-products")
@AllArgsConstructor
public class GroupedProductController {

  private final GroupedProductService service;

  /** Atualiza um produto agrupado pelo ID. */
  @PutMapping("/{id}")
  public ResponseEntity<GroupedProductResponse> updateGroupedProduct(
      @PathVariable Long id, @RequestBody UpdateGroupedProduct dto) {
    GroupedProductResponse updated = service.updateGroupedProduct(id, dto);
    return ResponseEntity.ok(updated);
  }

  /** Deleta um produto agrupado pelo ID. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteGroupedProduct(@PathVariable Long id) {
    service.deleteGroupedProduct(id);
    return ResponseEntity.noContent().build();
  }
}
