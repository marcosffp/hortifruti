package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
  Optional<Client> findByEmail(String email);

  Optional<Client> findByClientName(String clientName);
}
