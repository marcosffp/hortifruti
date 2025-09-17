package com.hortifruti.sl.hortifruti.controller.climate_controller;

import com.hortifruti.sl.hortifruti.dto.climate_dto.CategorizedRecommendationsDTO;
import com.hortifruti.sl.hortifruti.dto.climate_dto.RecommendationDTO;
import com.hortifruti.sl.hortifruti.service.climate_service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService service;

    @GetMapping("/categorized")
    public CategorizedRecommendationsDTO getCategorizedRecommendations() {
        return service.getCategorizedRecommendations();
    }
    
    @GetMapping("/product/{productId}")
    public RecommendationDTO getRecommendationForProduct(@PathVariable Long productId) {
        return service.recommendForProduct(productId);
    }
}
