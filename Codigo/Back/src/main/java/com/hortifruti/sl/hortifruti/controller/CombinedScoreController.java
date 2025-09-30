package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.mapper.CombinedScoreMapper;
import com.hortifruti.sl.hortifruti.model.CombinedScore;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/grouped-products")
public class CombinedScoreController {

  private final CombinedScoreService groupedProductsService;
  private final CombinedScoreMapper groupedProductsMapper;

  @PostMapping("/confirm")
  public ResponseEntity<CombinedScoreResponse> confirmGrouping(
      @Valid @RequestBody CombinedScoreRequest request) {
    CombinedScore groupedProducts = groupedProductsService.confirmGrouping(request);
    return ResponseEntity.ok(groupedProductsMapper.toResponse(groupedProducts));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> cancelGrouping(@PathVariable Long id) {
    groupedProductsService.cancelGrouping(id);
    return ResponseEntity.ok().body("Agrupamento cancelado com sucesso.");
  }

  @GetMapping
  public ResponseEntity<Page<CombinedScoreResponse>> listGroupings(
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Page<CombinedScore> groupings = groupedProductsService.listGroupings(search, page, size);
    Page<CombinedScoreResponse> response = groupings.map(groupedProductsMapper::toResponse);
    return ResponseEntity.ok(response);
  }
}
