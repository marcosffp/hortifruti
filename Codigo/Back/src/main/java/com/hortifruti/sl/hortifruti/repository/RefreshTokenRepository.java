package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.userId = :userId AND r.revokedAt IS"
          + " NULL")
  void revokeAllActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

  @Modifying
  @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :threshold")
  int deleteAllExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
