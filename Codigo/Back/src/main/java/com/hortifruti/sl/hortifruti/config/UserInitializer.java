package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.model.FreightConfig;
import com.hortifruti.sl.hortifruti.model.Role;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.climate.ClimateProduct;
import com.hortifruti.sl.hortifruti.model.climate.Month;
import com.hortifruti.sl.hortifruti.model.climate.TemperatureCategory;
import com.hortifruti.sl.hortifruti.repository.FreightConfigRepository;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import com.hortifruti.sl.hortifruti.repository.climate.ProductRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class UserInitializer implements CommandLineRunner {

  private static final String BOOTSTRAP_PASSWORD_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ProductRepository productRepository;
  private final FreightConfigRepository freightConfigRepository;
  private final Base64FileDecoder base64FileDecoder;
  private final Environment environment;

  @Override
  public void run(String... args) throws Exception {
    decodeBase64Files();
    initializeUsers();
    initializeFreightConfig();
    repopulateProductsIfNeeded();
  }

  private void repopulateProductsIfNeeded() {
    boolean needsRepopulation;
    try {
      needsRepopulation =
          productRepository.findAll().stream()
              .anyMatch(p -> p.getPeakSalesMonths() == null || p.getPeakSalesMonths().isEmpty());
    } catch (Exception e) {
      log.error("Falha ao ler produtos existentes (dados corrompidos?). Repopulando do zero...", e);
      needsRepopulation = true;
    }

    if (needsRepopulation) {
      log.info("Detectado produtos com listas vazias ou inválidas. Repopulando...");
      productRepository.deleteAllInBatch();
      createSampleProducts();
      log.info("Produtos repopulados com sucesso!");
    }
  }

  private void decodeBase64Files() {
    try {
      log.info("Decodificando arquivos Base64...");
      base64FileDecoder.decodeGoogleDriveCredentials();
      base64FileDecoder.decodePfx();
      base64FileDecoder.decodePem();
      log.info("Arquivos Base64 decodificados com sucesso!");
    } catch (Exception e) {
      log.error("Erro ao decodificar arquivos Base64: ", e);
    }
  }

  private void initializeUsers() {
    if (userRepository.count() == 0) {
      if (environment.matchesProfiles("local")) {
        createUser("root", "root", Role.MANAGER, "Desenvolvedor");
        createUser("admin", "admin", Role.EMPLOYEE, "Administrador");
      } else {
        createBootstrapManager();
      }
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

  private void createBootstrapManager() {
    String password = generateSecurePassword();
    createUser("admin", password, Role.MANAGER, "Administrador", true);
    // A senha só existe neste escopo local — não é logada, evitando que fique retida
    // indefinidamente em agregadores de log de terceiros. É entregue apenas via console, e a conta
    // fica com mustChangePassword=true: SecurityFilter bloqueia qualquer rota fora de /auth/** e
    // PUT /users/update até a senha ser trocada.
    System.out.println(
        "======================================================================");
    System.out.println("Nenhum usuário encontrado. Conta administrativa inicial criada.");
    System.out.println("  usuário: admin");
    System.out.println("  senha temporária (troca obrigatória no primeiro login): " + password);
    System.out.println(
        "======================================================================");
    log.warn(
        "Nenhum usuário encontrado. Conta administrativa inicial 'admin' criada com senha"
            + " temporária — veja a saída do console. Troca de senha obrigatória no primeiro"
            + " login.");
  }

  private String generateSecurePassword() {
    StringBuilder password = new StringBuilder(20);
    for (int i = 0; i < 20; i++) {
      password.append(
          BOOTSTRAP_PASSWORD_CHARS.charAt(
              SECURE_RANDOM.nextInt(BOOTSTRAP_PASSWORD_CHARS.length())));
    }
    return password.toString();
  }

  private void createUser(String username, String password, Role role, String position) {
    createUser(username, password, role, position, false);
  }

  private void createUser(
      String username, String password, Role role, String position, boolean mustChangePassword) {
    User user =
        User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .role(role)
            .position(position)
            .mustChangePassword(mustChangePassword)
            .build();
    userRepository.save(user);
  }

  private void initializeFreightConfig() {
    if (freightConfigRepository.count() == 0) {
      FreightConfig defaultConfig = createDefaultFreightConfig();
      freightConfigRepository.save(defaultConfig);
      log.info("Configuração de frete padrão criada com sucesso!");
    }
  }

  private FreightConfig createDefaultFreightConfig() {
    return FreightConfig.builder()
        .kmPerLiterConsumption(new BigDecimal("10.0"))
        .fuelPrice(new BigDecimal("6.30"))
        .maintenanceCostPerKm(new BigDecimal("0.15"))
        .tireCostPerKm(new BigDecimal("0.04"))
        .depreciationCostPerKm(new BigDecimal("0.53"))
        .insuranceCostPerKm(new BigDecimal("0.14"))
        .baseSalary(new BigDecimal("1600.00"))
        .chargesPercentage(new BigDecimal("39.37"))
        .monthlyHoursWorked(new BigDecimal("192.0"))
        .administrativeCostsPercentage(new BigDecimal("15.0"))
        .marginPercentage(new BigDecimal("20.0"))
        .fixedFee(new BigDecimal("3.00"))
        .build();
  }
}
