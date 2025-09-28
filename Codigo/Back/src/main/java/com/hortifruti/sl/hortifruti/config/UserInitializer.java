package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.model.Product;
import com.hortifruti.sl.hortifruti.model.FreightConfig;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import com.hortifruti.sl.hortifruti.model.enumeration.Role;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;
import com.hortifruti.sl.hortifruti.repository.FreightConfigRepository;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ProductRepository productRepository;
  private final FreightConfigRepository freightConfigRepository;

  @Override
  public void run(String... args) throws Exception {
    initializeUsers();
    initializeFreightConfig();
  }

  // Inicializa os usuários padrão
  private void initializeUsers() {
    if (userRepository.count() == 0) {
      createUser("root", "root", Role.MANAGER, "Desenvolvedor");
      createUser("admin", "admin", Role.EMPLOYEE, "Administrador");
    }

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

  // Cria um usuário com os dados fornecidos
  private void createUser(String username, String password, Role role, String position) {
    User user =
        User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .role(role)
            .position(position)
            .build();
    userRepository.save(user);
    System.out.println("Usuário " + username + " criado com sucesso!");
  }

  // Inicializa a configuração de frete padrão
  private void initializeFreightConfig() {
    if (freightConfigRepository.count() == 0) {
      FreightConfig defaultConfig = createDefaultFreightConfig();
      freightConfigRepository.save(defaultConfig);
      System.out.println("Configuração de frete padrão criada com sucesso!");
    }
  }

  // Cria a configuração de frete padrão
  private FreightConfig createDefaultFreightConfig() {
    return FreightConfig.builder()
        .kmPerLiterConsumption(10.0)
        .fuelPrice(6.30)
        .maintenanceCostPerKm(0.15)
        .tireCostPerKm(0.04)
        .depreciationCostPerKm(0.53)
        .insuranceCostPerKm(0.14)
        .baseSalary(1600.00)
        .chargesPercentage(39.37)
        .monthlyHoursWorked(192.0)
        .administrativeCostsPercentage(15.0)
        .marginPercentage(20.0)
        .fixedFee(3.00)
        .build();
  }
}
