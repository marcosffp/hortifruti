package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.ProductRequest;
import com.hortifruti.sl.hortifruti.dto.ProductResponse;
import com.hortifruti.sl.hortifruti.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para operações CRUD de produtos
 * Acessível apenas para usuários com role MANAGER
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Endpoints para gerenciamento de produtos (apenas MANAGER)")
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * Lista todos os produtos
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Listar todos os produtos", description = "Lista todos os produtos cadastrados")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de produtos retornada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER")
    })
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        try {
            List<ProductResponse> products = productService.getAllProducts();
            log.info("Retornando {} produtos", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Erro ao listar produtos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Lista produtos com paginação
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Listar produtos com paginação")
    public ResponseEntity<Page<ProductResponse>> getProductsPaginated(
            @Parameter(description = "Número da página (inicia em 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo para ordenação")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Direção da ordenação")
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ProductResponse> products = productService.getAllProducts(pageable);
            
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Erro ao listar produtos paginados: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Busca produto por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Buscar produto por ID")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "ID do produto")
            @PathVariable Long id) {
        
        try {
            ProductResponse product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            log.warn("Produto não encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao buscar produto por ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Busca produtos por nome
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Buscar produtos por nome")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Nome ou parte do nome do produto")
            @RequestParam String name) {
        
        try {
            List<ProductResponse> products = productService.searchProductsByName(name);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Erro ao buscar produtos por nome '{}': {}", name, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Cria novo produto
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Criar novo produto")
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Dados do produto a ser criado")
            @Valid @RequestBody ProductRequest productRequest) {
        
        try {
            ProductResponse createdProduct = productService.createProduct(productRequest);
            log.info("Produto criado: {}", createdProduct.name());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        } catch (RuntimeException e) {
            log.warn("Erro ao criar produto: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro interno ao criar produto: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Atualiza produto existente
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Atualizar produto existente")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "ID do produto a ser atualizado")
            @PathVariable Long id,
            @Parameter(description = "Novos dados do produto")
            @Valid @RequestBody ProductRequest productRequest) {
        
        try {
            ProductResponse updatedProduct = productService.updateProduct(id, productRequest);
            log.info("Produto atualizado: {}", updatedProduct.name());
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            log.warn("Erro ao atualizar produto ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro interno ao atualizar produto ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Remove produto
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Remover produto")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "ID do produto a ser removido")
            @PathVariable Long id) {
        
        try {
            productService.deleteProduct(id);
            log.info("Produto removido: ID {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Erro ao remover produto ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro interno ao remover produto ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Conta total de produtos
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Contar produtos")
    public ResponseEntity<Long> countProducts() {
        try {
            long count = productService.countProducts();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Erro ao contar produtos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}