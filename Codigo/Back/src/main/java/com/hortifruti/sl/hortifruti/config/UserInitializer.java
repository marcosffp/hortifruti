package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.model.ClimateProduct;
import com.hortifruti.sl.hortifruti.model.FreightConfig;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.enumeration.Month;
import com.hortifruti.sl.hortifruti.model.enumeration.Role;
import com.hortifruti.sl.hortifruti.model.enumeration.TemperatureCategory;
import com.hortifruti.sl.hortifruti.repository.FreightConfigRepository;
import com.hortifruti.sl.hortifruti.repository.ProductRepository;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class UserInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ProductRepository productRepository;
  private final FreightConfigRepository freightConfigRepository;
  private final Base64FileDecoder base64FileDecoder;

  @Override
  public void run(String... args) throws Exception {
    decodeBase64Files(); // Decodifica os arquivos Base64 primeiro
    initializeUsers();
    initializeFreightConfig();
  }

  // Decodifica os arquivos Base64 necessários
  private void decodeBase64Files() {
    try {
      log.info("Decodificando arquivos Base64...");
      base64FileDecoder.decodeGoogleDriveCredentials();
      base64FileDecoder.decodePfx();
      log.info("Arquivos Base64 decodificados com sucesso!");
    } catch (Exception e) {
      log.error("Erro ao decodificar arquivos Base64: ", e);
    }
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
    // Produtos FRIOS (6-14°C)
    productRepository.save(
        new ClimateProduct(
            "COUVE",
            TemperatureCategory.FRIO,
            List.of(
                Month.ABRIL, Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.SETEMBRO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.OUTUBRO,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "CEBOLINHA",
            TemperatureCategory.FRIO,
            List.of(
                Month.ABRIL, Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.SETEMBRO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.OUTUBRO,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "MOSTARDA",
            TemperatureCategory.FRIO,
            List.of(Month.ABRIL, Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.SETEMBRO,
                Month.OUTUBRO,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "MANDIOCA",
            TemperatureCategory.FRIO,
            List.of(
                Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.SETEMBRO, Month.OUTUBRO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.ABRIL,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "ALMEIRÃO",
            TemperatureCategory.FRIO,
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.OUTUBRO,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "BATATA",
            TemperatureCategory.FRIO,
            List.of(Month.AGOSTO, Month.JUNHO, Month.JULHO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.OUTUBRO,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));

    productRepository.save(
        new ClimateProduct(
            "ABOBORA D'ÁGUA",
            TemperatureCategory.FRIO,
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.ABRIL,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));

    // Produtos QUENTES
    productRepository.save(
        new ClimateProduct(
            "AMEIXA",
            TemperatureCategory.QUENTE,
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.MARCO, Month.NOVEMBRO, Month.DEZEMBRO),
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO)));
    productRepository.save(
        new ClimateProduct(
            "KIWI",
            TemperatureCategory.QUENTE,
            List.of(
                Month.FEVEREIRO, Month.MARCO, Month.ABRIL, Month.MAIO, Month.JUNHO, Month.JULHO),
            List.of(Month.SETEMBRO, Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "UVA SEM SEMENTE",
            TemperatureCategory.QUENTE,
            List.of(Month.JANEIRO, Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO),
            List.of(Month.JULHO, Month.JUNHO, Month.AGOSTO, Month.MAIO)));
    productRepository.save(
        new ClimateProduct(
            "QUIABO",
            TemperatureCategory.QUENTE,
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.ABRIL,
                Month.MAIO,
                Month.SETEMBRO,
                Month.OUTUBRO,
                Month.NOVEMBRO,
                Month.DEZEMBRO),
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO)));
    productRepository.save(
        new ClimateProduct(
            "MELÂNCIA",
            TemperatureCategory.QUENTE,
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.MARCO, Month.DEZEMBRO),
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.MAIO)));
    productRepository.save(
        new ClimateProduct(
            "MELÃO",
            TemperatureCategory.QUENTE,
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO),
            List.of(Month.MARCO, Month.ABRIL, Month.SETEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "MARACUJÁ",
            TemperatureCategory.QUENTE,
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO),
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO)));
    productRepository.save(
        new ClimateProduct(
            "ALFACE",
            TemperatureCategory.QUENTE,
            List.of(Month.JANEIRO, Month.SETEMBRO, Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO),
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO)));
    productRepository.save(
        new ClimateProduct(
            "REPOLHO",
            TemperatureCategory.QUENTE,
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.ABRIL,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "RUCULA",
            TemperatureCategory.QUENTE,
            List.of(Month.JANEIRO, Month.SETEMBRO, Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO),
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO)));

    productRepository.save(
        new ClimateProduct(
            "ÁGUA DE COCO",
            TemperatureCategory.QUENTE,
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.SETEMBRO, Month.DEZEMBRO),
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO, Month.OUTUBRO)));

    // Produtos AMENOS
    productRepository.save(
        new ClimateProduct(
            "LARANJA",
            TemperatureCategory.AMENO,
            List.of(Month.JUNHO, Month.JULHO, Month.MAIO, Month.AGOSTO),
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.MARCO, Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "LIMÃO",
            TemperatureCategory.AMENO,
            List.of(Month.JANEIRO, Month.SETEMBRO, Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO),
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO)));
    productRepository.save(
        new ClimateProduct(
            "TOMATE",
            TemperatureCategory.AMENO,
            List.of(Month.SETEMBRO, Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO),
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO, Month.AGOSTO)));
    productRepository.save(
        new ClimateProduct(
            "CENOURA",
            TemperatureCategory.AMENO,
            List.of(Month.MAIO, Month.JUNHO, Month.JULHO),
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.ABRIL,
                Month.NOVEMBRO,
                Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "COENTRO",
            TemperatureCategory.AMENO,
            List.of(
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.JULHO,
                Month.JULHO,
                Month.AGOSTO,
                Month.SETEMBRO),
            List.of(Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "MORANGO",
            TemperatureCategory.AMENO,
            List.of(Month.MAIO, Month.ABRIL, Month.JUNHO, Month.DEZEMBRO),
            List.of(Month.OUTUBRO, Month.NOVEMBRO, Month.JANEIRO, Month.FEVEREIRO, Month.MARCO)));
    productRepository.save(
        new ClimateProduct(
            "MEXERICA",
            TemperatureCategory.AMENO,
            List.of(Month.JUNHO, Month.JULHO, Month.MAIO),
            List.of(
                Month.NOVEMBRO,
                Month.DEZEMBRO,
                Month.JANEIRO,
                Month.FEVEREIRO,
                Month.MARCO,
                Month.ABRIL)));
    productRepository.save(
        new ClimateProduct(
            "OVO",
            TemperatureCategory.AMENO,
            List.of(Month.JANEIRO, Month.FEVEREIRO, Month.MARCO, Month.JUNHO),
            List.of(Month.OUTUBRO, Month.NOVEMBRO, Month.DEZEMBRO)));
    productRepository.save(
        new ClimateProduct(
            "MILHO VERDE",
            TemperatureCategory.AMENO,
            List.of(Month.JUNHO, Month.JULHO, Month.AGOSTO),
            List.of(Month.OUTUBRO, Month.ABRIL)));
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
