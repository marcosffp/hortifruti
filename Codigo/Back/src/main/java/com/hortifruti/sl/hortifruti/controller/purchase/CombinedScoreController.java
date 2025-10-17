package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductResponse;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/combined-scores")
@Tag(
    name = "CombinedScore Management",
    description = "Operações relacionadas ao gerenciamento de scores combinados")
public class CombinedScoreController {

  private final CombinedScoreService combinedScoreService;

  /** Cria um novo agrupamento de compras. */
  @PostMapping("/create")
  public ResponseEntity<?> createCombinedScore(@Valid @RequestBody CombinedScoreRequest request) {
    combinedScoreService.createCombinedScore(request);
    return ResponseEntity.ok("Agrupamento criado com sucesso.");
  }

  /** Lista os agrupamentos de compras, com suporte a paginação. */
  @GetMapping
  public ResponseEntity<Page<CombinedScoreResponse>> listGroupings(
      @RequestParam(required = false) Long clientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<CombinedScoreResponse> response = combinedScoreService.listGroupings(clientId, pageable);
    return ResponseEntity.ok(response);
  }

  /** Cancela um agrupamento de compras pelo ID. */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> cancelGrouping(@PathVariable Long id) {
    combinedScoreService.cancelGrouping(id);
    return ResponseEntity.ok("Agrupamento cancelado com sucesso.");
  }

  /** Confirma o pagamento de um agrupamento de compras pelo ID. */
  @PatchMapping("/confirm-payment/{id}")
  public ResponseEntity<?> confirmPayment(@PathVariable Long id) {
    combinedScoreService.confirmPayment(id);
    return ResponseEntity.ok("Pagamento confirmado com sucesso.");
  }

  /** Cancela o pagamento de um agrupamento de compras pelo ID. */
  @PatchMapping("/cancel-payment/{id}")
  public ResponseEntity<?> cancelPayment(@PathVariable Long id) {
    combinedScoreService.cancelPayment(id);
    return ResponseEntity.ok("Pagamento cancelado com sucesso.");
  }

  /** Lista os produtos agrupados associados a um CombinedScore pelo ID. */
  @GetMapping("/{id}/grouped-products")
  public ResponseEntity<List<GroupedProductResponse>> getGroupedProductsByCombinedScoreId(
      @PathVariable Long id) {
    try {
      List<GroupedProductResponse> groupedProducts =
          combinedScoreService.getGroupedProductsByCombinedScoreId(id);
      return ResponseEntity.ok(groupedProducts);
    } catch (Exception e) {
      log.error("Erro ao buscar produtos agrupados do CombinedScore: ", e);
      return ResponseEntity.status(500).body(null);
    }
  }
}
