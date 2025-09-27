package com.hortifruti.sl.hortifruti.mapper;

import com.hortifruti.sl.hortifruti.dto.ProductRequest;
import com.hortifruti.sl.hortifruti.dto.ProductResponse;
import com.hortifruti.sl.hortifruti.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring")
public interface ProductMapper {
    

    @Mapping(target = "id", ignore = true)
    Product toProduct(ProductRequest productRequest);
    

    default ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getTemperatureCategory(),
            product.getPeakSalesMonths(),
            product.getLowSalesMonths()
        );
    }

    @Mapping(target = "id", ignore = true)
    void updateProduct(@MappingTarget Product target, ProductRequest source);
}