package com.hortifruti.sl.hortifruti.repository.purchase;

import com.hortifruti.sl.hortifruti.model.purchase.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
  Optional<Client> findByEmail(String email);

  Optional<Client> findByClientName(String clientName);

  Optional<Client> findByClientNameIgnoreCase(String clientName);

  Optional<Client> findByNicknameIgnoreCase(String nickname);

  Optional<Client> findByDocument(String document);
}
