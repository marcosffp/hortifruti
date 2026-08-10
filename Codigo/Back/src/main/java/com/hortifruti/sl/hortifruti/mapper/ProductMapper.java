package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.climate.ProductRequest;
import com.hortifruti.sl.hortifruti.dto.climate.ProductResponse;
import com.hortifruti.sl.hortifruti.model.climate.ClimateProduct;
import com.hortifruti.sl.hortifruti.model.climate.Month;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

  @Mapping(target = "id", ignore = true)
  ClimateProduct toProduct(ProductRequest productRequest);

  default ProductResponse toProductResponse(ClimateProduct product) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getTemperatureCategory(),
        product.getPeakSalesMonths(),
        product.getLowSalesMonths(),
        product.getTemperatureCategory().getDisplayName(),
        formatMonthsList(product.getPeakSalesMonths()),
        formatMonthsList(product.getLowSalesMonths()));
  }

  private static String formatMonthsList(List<Month> months) {
    if (months == null || months.isEmpty()) {
      return "Nenhum";
    }
    return months.stream()
        .map(Month::getDisplayName)
        .reduce((a, b) -> a + ", " + b)
        .orElse("Nenhum");
  }

  @Mapping(target = "id", ignore = true)
  void updateProduct(@MappingTarget ClimateProduct target, ProductRequest source);
}
