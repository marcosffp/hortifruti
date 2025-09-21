package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.model.FreightConfig;
import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.enumeration.Role;
import com.hortifruti.sl.hortifruti.repository.FreightConfigRepository;
import com.hortifruti.sl.hortifruti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final FreightConfigRepository freightConfigRepository;

  @Override
  public void run(String... args) throws Exception {
    initializeUsers();
    initializeFreightConfig();
  }

  // Inicializa os usuários padrão
  private void initializeUsers() {
    if (userRepository.count() == 0) {
      createUser("root", "root", Role.MANAGER);
      createUser("admin", "admin", Role.EMPLOYEE);
    }
  }

  // Cria um usuário com os dados fornecidos
  private void createUser(String username, String password, Role role) {
    User user =
        User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .role(role)
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
