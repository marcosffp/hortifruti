package com.hortifruti.sl.hortifruti.controller.climate_controller;

import com.hortifruti.sl.hortifruti.model.climate_model.Product;
import com.hortifruti.sl.hortifruti.repository.climate_repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository repository;

    @GetMapping
    public List<Product> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        // Verificar se já existe um produto com esse nome
        Optional<Product> existing = repository.findByNameIgnoreCase(product.getName());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        
        Product saved = repository.save(product);
        return ResponseEntity.ok(saved);
    }

        @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        Optional<Product> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Atualizar apenas os campos fornecidos
        Product existingProduct = existing.get();
        if (product.getName() != null) {
            existingProduct.setName(product.getName());
        }
        if (product.getTags() != null) {
            existingProduct.setTags(product.getTags());
        }
        
        Product saved = repository.save(existingProduct);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
