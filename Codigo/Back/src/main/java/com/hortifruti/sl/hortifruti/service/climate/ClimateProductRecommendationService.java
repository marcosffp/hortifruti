package com.hortifruti.sl.hortifruti.service.climate;

import com.hortifruti.sl.hortifruti.dto.climate.ClimateProductRecommendationDTO;
import com.hortifruti.sl.hortifruti.dto.climate.WeatherForecastDTO;
import com.hortifruti.sl.hortifruti.exception.RecommendationException;
import com.hortifruti.sl.hortifruti.model.ClimateProduct;
import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import com.hortifruti.sl.hortifruti.model.enumeration.RecommendationTag;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClimateProductRecommendationService {

  private final ProductRepository productRepository;
  private final WeatherForecastService weatherForecastService;

  private static final double CLIMATE_WEIGHT = 0.7; 
  private static final double SEASONALITY_WEIGHT = 0.3; 

  private static final double PEAK_SEASON_SCORE = 10.0;
  private static final double MEDIUM_SEASON_SCORE = 5.0;
  private static final double LOW_SEASON_SCORE = 0.0;
  private static final double PERFECT_CLIMATE_SCORE = 15.0;

  /**
   * Gera recomendações baseadas na temperatura e mês atual lembrando que (mês atual) não é o mês
   * todo, até porque a api só pega 5 dias, é o Mês atual tipo hoje é Março
   */
  private List<ClimateProductRecommendationDTO> generateRecommendations(
      TemperatureCategory temperatureCategory, Month currentMonth) {
    List<ClimateProduct> allProducts = productRepository.findAll();

    return allProducts.stream()
        .map(product -> calculateProductScore(product, temperatureCategory, currentMonth))
        .sorted(Comparator.comparingDouble(ClimateProductRecommendationDTO::score).reversed())
        .collect(Collectors.toList());
  }

  /** Calcula a pontuação de um produto baseado no clima e sazonalidade */
  private ClimateProductRecommendationDTO calculateProductScore(
      ClimateProduct product, TemperatureCategory climateCategory, Month currentMonth) {
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
        Math.round(finalScore * 100.0) / 100.0, 
        tag);
  }

  /** Calcula pontuação baseada no clima atual */
  private double calculateClimateScore(ClimateProduct product, TemperatureCategory currentClimate) {
    if (product.getTemperatureCategory() == currentClimate) {
      return PERFECT_CLIMATE_SCORE; 
    }

    return calculateProximityScore(product.getTemperatureCategory(), currentClimate);
  }

  /** Calcula pontuação de proximidade entre categorias de temperatura */
  private double calculateProximityScore(
      TemperatureCategory productCategory, TemperatureCategory currentClimate) {
    int productOrdinal = productCategory.ordinal();
    int currentOrdinal = currentClimate.ordinal();
    int distance = Math.abs(productOrdinal - currentOrdinal);

    return switch (distance) {
      case 0 -> PERFECT_CLIMATE_SCORE;
      case 1 -> PERFECT_CLIMATE_SCORE * 0.6;
      case 2 -> PERFECT_CLIMATE_SCORE * 0.3;
      case 3 -> PERFECT_CLIMATE_SCORE * 0.1;
      default -> 0.0;
    };
  }

  /** Calcula pontuação baseada na sazonalidade (mês atual) */
  private double calculateSeasonalityScore(ClimateProduct product, Month currentMonth) {
    if (product.getPeakSalesMonths() != null
        && product.getPeakSalesMonths().contains(currentMonth)) {
      return PEAK_SEASON_SCORE;
    }

    if (product.getLowSalesMonths() != null && product.getLowSalesMonths().contains(currentMonth)) {
      return LOW_SEASON_SCORE;
    }

    return MEDIUM_SEASON_SCORE;
  }

  /** Obtém o mês atual baseado no enum Month customizado */
  private Month getCurrentMonth() {
    int currentMonthNumber = LocalDate.now().getMonthValue();
    return Month.values()[currentMonthNumber - 1];
  }

  /** Busca produtos por categoria de temperatura específica */
  public List<ClimateProductRecommendationDTO> getProductsByTemperatureCategory(
      TemperatureCategory category) {
    if (category == null) {
      throw new RecommendationException("Categoria de temperatura não pode ser nula.");
    }

    List<ClimateProduct> products = productRepository.findByTemperatureCategory(category);
    if (products.isEmpty()) {
      return List.of();
    }

    Month currentMonth = getCurrentMonth();

    return products.stream()
        .map(product -> calculateProductScore(product, category, currentMonth))
        .sorted(Comparator.comparingDouble(ClimateProductRecommendationDTO::score).reversed())
        .collect(Collectors.toList());
  }

  /**
   * NOVO: Gera recomendações baseadas apenas na data Busca os dados climáticos da API para a data
   * especificada
   */
  public List<ClimateProductRecommendationDTO> getRecommendationsByDate(String dateString) {
    if (dateString == null || dateString.trim().isEmpty()) {
      throw new RecommendationException("Data não pode ser vazia.");
    }

    try {
      LocalDate date = LocalDate.parse(dateString);


      WeatherForecastDTO weatherForecast = weatherForecastService.getFiveDayForecast();

      if (weatherForecast == null || weatherForecast.dailyForecasts().isEmpty()) {
        throw new RecommendationException("Dados climáticos não disponíveis para a data: " + date);
      }

      var targetDay =
          weatherForecast.dailyForecasts().stream()
              .filter(day -> day.date().equals(date))
              .findFirst()
              .orElse(weatherForecast.dailyForecasts().get(0)); 

      TemperatureCategory temperatureCategory =
          TemperatureCategory.fromTemperature(targetDay.avgFeelsLike());

      Month month = getMonthFromLocalDate(date);


      return generateRecommendations(temperatureCategory, month);

    } catch (Exception e) {
      throw new RecommendationException(
          "Erro ao processar data: " + dateString + ". " + e.getMessage());
    }
  }

  /** Método auxiliar para converter LocalDate para Month enum */
  private Month getMonthFromLocalDate(LocalDate date) {
    int monthNumber = date.getMonthValue();
    for (Month month : Month.values()) {
      if (month.getNumber() == monthNumber) {
        return month;
      }
    }
    return Month.JANEIRO; 
  }
}
