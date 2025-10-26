package com.hortifruti.sl.hortifruti.service.climate;

import com.hortifruti.sl.hortifruti.dto.climate.ProductRequest;
import com.hortifruti.sl.hortifruti.dto.climate.ProductResponse;
import com.hortifruti.sl.hortifruti.exception.ProductException;
import com.hortifruti.sl.hortifruti.mapper.ProductMapper;
import com.hortifruti.sl.hortifruti.model.ClimateProduct;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  public List<ProductResponse> getAllProducts() {

    return productRepository.findAll().stream()
        .map(productMapper::toProductResponse)
        .collect(Collectors.toList());
  }

  public Page<ProductResponse> getAllProducts(Pageable pageable) {

    return productRepository.findAll(pageable).map(productMapper::toProductResponse);
  }

  public ProductResponse getProductById(Long id) {

    ClimateProduct product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ProductException("Produto não encontrado com ID: " + id));

    return productMapper.toProductResponse(product);
  }

  public List<ProductResponse> searchProductsByName(String name) {

    List<ProductResponse> products =
        productRepository.findByNameContainingIgnoreCase(name).stream()
            .map(productMapper::toProductResponse)
            .collect(Collectors.toList());

    return products;
  }

  @Transactional
  public ProductResponse createProduct(ProductRequest productRequest) {

    if (productRepository.findByNameContainingIgnoreCase(productRequest.name()).stream()
        .anyMatch(p -> p.getName().equalsIgnoreCase(productRequest.name()))) {
      throw new ProductException("Já existe um produto com este nome: " + productRequest.name());
    }

    ClimateProduct product = productMapper.toProduct(productRequest);
    ClimateProduct savedProduct = productRepository.save(product);

    return productMapper.toProductResponse(savedProduct);
  }

  @Transactional
  public ProductResponse updateProduct(Long id, ProductRequest productRequest) {

    ClimateProduct existingProduct =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ProductException("Produto não encontrado com ID: " + id));

    productRepository.findByNameContainingIgnoreCase(productRequest.name()).stream()
        .filter(p -> !p.getId().equals(id))
        .filter(p -> p.getName().equalsIgnoreCase(productRequest.name()))
        .findAny()
        .ifPresent(
            p -> {
              throw new ProductException(
                  "Já existe outro produto com este nome: " + productRequest.name());
            });

    productMapper.updateProduct(existingProduct, productRequest);
    ClimateProduct updatedProduct = productRepository.save(existingProduct);

    return productMapper.toProductResponse(updatedProduct);
  }

  @Transactional
  public void deleteProduct(Long id) {

    if (!productRepository.existsById(id)) {
      throw new ProductException("Produto não encontrado com ID: " + id);
    }

    productRepository.deleteById(id);
  }

  public long countProducts() {
    return productRepository.count();
  }
}
