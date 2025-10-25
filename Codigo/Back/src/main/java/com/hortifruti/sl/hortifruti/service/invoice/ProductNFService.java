package com.hortifruti.sl.hortifruti.service.invoice;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class ProductNFService {

    private List<Map<String, Object>> products;

    public ProductNFService() {
        loadProducts();
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("products.yml")) {
            Map<String, Object> data = yaml.load(inputStream);
            products = (List<Map<String, Object>>) data.get("products");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar arquivo YAML: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> findProductByCode(String code) {
        return products.stream()
                .filter(product -> product.get("code").equals(code))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Produto não encontrado para o código: " + code));
    }
}
