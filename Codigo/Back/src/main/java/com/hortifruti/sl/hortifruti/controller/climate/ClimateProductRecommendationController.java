package com.hortifruti.sl.hortifruti.controller.climate;

import com.hortifruti.sl.hortifruti.dto.climate.ClimateProductRecommendationDTO;
import com.hortifruti.sl.hortifruti.exception.RecommendationException;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.service.climate.ClimateProductRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(
    name = "Product Recommendations",
    description =
        "Endpoints para recomendação inteligente de produtos baseada em clima e sazonalidade (acesso restrito a MANAGER)")
public class ClimateProductRecommendationController {

  private final ClimateProductRecommendationService recommendationService;

  /** Obtém produtos por categoria de temperatura Acesso restrito apenas para usuários MANAGER */
  @GetMapping("/by-temperature/{category}")
  @PreAuthorize("hasRole('MANAGER')")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Produtos encontrados com sucesso"),
    @ApiResponse(
        responseCode = "400",
        description = "Categoria de temperatura inválida ou erro de recomendação"),
    @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER")
  })
  public ResponseEntity<List<ClimateProductRecommendationDTO>> getProductsByTemperature(
      @Parameter(description = "Categoria de temperatura", example = "QUENTE") @PathVariable
          TemperatureCategory category) {

    if (category == null) {
      throw new RecommendationException("Categoria de temperatura não pode ser nula.");
    }

    log.info("Buscando produtos para categoria de temperatura: {}", category);

    List<ClimateProductRecommendationDTO> products =
        recommendationService.getProductsByTemperatureCategory(category);

    log.info("Encontrados {} produtos para categoria {}", products.size(), category);

    return ResponseEntity.ok(products);
  }

  /**
   * Recomendações baseadas apenas na data O frontend envia apenas a data, e buscamos os dados
   * climáticos via API
   */
  @GetMapping("/by-date")
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(
      summary = "Recomendações por data",
      description =
          "Retorna produtos recomendados baseados nos dados climáticos da data especificada. Acesso apenas para MANAGER.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Recomendações obtidas com sucesso"),
    @ApiResponse(responseCode = "400", description = "Data inválida ou erro de recomendação"),
    @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER")
  })
  public ResponseEntity<List<ClimateProductRecommendationDTO>> getRecommendationsByDate(
      @Parameter(
              description = "Data para buscar recomendações (formato: YYYY-MM-DD)",
              example = "2025-09-27")
          @RequestParam
          String date) {

    if (date == null || date.trim().isEmpty()) {
      throw new RecommendationException("Data não pode ser vazia.");
    }

    // Validação básica do formato da data
    if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
      throw new RecommendationException("Formato de data inválido. Use o formato YYYY-MM-DD.");
    }

    log.info("Buscando recomendações para a data: {}", date);

    List<ClimateProductRecommendationDTO> recommendations =
        recommendationService.getRecommendationsByDate(date);

    log.info("Encontradas {} recomendações para a data {}", recommendations.size(), date);

    return ResponseEntity.ok(recommendations);
  }
}
