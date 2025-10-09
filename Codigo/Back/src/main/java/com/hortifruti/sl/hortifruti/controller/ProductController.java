package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.ProductRequest;
import com.hortifruti.sl.hortifruti.dto.ProductResponse;
import com.hortifruti.sl.hortifruti.exception.ProductException;
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
        @ApiResponse(responseCode = "400", description = "Erro de produto"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER")
    })
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        log.info("Retornando {} produtos", products.size());
        return ResponseEntity.ok(products);
    }
    
    /**
     * Lista produtos com paginação
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Listar produtos com paginação")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de produtos paginada retornada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro de produto"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER")
    })
    public ResponseEntity<Page<ProductResponse>> getProductsPaginated(
            @Parameter(description = "Número da página (inicia em 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo para ordenação")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Direção da ordenação")
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        if (page < 0 || size <= 0) {
            throw new ProductException("Parâmetros de paginação inválidos. Página deve ser >= 0 e tamanho > 0.");
        }
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getAllProducts(pageable);
        
        return ResponseEntity.ok(products);
    }
    
    /**
     * Busca produto por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Buscar produto por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto encontrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro de produto"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "ID do produto")
            @PathVariable Long id) {
        
        if (id == null || id <= 0) {
            throw new ProductException("ID do produto deve ser um número positivo válido.");
        }
        
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Busca produtos por nome
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Buscar produtos por nome")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produtos encontrados com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro de produto"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER")
    })
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Nome ou parte do nome do produto")
            @RequestParam String name) {
        
        if (name == null || name.trim().isEmpty()) {
            throw new ProductException("Nome do produto não pode ser vazio.");
        }
        
        if (name.length() < 2) {
            throw new ProductException("Nome do produto deve ter pelo menos 2 caracteres.");
        }
        
        List<ProductResponse> products = productService.searchProductsByName(name);
        return ResponseEntity.ok(products);
    }
    
    /**
     * Cria novo produto
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Criar novo produto")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou erro de produto"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER"),
        @ApiResponse(responseCode = "409", description = "Produto já existe")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Dados do produto a ser criado")
            @Valid @RequestBody ProductRequest productRequest) {
        
        ProductResponse createdProduct = productService.createProduct(productRequest);
        log.info("Produto criado: {}", createdProduct.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }
    
    /**
     * Atualiza produto existente
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Atualizar produto existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou erro de produto"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "ID do produto a ser atualizado")
            @PathVariable Long id,
            @Parameter(description = "Novos dados do produto")
            @Valid @RequestBody ProductRequest productRequest) {
        
        if (id == null || id <= 0) {
            throw new ProductException("ID do produto deve ser um número positivo válido.");
        }
        
        ProductResponse updatedProduct = productService.updateProduct(id, productRequest);
        log.info("Produto atualizado: {}", updatedProduct.name());
        return ResponseEntity.ok(updatedProduct);
    }
    
    /**
     * Remove produto
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Remover produto")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Produto removido com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro de produto"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "ID do produto a ser removido")
            @PathVariable Long id) {
        
        if (id == null || id <= 0) {
            throw new ProductException("ID do produto deve ser um número positivo válido.");
        }
        
        productService.deleteProduct(id);
        log.info("Produto removido: ID {}", id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Conta total de produtos
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Contar produtos")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contagem de produtos retornada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro de produto"),
        @ApiResponse(responseCode = "403", description = "Acesso negado - apenas MANAGER")
    })
    public ResponseEntity<Long> countProducts() {
        long count = productService.countProducts();
        return ResponseEntity.ok(count);
    }
}