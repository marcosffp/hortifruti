package com.hortifruti.sl.hortifruti.controller.recommendation;

import com.hortifruti.sl.hortifruti.dto.recommendation.ProductRecommendationDTO;
import com.hortifruti.sl.hortifruti.model.climate_model.TemperatureCategory;
import com.hortifruti.sl.hortifruti.service.recommendation.ProductRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Product Recommendations", description = "Endpoints para recomendação inteligente de produtos baseada em clima e sazonalidade")
public class ProductRecommendationController {
    
    private final ProductRecommendationService recommendationService;
    
    /**
     * Obtém recomendações de produtos baseadas no clima atual
     * Usa automaticamente a cidade configurada no application.yml
     */
    @GetMapping
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de recomendações obtida com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ProductRecommendationDTO>> getRecommendations() {
        
        try {
            log.info("Buscando recomendações usando cidade configurada no sistema");
            
            List<ProductRecommendationDTO> recommendations = recommendationService.getRecommendations();
            
            log.info("Encontradas {} recomendações", recommendations.size());
            
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            log.error("Erro ao buscar recomendações: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtém produtos por categoria de temperatura
     */
    @GetMapping("/by-temperature/{category}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produtos encontrados com sucesso"),
        @ApiResponse(responseCode = "400", description = "Categoria de temperatura inválida"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ProductRecommendationDTO>> getProductsByTemperature(
            @Parameter(description = "Categoria de temperatura", example = "QUENTE")
            @PathVariable TemperatureCategory category) {
        
        try {
            log.info("Buscando produtos para categoria de temperatura: {}", category);
            
            List<ProductRecommendationDTO> products = recommendationService.getProductsByTemperatureCategory(category);
            
            log.info("Encontrados {} produtos para categoria {}", products.size(), category);
            
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            log.error("Erro ao buscar produtos por categoria {}: {}", category, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * NOVO: Obtém recomendações baseadas em dados climáticos específicos de um dia
     * Este endpoint será chamado quando o usuário clicar em um card específico dos 5 dias
     */
    @PostMapping("/by-day-climate")
    @Operation(summary = "Recomendações por dados climáticos de um dia", 
               description = "Retorna produtos recomendados baseados nos dados completos de clima de um dia específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recomendações obtidas com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados climáticos inválidos"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ProductRecommendationDTO>> getRecommendationsByDayClimate(
            @Parameter(description = "Dados climáticos do dia selecionado")
            @RequestBody com.hortifruti.sl.hortifruti.dto.recommendation.DayClimateDataDTO dayClimateData) {
        
        try {
            log.info("Buscando recomendações para dia {} com temperatura média {}°C", 
                    dayClimateData.date(), dayClimateData.avgTemp());
            
            List<ProductRecommendationDTO> recommendations = recommendationService.getRecommendationsByDayClimate(dayClimateData);
            
            log.info("Encontradas {} recomendações para o dia {}", recommendations.size(), dayClimateData.date());
            
            return ResponseEntity.ok(recommendations);
            
        } catch (IllegalArgumentException e) {
            log.warn("Dados climáticos inválidos: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao buscar recomendações por clima do dia: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint de teste para verificar se o sistema está funcionando
     */
    @GetMapping("/test")
    public ResponseEntity<String> testRecommendations() {
        try {
            // Testar usando cidade configurada no sistema
            List<ProductRecommendationDTO> recommendations = recommendationService.getRecommendations();
            
            return ResponseEntity.ok(String.format(
                "Sistema de recomendações funcionando! Encontradas %d recomendações.", 
                recommendations.size()
            ));
            
        } catch (Exception e) {
            log.error("Erro no teste de recomendações: {}", e.getMessage(), e);
            return ResponseEntity.ok("Erro no sistema de recomendações: " + e.getMessage());
        }
    }
}