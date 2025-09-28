package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.dto.ProductRequest;
import com.hortifruti.sl.hortifruti.dto.ProductResponse;
import com.hortifruti.sl.hortifruti.mapper.ProductMapper;
import com.hortifruti.sl.hortifruti.model.Product;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> getAllProducts() {
        log.info("Listando todos os produtos");
        
        return productRepository.findAll().stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }
    

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Listando produtos com paginação: {}", pageable);
        
        return productRepository.findAll(pageable)
                .map(productMapper::toProductResponse);
    }
    

    public ProductResponse getProductById(Long id) {
        log.info("Buscando produto por ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        
        return productMapper.toProductResponse(product);
    }
    

    public List<ProductResponse> searchProductsByName(String name) {
        log.info("Buscando produtos por nome: {}", name);
        
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }
    

    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        log.info("Criando novo produto: {}", productRequest.name());
        
        
        if (productRepository.findByNameContainingIgnoreCase(productRequest.name()).stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(productRequest.name()))) {
            throw new RuntimeException("Já existe um produto com este nome: " + productRequest.name());
        }
        
        Product product = productMapper.toProduct(productRequest);
        Product savedProduct = productRepository.save(product);
        
        log.info("Produto criado com sucesso: ID {}", savedProduct.getId());
        
        return productMapper.toProductResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        log.info("Atualizando produto ID {}: {}", id, productRequest.name());
        
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        

        productRepository.findByNameContainingIgnoreCase(productRequest.name()).stream()
                .filter(p -> !p.getId().equals(id))
                .filter(p -> p.getName().equalsIgnoreCase(productRequest.name()))
                .findAny()
                .ifPresent(p -> {
                    throw new RuntimeException("Já existe outro produto com este nome: " + productRequest.name());
                });
        
        productMapper.updateProduct(existingProduct, productRequest);
        Product updatedProduct = productRepository.save(existingProduct);
        
        log.info("Produto atualizado com sucesso: ID {}", updatedProduct.getId());
        
        return productMapper.toProductResponse(updatedProduct);
    }
    

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Removendo produto ID: {}", id);
        
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Produto não encontrado com ID: " + id);
        }
        
        productRepository.deleteById(id);
        
        log.info("Produto removido com sucesso: ID {}", id);
    }
    

    public long countProducts() {
        return productRepository.count();
    }
}