package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "login_lockouts",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_login_lockouts_type_identifier",
            columnNames = {"identifier_type", "identifier"}))
public class LoginLockout {

  public enum IdentifierType {
    ACCOUNT,
    IP
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "identifier_type", nullable = false, length = 16)
  private IdentifierType identifierType;

  @Column(name = "identifier", nullable = false, length = 255)
  private String identifier;

  @Column(name = "failed_attempts", nullable = false)
  @Builder.Default
  private int failedAttempts = 0;

  @Column(name = "lockout_level", nullable = false)
  @Builder.Default
  private int lockoutLevel = 0;

  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  @Column(name = "last_attempt_at")
  private LocalDateTime lastAttemptAt;

  @Column(name = "last_email_sent_at")
  private LocalDateTime lastEmailSentAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
