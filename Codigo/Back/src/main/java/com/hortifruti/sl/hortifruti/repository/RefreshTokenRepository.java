package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Usado só por {@code RefreshTokenService.rotate()} (sempre dentro de uma transação): trava a
   * linha (equivalente a {@code SELECT ... FOR UPDATE} no MySQL/InnoDB) para que duas chamadas
   * concorrentes de {@code POST /auth/refresh} apresentando o mesmo {@code refresh_token} (duas
   * abas, várias chamadas de API expirando juntas) nunca leiam a mesma linha com
   * {@code revokedAt == null} ao mesmo tempo — a segunda espera a primeira commitar e só então lê
   * o estado final (token já rotacionado), em vez de cada uma decidir de forma independente e
   * incorreta que o token ainda não foi usado.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
  Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

  @Modifying
  @Query(
      "UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.userId = :userId AND r.revokedAt IS"
          + " NULL")
  void revokeAllActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

  @Modifying
  @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :threshold")
  int deleteAllExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
