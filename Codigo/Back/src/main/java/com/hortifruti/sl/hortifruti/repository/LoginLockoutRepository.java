package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.LoginLockout;
import com.hortifruti.sl.hortifruti.model.LoginLockout.IdentifierType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLockoutRepository extends JpaRepository<LoginLockout, Long> {
  Optional<LoginLockout> findByIdentifierTypeAndIdentifier(
      IdentifierType identifierType, String identifier);
}
