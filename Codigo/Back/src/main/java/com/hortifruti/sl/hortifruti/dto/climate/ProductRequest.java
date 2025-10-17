package com.hortifruti.sl.hortifruti.dto.climate;

import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTO para requisições de criação/atualização de produtos */
public record ProductRequest(
    @NotBlank(message = "Nome do produto é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String name,
    @NotNull(message = "Categoria de temperatura é obrigatória")
        TemperatureCategory temperatureCategory,
    List<Month> peakSalesMonths,
    List<Month> lowSalesMonths) {

  /** Construtor com validações customizadas */
  public ProductRequest {
    // Garantir que as listas não sejam nulas
    if (peakSalesMonths == null) {
      peakSalesMonths = List.of();
    }
    if (lowSalesMonths == null) {
      lowSalesMonths = List.of();
    }
  }
}
