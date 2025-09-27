package com.hortifruti.sl.hortifruti.service.recommendation;

import com.hortifruti.sl.hortifruti.model.Product;
import com.hortifruti.sl.hortifruti.model.climate_model.Month;
import com.hortifruti.sl.hortifruti.model.climate_model.TemperatureCategory;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Serviço para popular dados de exemplo de produtos na inicialização da aplicação
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDataInitializer implements CommandLineRunner {
    
    private final ProductRepository productRepository;
    
    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            log.info("Populando dados de exemplo de produtos...");
            createSampleProducts();
            log.info("Dados de exemplo criados com sucesso!");
        } else {
            log.info("Produtos já existem no banco de dados. Pulando inicialização de dados.");
        }
    }
    
    private void createSampleProducts() {
        // Produtos QUENTES (>=25°C)
        productRepository.save(new Product(
            "Melancia",
            TemperatureCategory.QUENTE,
            List.of(Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO, Month.MARCO), // Verão
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO) // Inverno
        ));
        
        productRepository.save(new Product(
            "Abacaxi",
            TemperatureCategory.QUENTE,
            List.of(Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO),
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO)
        ));
        
        productRepository.save(new Product(
            "Água de Coco",
            TemperatureCategory.QUENTE,
            List.of(Month.NOVEMBRO, Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO, Month.MARCO),
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO)
        ));
        
        // Produtos AMENOS (15-24°C)
        productRepository.save(new Product(
            "Maçã",
            TemperatureCategory.AMENO,
            List.of(Month.MARCO, Month.ABRIL, Month.MAIO, Month.SETEMBRO, Month.OUTUBRO),
            List.of(Month.DEZEMBRO, Month.JANEIRO)
        ));
        
        productRepository.save(new Product(
            "Banana",
            TemperatureCategory.AMENO,
            List.of(Month.MARCO, Month.ABRIL, Month.MAIO, Month.SETEMBRO, Month.OUTUBRO, Month.NOVEMBRO),
            List.of(Month.JULHO, Month.AGOSTO)
        ));
        
        // Produtos FRIOS (6-14°C)
        productRepository.save(new Product(
            "Batata Doce",
            TemperatureCategory.FRIO,
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.SETEMBRO),
            List.of(Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO)
        ));
        
        productRepository.save(new Product(
            "Mandioca",
            TemperatureCategory.FRIO,
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.SETEMBRO),
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.MARCO)
        ));
        
        // Produtos CONGELANDO (<=5°C)
        productRepository.save(new Product(
            "Gengibre",
            TemperatureCategory.CONGELANDO,
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO),
            List.of(Month.DEZEMBRO, Month.JANEIRO, Month.FEVEREIRO)
        ));
        
        log.info("Criados {} produtos de exemplo", productRepository.count());
    }
}