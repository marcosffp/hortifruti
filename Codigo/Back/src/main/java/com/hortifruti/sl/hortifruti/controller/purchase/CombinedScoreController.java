package com.hortifruti.sl.hortifruti.controller.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.CombinedScoreResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.GroupedProductResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.PurchaseImageResponse;
import com.hortifruti.sl.hortifruti.dto.purchase.WildcardBilletRequest;
import com.hortifruti.sl.hortifruti.dto.purchase.client.ClientLastGroupingResponse;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreCancellationService;
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
  private final CombinedScoreCancellationService combinedScoreCancellationService;

  @PostMapping("/create")
  public ResponseEntity<?> createCombinedScore(@Valid @RequestBody CombinedScoreRequest request) {
    combinedScoreService.createCombinedScore(request);
    return ResponseEntity.ok("Agrupamento criado com sucesso.");
  }

  /**
   * Cria um agrupamento avulso com o produto coringa (R$1/kg), para clientes configurados como
   * "somente boleto".
   */
  @PostMapping("/create-wildcard-billet")
  public ResponseEntity<Long> createWildcardBillet(
      @Valid @RequestBody WildcardBilletRequest request) {
    Long combinedScoreId = combinedScoreService.createWildcardCombinedScore(request);
    return ResponseEntity.ok(combinedScoreId);
  }

  @GetMapping
  public ResponseEntity<Page<CombinedScoreResponse>> listGroupings(
      @RequestParam(required = false) Long clientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<CombinedScoreResponse> response = combinedScoreService.listGroupings(clientId, pageable);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/last-per-client")
  public ResponseEntity<List<ClientLastGroupingResponse>> getLastGroupingPerClient() {
    return ResponseEntity.ok(combinedScoreService.getLastGroupingPerClient());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> cancelGrouping(@PathVariable Long id) {
    combinedScoreCancellationService.cancelGrouping(id);
    return ResponseEntity.ok("Agrupamento cancelado com sucesso.");
  }

  @PatchMapping("/confirm-payment/{id}")
  public ResponseEntity<?> confirmPayment(@PathVariable Long id) {
    combinedScoreService.confirmPayment(id);
    return ResponseEntity.ok("Pagamento confirmado com sucesso.");
  }

  @PatchMapping("/cancel-payment/{id}")
  public ResponseEntity<?> cancelPayment(@PathVariable Long id) {
    combinedScoreService.cancelPayment(id);
    return ResponseEntity.ok("Pagamento cancelado com sucesso.");
  }

  @GetMapping("/{id}/imagens")
  public ResponseEntity<List<PurchaseImageResponse>> listImages(@PathVariable Long id) {
    return ResponseEntity.ok(combinedScoreService.listImagesByCombinedScoreId(id));
  }

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
