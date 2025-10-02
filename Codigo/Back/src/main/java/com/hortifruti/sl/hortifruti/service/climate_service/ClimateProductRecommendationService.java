package com.hortifruti.sl.hortifruti.service.climate_service;

import com.hortifruti.sl.hortifruti.dto.climate_dto.ClimateProductRecommendationDTO;
import com.hortifruti.sl.hortifruti.dto.climate_dto.WeatherForecastDTO;
import com.hortifruti.sl.hortifruti.exception.RecommendationException;
import com.hortifruti.sl.hortifruti.model.Product;
import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import com.hortifruti.sl.hortifruti.model.enumeration.RecommendationTag;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClimateProductRecommendationService {
    
    private final ProductRepository productRepository;
    private final WeatherForecastService weatherForecastService;
    
    // Pesos para o sistema de pontuação
    private static final double CLIMATE_WEIGHT = 0.7;  // 70% - clima é o mais importante
    private static final double SEASONALITY_WEIGHT = 0.3; // 30% - sazonalidade
    
    // Pontuação base para cada categoria
    private static final double PEAK_SEASON_SCORE = 10.0;
    private static final double MEDIUM_SEASON_SCORE = 5.0;
    private static final double LOW_SEASON_SCORE = 0.0;
    private static final double PERFECT_CLIMATE_SCORE = 15.0;
    
    
    /**
     * Gera recomendações baseadas na temperatura e mês atual
     * lembrando que (mês atual) não é o mês todo, até porque a api só pega
     * 5 dias, é o Mês atual tipo hoje é Março
     */
    private List<ClimateProductRecommendationDTO> generateRecommendations(TemperatureCategory temperatureCategory, Month currentMonth) {
        List<Product> allProducts = productRepository.findAll();
        
        return allProducts.stream()
                .map(product -> calculateProductScore(product, temperatureCategory, currentMonth))
                .sorted(Comparator.comparingDouble(ClimateProductRecommendationDTO::score).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Calcula a pontuação de um produto baseado no clima e sazonalidade
     */
    private ClimateProductRecommendationDTO calculateProductScore(Product product, TemperatureCategory climateCategory, Month currentMonth) {
        // 1. Pontuação do clima (peso maior - 70%)
        double climateScore = calculateClimateScore(product, climateCategory);
        
        // 2. Pontuação da sazonalidade (peso menor - 30%)
        double seasonalityScore = calculateSeasonalityScore(product, currentMonth);
        
        // 3. Pontuação final ponderada
        double finalScore = (climateScore * CLIMATE_WEIGHT) + (seasonalityScore * SEASONALITY_WEIGHT);
        
        // 4. Determinar tag baseada na pontuação
        RecommendationTag tag = RecommendationTag.fromScore(finalScore);
        
        return new ClimateProductRecommendationDTO(
                product.getId(),
                product.getName(),
                product.getTemperatureCategory(),
                Math.round(finalScore * 100.0) / 100.0, // Arredondar para 2 casas decimais
                tag
        );
    }
    
    /**
     * Calcula pontuação baseada no clima atual
     */
    private double calculateClimateScore(Product product, TemperatureCategory currentClimate) {
        if (product.getTemperatureCategory() == currentClimate) {
            return PERFECT_CLIMATE_SCORE; // Categoria perfeita para o clima
        }
        
        // Dar pontuação parcial para categorias próximas
        return calculateProximityScore(product.getTemperatureCategory(), currentClimate);
    }
    
    /**
     * Calcula pontuação de proximidade entre categorias de temperatura
     */
    private double calculateProximityScore(TemperatureCategory productCategory, TemperatureCategory currentClimate) {
        // Criar uma "distância" entre as categorias
        int productOrdinal = productCategory.ordinal();
        int currentOrdinal = currentClimate.ordinal();
        int distance = Math.abs(productOrdinal - currentOrdinal);
        
        return switch (distance) {
            case 0 -> PERFECT_CLIMATE_SCORE;     // Mesma categoria
            case 1 -> PERFECT_CLIMATE_SCORE * 0.6; // Categoria adjacente (60%)
            case 2 -> PERFECT_CLIMATE_SCORE * 0.3; // Duas categorias de distância (30%)
            case 3 -> PERFECT_CLIMATE_SCORE * 0.1; // Três categorias de distância (10%)
            default -> 0.0;
        };
    }
    
    /**
     * Calcula pontuação baseada na sazonalidade (mês atual)
     */
    private double calculateSeasonalityScore(Product product, Month currentMonth) {
        // Mês de alta venda
        if (product.getPeakSalesMonths() != null && product.getPeakSalesMonths().contains(currentMonth)) {
            return PEAK_SEASON_SCORE;
        }
        
        // Mês de baixa venda
        if (product.getLowSalesMonths() != null && product.getLowSalesMonths().contains(currentMonth)) {
            return LOW_SEASON_SCORE;
        }
        
        // Mês médio (não está nem na lista de alta nem na de baixa)
        return MEDIUM_SEASON_SCORE;
    }
    

    
    /**
     * Obtém o mês atual baseado no enum Month customizado
     */
    private Month getCurrentMonth() {
        int currentMonthNumber = LocalDate.now().getMonthValue();
        return Month.values()[currentMonthNumber - 1]; // Month enum começa do índice 0
    }
    
    /**
     * Busca produtos por categoria de temperatura específica
     */
    public List<ClimateProductRecommendationDTO> getProductsByTemperatureCategory(TemperatureCategory category) {
        if (category == null) {
            throw new RecommendationException("Categoria de temperatura não pode ser nula.");
        }
        
        List<Product> products = productRepository.findByTemperatureCategory(category);
        if (products.isEmpty()) {
            log.warn("Nenhum produto encontrado para a categoria de temperatura: {}", category);
            return List.of(); // Retorna lista vazia em vez de lançar exception
        }
        
        Month currentMonth = getCurrentMonth();
        
        return products.stream()
                .map(product -> calculateProductScore(product, category, currentMonth))
                .sorted(Comparator.comparingDouble(ClimateProductRecommendationDTO::score).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * NOVO: Gera recomendações baseadas apenas na data
     * Busca os dados climáticos da API para a data especificada
     */
    public List<ClimateProductRecommendationDTO> getRecommendationsByDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new RecommendationException("Data não pode ser vazia.");
        }
        
        try {
            // Parse da data
            LocalDate date = LocalDate.parse(dateString);
            
            log.info("Buscando dados climáticos para a data: {}", date);
            
            // Buscar dados climáticos da API para a data específica
            WeatherForecastDTO weatherForecast = weatherForecastService.getFiveDayForecast();
            
            if (weatherForecast == null || weatherForecast.dailyForecasts().isEmpty()) {
                log.warn("Não foi possível obter dados climáticos para a data: {}", date);
                throw new RecommendationException("Dados climáticos não disponíveis para a data: " + date);
            }
            
            // Procurar o dia específico na previsão ou usar o primeiro dia disponível
            var targetDay = weatherForecast.dailyForecasts().stream()
                    .filter(day -> day.date().equals(date))
                    .findFirst()
                    .orElse(weatherForecast.dailyForecasts().get(0)); // Fallback para o primeiro dia
            
            // Determinar categoria baseada na SENSAÇÃO TÉRMICA (mais realista)
            TemperatureCategory temperatureCategory = TemperatureCategory.fromTemperature(targetDay.avgFeelsLike());
            
            // Obter o mês da data
            Month month = getMonthFromLocalDate(date);
            
            log.info("Data: {}, Temp: {}°C, Sensação: {}°C, Categoria: {}, Mês: {}", 
                    date, targetDay.avgTemp(), targetDay.avgFeelsLike(), temperatureCategory, month);
            
            // Gerar recomendações
            return generateRecommendations(temperatureCategory, month);
            
        } catch (Exception e) {
            log.error("Erro ao buscar recomendações para a data {}: {}", dateString, e.getMessage(), e);
            throw new RecommendationException("Erro ao processar data: " + dateString + ". " + e.getMessage());
        }
    }
    
    
    /**
     * Método auxiliar para converter LocalDate para Month enum
     */
    private Month getMonthFromLocalDate(LocalDate date) {
        int monthNumber = date.getMonthValue();
        for (Month month : Month.values()) {
            if (month.getNumber() == monthNumber) {
                return month;
            }
        }
        return Month.JANEIRO; // fallback
    }
}