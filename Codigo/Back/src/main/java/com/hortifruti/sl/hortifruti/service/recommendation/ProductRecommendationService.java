package com.hortifruti.sl.hortifruti.service.recommendation;

import com.hortifruti.sl.hortifruti.dto.climate_dto.WeatherForecastDTO;
import com.hortifruti.sl.hortifruti.dto.recommendation.ProductRecommendationDTO;
import com.hortifruti.sl.hortifruti.model.Product;
import com.hortifruti.sl.hortifruti.model.climate_model.Month;
import com.hortifruti.sl.hortifruti.model.climate_model.TemperatureCategory;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;
import com.hortifruti.sl.hortifruti.service.climate_service.WeatherForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductRecommendationService {
    
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
     * Gera recomendações de produtos usando a cidade configurada no sistema
     * @return lista de produtos recomendados ordenada por pontuação
     */
    public List<ProductRecommendationDTO> getRecommendations() {
        try {
            // Buscar previsão do tempo (usa automaticamente cidade do YML)
            WeatherForecastDTO weatherForecast = weatherForecastService.getFiveDayForecast();
            
            if (weatherForecast == null || weatherForecast.dailyForecasts().isEmpty()) {
                log.warn("Não foi possível obter previsão do tempo");
                return getDefaultRecommendations();
            }
            
            // Pegar temperatura média do primeiro dia (hoje)
            double todayAvgTemp = weatherForecast.dailyForecasts().get(0).avgTemp();
            TemperatureCategory todayCategory = TemperatureCategory.fromTemperature(todayAvgTemp);
            
            log.info("Gerando recomendações para {} - Temperatura: {}°C ({})", 
                    weatherForecast.city(), todayAvgTemp, todayCategory.getDisplayName());
            
            return generateRecommendations(todayCategory, getCurrentMonth());
            
        } catch (Exception e) {
            log.error("Erro ao gerar recomendações: {}", e.getMessage(), e);
            return getDefaultRecommendations();
        }
    }
    
    /**
     * @deprecated Mantido para compatibilidade. Use {@link #getRecommendations()} que usa a cidade configurada no YML
     */
    @Deprecated
    public List<ProductRecommendationDTO> getRecommendations(String city) {
        log.warn("Método getRecommendations(String city) está deprecated. Parâmetro '{}' será ignorado, usando cidade configurada no sistema.", city);
        return getRecommendations();
    }
    
    /**
     * Gera recomendações baseadas na temperatura e mês atual
     * lembrando que (mês atual) não é o mês todo, até porque a api só pega
     * 5 dias, é o Mês atual tipo hoje é Março
     */
    private List<ProductRecommendationDTO> generateRecommendations(TemperatureCategory temperatureCategory, Month currentMonth) {
        List<Product> allProducts = productRepository.findAll();
        
        return allProducts.stream()
                .map(product -> calculateProductScore(product, temperatureCategory, currentMonth))
                .sorted(Comparator.comparingDouble(ProductRecommendationDTO::score).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Calcula a pontuação de um produto baseado no clima e sazonalidade
     */
    private ProductRecommendationDTO calculateProductScore(Product product, TemperatureCategory climateCategory, Month currentMonth) {
        // 1. Pontuação do clima (peso maior - 70%)
        double climateScore = calculateClimateScore(product, climateCategory);
        
        // 2. Pontuação da sazonalidade (peso menor - 30%)
        double seasonalityScore = calculateSeasonalityScore(product, currentMonth);
        
        // 3. Pontuação final ponderada
        double finalScore = (climateScore * CLIMATE_WEIGHT) + (seasonalityScore * SEASONALITY_WEIGHT);
        
        // 4. Determinar razão da recomendação
        String recommendationReason = buildRecommendationReason(product, climateCategory, currentMonth, climateScore, seasonalityScore);
        
        return new ProductRecommendationDTO(
                product.getId(),
                product.getName(),
                product.getTemperatureCategory(),
                Math.round(finalScore * 100.0) / 100.0, // Arredondar para 2 casas decimais
                recommendationReason
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
     * Constrói a explicação da recomendação
     */
    private String buildRecommendationReason(Product product, TemperatureCategory climateCategory, Month currentMonth, 
                                           double climateScore, double seasonalityScore) {
        List<String> reasons = new ArrayList<>();
        
        // Razão climática (mais importante)
        if (climateScore >= PERFECT_CLIMATE_SCORE) {
            reasons.add("Perfeito para o clima " + climateCategory.getDisplayName().toLowerCase());
        } else if (climateScore > PERFECT_CLIMATE_SCORE * 0.5) {
            reasons.add("Adequado para o clima " + climateCategory.getDisplayName().toLowerCase());
        }
        
        // Razão sazonal
        if (seasonalityScore >= PEAK_SEASON_SCORE) {
            reasons.add("Alta temporada de vendas");
        } else if (seasonalityScore >= MEDIUM_SEASON_SCORE) {
            reasons.add("Temporada regular");
        }
        
        if (reasons.isEmpty()) {
            reasons.add("Produto disponível");
        }
        
        return String.join(" • ", reasons);
    }
    
    /**
     * Obtém o mês atual baseado no enum Month customizado
     */
    private Month getCurrentMonth() {
        int currentMonthNumber = LocalDate.now().getMonthValue();
        return Month.values()[currentMonthNumber - 1]; // Month enum começa do índice 0
    }
    
    /**
     * Retorna recomendações padrão quando não há dados climáticos
     */
    private List<ProductRecommendationDTO> getDefaultRecommendations() {
        log.info("Gerando recomendações padrão (sem dados climáticos)");
        
        Month currentMonth = getCurrentMonth();
        List<Product> products = productRepository.findAll();
        
        return products.stream()
                .map(product -> {
                    double seasonalityScore = calculateSeasonalityScore(product, currentMonth);
                    String reason = seasonalityScore >= PEAK_SEASON_SCORE ? 
                            "Alta temporada de vendas" : "Produto disponível";
                    
                    return new ProductRecommendationDTO(
                            product.getId(),
                            product.getName(),
                            product.getTemperatureCategory(),
                            seasonalityScore,
                            reason
                    );
                })
                .sorted(Comparator.comparingDouble(ProductRecommendationDTO::score).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Busca produtos por categoria de temperatura específica
     */
    public List<ProductRecommendationDTO> getProductsByTemperatureCategory(TemperatureCategory category) {
        List<Product> products = productRepository.findByTemperatureCategory(category);
        Month currentMonth = getCurrentMonth();
        
        return products.stream()
                .map(product -> calculateProductScore(product, category, currentMonth))
                .sorted(Comparator.comparingDouble(ProductRecommendationDTO::score).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * NOVO: Gera recomendações baseadas apenas na data
     * Busca os dados climáticos da API para a data especificada
     */
    public List<ProductRecommendationDTO> getRecommendationsByDate(String dateString) {
        try {
            // Parse da data
            LocalDate date = LocalDate.parse(dateString);
            
            log.info("Buscando dados climáticos para a data: {}", date);
            
            // Buscar dados climáticos da API para a data específica
            WeatherForecastDTO weatherForecast = weatherForecastService.getFiveDayForecast();
            
            if (weatherForecast == null || weatherForecast.dailyForecasts().isEmpty()) {
                log.warn("Não foi possível obter dados climáticos para a data: {}", date);
                return List.of();
            }
            
            // Procurar o dia específico na previsão ou usar o primeiro dia disponível
            var targetDay = weatherForecast.dailyForecasts().stream()
                    .filter(day -> day.date().equals(date))
                    .findFirst()
                    .orElse(weatherForecast.dailyForecasts().get(0)); // Fallback para o primeiro dia
            
            // Determinar categoria de temperatura
            TemperatureCategory temperatureCategory = TemperatureCategory.fromTemperature(targetDay.avgTemp());
            
            // Obter o mês da data
            Month month = getMonthFromLocalDate(date);
            
            log.info("Data: {}, Temp: {}°C, Categoria: {}, Mês: {}", 
                    date, targetDay.avgTemp(), temperatureCategory, month);
            
            // Gerar recomendações
            return generateRecommendations(temperatureCategory, month);
            
        } catch (Exception e) {
            log.error("Erro ao buscar recomendações para a data {}: {}", dateString, e.getMessage(), e);
            throw new IllegalArgumentException("Erro ao processar data: " + dateString, e);
        }
    }
    
    /**
     * NOVO: Gera recomendações baseadas em dados climáticos completos de um dia
     * Versão melhorada que recebe um DTO com todos os dados organizados
     */
    public List<ProductRecommendationDTO> getRecommendationsByDayClimate(
            com.hortifruti.sl.hortifruti.dto.recommendation.DayClimateDataDTO dayClimateData) {
        
        try {
            // Determinar categoria de temperatura baseada na temperatura média
            TemperatureCategory temperatureCategory = TemperatureCategory.fromTemperature(dayClimateData.avgTemp());
            
            // Extrair mês da data fornecida
            Month month = Month.values()[dayClimateData.date().getMonthValue() - 1];
            
            log.info("Gerando recomendações para {} ({}°C - {}) no mês de {} com dados completos", 
                    dayClimateData.date(), dayClimateData.avgTemp(), 
                    temperatureCategory.getDisplayName(), month.name());
            
            // Usar dados extras para futuras melhorias no algoritmo
            log.debug("Dados extras do clima - Umidade: {}%, Chuva: {}mm, Vento: {}km/h", 
                    dayClimateData.humidity(), dayClimateData.rainfall(), dayClimateData.windSpeed());
            
            return generateRecommendations(temperatureCategory, month);
            
        } catch (Exception e) {
            log.error("Erro ao processar dados climáticos do dia {}: {}", 
                    dayClimateData.date(), e.getMessage());
            throw new IllegalArgumentException("Dados climáticos inválidos", e);
        }
    }
    
    /**
     * DEPRECATED: Método mantido para compatibilidade, mas recomenda-se usar o novo método com DTO
     * @deprecated Use {@link #getRecommendationsByDayClimate(com.hortifruti.sl.hortifruti.dto.recommendation.DayClimateDataDTO)} instead
     */
    @Deprecated
    public List<ProductRecommendationDTO> getRecommendationsByDayClimate(
            double avgTemp, String dateStr, Double minTemp, Double maxTemp, Double humidity) {
        
        try {
            // Converter para o novo formato de DTO
            LocalDate date = LocalDate.parse(dateStr);
            var dayClimateData = new com.hortifruti.sl.hortifruti.dto.recommendation.DayClimateDataDTO(
                date, 
                minTemp != null ? minTemp : avgTemp - 5,
                maxTemp != null ? maxTemp : avgTemp + 5,
                avgTemp,
                humidity != null ? humidity : 50.0,
                0.0, // rainfall padrão
                0.0, // windSpeed padrão
                "N/A", // weatherDescription padrão
                "01d" // weatherIcon padrão
            );
            
            // Chamar o novo método
            return getRecommendationsByDayClimate(dayClimateData);
            
        } catch (Exception e) {
            log.error("Erro ao processar data {}: {}", dateStr, e.getMessage());
            throw new IllegalArgumentException("Data inválida: " + dateStr, e);
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