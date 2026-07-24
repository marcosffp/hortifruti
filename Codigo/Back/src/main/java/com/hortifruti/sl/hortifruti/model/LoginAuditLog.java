package com.hortifruti.sl.hortifruti.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
    name = "login_audit_logs",
    indexes = {
      @Index(name = "idx_login_audit_logs_username", columnList = "username"),
      @Index(name = "idx_login_audit_logs_ip_address", columnList = "ip_address"),
      @Index(name = "idx_login_audit_logs_created_at", columnList = "created_at")
    })
public class LoginAuditLog {

  public enum FailureReason {
    USER_NOT_FOUND,
    BAD_PASSWORD,
    ACCOUNT_LOCKED,
    IP_LOCKED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, length = 255)
  private String username;

  @Column(name = "ip_address", nullable = false, length = 64)
  private String ipAddress;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(name = "success", nullable = false)
  private boolean success;

  @Enumerated(EnumType.STRING)
  @Column(name = "failure_reason", length = 32)
  private FailureReason failureReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
