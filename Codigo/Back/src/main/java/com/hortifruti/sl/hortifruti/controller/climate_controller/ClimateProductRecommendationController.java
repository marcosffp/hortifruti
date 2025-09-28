package com.hortifruti.sl.hortifruti.controller.climate_controller;

import com.hortifruti.sl.hortifruti.dto.climate_dto.ClimateProductRecommendationDTO;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.service.climate_service.ClimateProductRecommendationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Product Recommendations", description = "Endpoints para recomendação inteligente de produtos baseada em clima e sazonalidade (acesso restrito a MANAGER)")
public class ClimateProductRecommendationController {
    
    private final ClimateProductRecommendationService recommendationService;
    
    /**
     * Obtém recomendações de produtos baseadas no clima atual
     * Usa automaticamente a cidade configurada no application.yml
     * Acesso restrito apenas para usuários MANAGER
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de recomendações obtida com sucesso"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ClimateProductRecommendationDTO>> getRecommendations() {
        
        try {
            log.info("Buscando recomendações usando cidade configurada no sistema");
            
            List<ClimateProductRecommendationDTO> recommendations = recommendationService.getRecommendations();
            
            log.info("Encontradas {} recomendações", recommendations.size());
            
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            log.error("Erro ao buscar recomendações: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtém produtos por categoria de temperatura
     * Acesso restrito apenas para usuários MANAGER
     */
    @GetMapping("/by-temperature/{category}")
    @PreAuthorize("hasRole('MANAGER')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produtos encontrados com sucesso"),
        @ApiResponse(responseCode = "400", description = "Categoria de temperatura inválida"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ClimateProductRecommendationDTO>> getProductsByTemperature(
            @Parameter(description = "Categoria de temperatura", example = "QUENTE")
            @PathVariable TemperatureCategory category) {
        
        try {
            log.info("Buscando produtos para categoria de temperatura: {}", category);
            
            List<ClimateProductRecommendationDTO> products = recommendationService.getProductsByTemperatureCategory(category);
            
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
     * Acesso restrito apenas para usuários MANAGER
     */
    @PostMapping("/by-day-climate")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Recomendações por dados climáticos de um dia", 
               description = "Retorna produtos recomendados baseados nos dados completos de clima de um dia específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recomendações obtidas com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados climáticos inválidos"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ClimateProductRecommendationDTO>> getRecommendationsByDayClimate(
            @Parameter(description = "Dados climáticos do dia selecionado")
            @RequestBody com.hortifruti.sl.hortifruti.dto.climate_dto.DayClimateDataDTO dayClimateData) {
        
        try {
            log.info("Buscando recomendações para dia {} com temperatura média {}°C", 
                    dayClimateData.date(), dayClimateData.avgTemp());
            
            List<ClimateProductRecommendationDTO> recommendations = recommendationService.getRecommendationsByDayClimate(dayClimateData);
            
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
     * NOVO: Recomendações baseadas apenas na data
     * O frontend envia apenas a data, e buscamos os dados climáticos via API
     */
    @GetMapping("/by-date")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Recomendações por data", 
               description = "Retorna produtos recomendados baseados nos dados climáticos da data especificada. Acesso apenas para MANAGER.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recomendações obtidas com sucesso"),
        @ApiResponse(responseCode = "400", description = "Data inválida"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ClimateProductRecommendationDTO>> getRecommendationsByDate(
            @Parameter(description = "Data para buscar recomendações (formato: YYYY-MM-DD)", example = "2025-09-27")
            @RequestParam String date) {
        
        try {
            log.info("Buscando recomendações para a data: {}", date);
            
            List<ClimateProductRecommendationDTO> recommendations = recommendationService.getRecommendationsByDate(date);
            
            log.info("Encontradas {} recomendações para a data {}", recommendations.size(), date);
            
            return ResponseEntity.ok(recommendations);
            
        } catch (IllegalArgumentException e) {
            log.warn("Data inválida: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao buscar recomendações por data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint de teste para verificar se o sistema está funcionando
     * Acesso restrito apenas para usuários MANAGER
     */
    @GetMapping("/test")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> testRecommendations() {
        try {
            // Testar usando cidade configurada no sistema
            List<ClimateProductRecommendationDTO> recommendations = recommendationService.getRecommendations();
            
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