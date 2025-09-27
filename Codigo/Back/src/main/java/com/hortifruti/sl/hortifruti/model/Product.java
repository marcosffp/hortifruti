package com.hortifruti.sl.hortifruti.model;

import com.hortifruti.sl.hortifruti.model.climate_model.Month;
import com.hortifruti.sl.hortifruti.model.climate_model.TemperatureCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    //! Adicionei por causa do metodo findByNameContainingIgnoreCaseAndActiveTrue() em ProductRepository
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean active = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_category", nullable = false)
    private TemperatureCategory temperatureCategory;
    

    @Enumerated(EnumType.STRING)
    @ElementCollection(targetClass = Month.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "product_peak_months", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "peak_month")
    private List<Month> peakSalesMonths;
    

    @Enumerated(EnumType.STRING)
    @ElementCollection(targetClass = Month.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "product_low_months", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "low_month")
    private List<Month> lowSalesMonths;
    
    /**
     * Construtor para facilitar a criação de produtos
     */
    public Product(String name,
                   TemperatureCategory temperatureCategory, 
                   List<Month> peakSalesMonths, 
                   List<Month> lowSalesMonths) {
        this.name = name;
        this.active = true; // Por padrão, produtos são criados como ativos //!ProductRepository
        this.temperatureCategory = temperatureCategory;
        this.peakSalesMonths = peakSalesMonths;
        this.lowSalesMonths = lowSalesMonths;
    }
}