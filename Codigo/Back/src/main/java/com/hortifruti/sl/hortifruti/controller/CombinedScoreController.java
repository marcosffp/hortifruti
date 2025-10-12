package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@AllArgsConstructor
@RequestMapping("/grouped-products")
public class CombinedScoreController {

  private final CombinedScoreService combinedScoreService;

  @PostMapping("/confirm")
  public ResponseEntity<CombinedScoreResponse> confirmGrouping(
      @Valid @RequestBody CombinedScoreRequest request) {
    CombinedScoreResponse response = combinedScoreService.confirmGrouping(request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> cancelGrouping(@PathVariable Long id) {
    combinedScoreService.cancelGrouping(id);
    return ResponseEntity.ok().body("Agrupamento cancelado com sucesso.");
  }

  @GetMapping
  public ResponseEntity<Page<CombinedScoreResponse>> listGroupings(
      @RequestParam(required = false) Long clientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<CombinedScoreResponse> response = combinedScoreService.listGroupings(clientId, pageable);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("confirm-payment/{id}")
  public ResponseEntity<?> confirmPayment(@PathVariable Long id) {
    combinedScoreService.confirmPayment(id);
    return ResponseEntity.ok().body("Pagamento confirmado com sucesso.");
  }
}
