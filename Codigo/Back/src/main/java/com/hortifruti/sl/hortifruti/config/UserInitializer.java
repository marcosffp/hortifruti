package com.hortifruti.sl.hortifruti.config;

import com.hortifruti.sl.hortifruti.model.User;
import com.hortifruti.sl.hortifruti.model.enumeration.Role;
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

  @Override
  public void run(String... args) throws Exception {
    if (userRepository.count() == 0) {
      User rootUser =
          User.builder()
              .username("root")
              .password(passwordEncoder.encode("root"))
              .role(Role.MANAGER)
              .build();
      userRepository.save(rootUser);
      System.out.println("Usuário root criado com sucesso!");
      User adminUser =
          User.builder()
              .username("admin")
              .password(passwordEncoder.encode("admin"))
              .role(Role.EMPLOYEE)
              .build();
      userRepository.save(adminUser);
      System.out.println("Usuário admin criado com sucesso!");
    }
  }
}
