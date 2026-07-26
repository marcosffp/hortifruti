package com.hortifruti.sl.hortifruti.config.auth;

import com.hortifruti.sl.hortifruti.exception.auth.AccountLockedException;
import com.hortifruti.sl.hortifruti.model.LoginAuditLog;
import com.hortifruti.sl.hortifruti.model.LoginAuditLog.FailureReason;
import com.hortifruti.sl.hortifruti.model.LoginLockout;
import com.hortifruti.sl.hortifruti.model.LoginLockout.IdentifierType;
import com.hortifruti.sl.hortifruti.repository.LoginAuditLogRepository;
import com.hortifruti.sl.hortifruti.repository.LoginLockoutRepository;
import com.hortifruti.sl.hortifruti.service.notification.NotificationCoordinator;
import com.hortifruti.sl.hortifruti.service.notification.email.EmailTemplateService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proteção contra brute-force no login: contadores independentes por conta e por IP, lockout
 * progressivo (15min / 1h / 24h), reset automático após período limpo, e-mail de alerta throttled,
 * e log de auditoria de toda tentativa (bloqueada, falha ou sucesso). Tudo persistido no MySQL via
 * {@link LoginLockout}/{@link LoginAuditLog} — sem dependência de Redis, para sobreviver a
 * deploys/restarts em instância única.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LoginProtectionService {

  private static final String GENERIC_LOCK_MESSAGE =
      "Credenciais inválidas ou acesso temporariamente restrito. Tente novamente mais tarde.";

  private final LoginLockoutRepository loginLockoutRepository;
  private final LoginAuditLogRepository loginAuditLogRepository;
  private final NotificationCoordinator notificationCoordinator;
  private final EmailTemplateService emailTemplateService;

  @Value("${security.login.max-attempts:3}")
  private int maxAttempts;

  @Value("${security.login.lockout.level1-minutes:15}")
  private long level1Minutes;

  @Value("${security.login.lockout.level2-minutes:60}")
  private long level2Minutes;

  @Value("${security.login.lockout.level3-minutes:1440}")
  private long level3Minutes;

  @Value("${security.login.lockout.reset-after-days:7}")
  private long resetAfterDays;

  @Value("${security.login.alert.emails:}")
  private String alertEmails;

  @Value("${security.login.alert.throttle-minutes:30}")
  private long alertThrottleMinutes;

  /** Checa se a conta ou o IP já estão bloqueados. Se sim, audita a tentativa e lança. */
  public void assertNotLocked(String username, String ip, String userAgent) {
    LocalDateTime now = LocalDateTime.now();

    Optional<LoginLockout> accountLockout =
        loginLockoutRepository.findByIdentifierTypeAndIdentifier(IdentifierType.ACCOUNT, username);
    if (isLocked(accountLockout, now)) {
      saveAudit(username, ip, userAgent, false, FailureReason.ACCOUNT_LOCKED);
      throw lockedException(accountLockout.get(), now);
    }

    Optional<LoginLockout> ipLockout =
        loginLockoutRepository.findByIdentifierTypeAndIdentifier(IdentifierType.IP, ip);
    if (isLocked(ipLockout, now)) {
      saveAudit(username, ip, userAgent, false, FailureReason.IP_LOCKED);
      throw lockedException(ipLockout.get(), now);
    }
  }

  @Transactional
  public void registerFailure(String username, String ip, String userAgent, boolean accountExists) {
    LocalDateTime now = LocalDateTime.now();
    FailureReason reason =
        accountExists ? FailureReason.BAD_PASSWORD : FailureReason.USER_NOT_FOUND;

    if (accountExists) {
      registerAttempt(IdentifierType.ACCOUNT, username, username, ip, userAgent, now);
    }
    registerAttempt(IdentifierType.IP, ip, username, ip, userAgent, now);

    saveAudit(username, ip, userAgent, false, reason);
  }

  @Transactional
  public void registerSuccess(String username, String ip, String userAgent) {
    resetLockout(IdentifierType.ACCOUNT, username);
    resetLockout(IdentifierType.IP, ip);
    saveAudit(username, ip, userAgent, true, null);
  }

  private void registerAttempt(
      IdentifierType type,
      String identifier,
      String username,
      String ip,
      String userAgent,
      LocalDateTime now) {
    LoginLockout lockout = getOrCreate(type, identifier);

    if (lockout.getLastAttemptAt() != null
        && lockout.getLockedUntil() == null
        && lockout.getLastAttemptAt().isBefore(now.minusDays(resetAfterDays))) {
      lockout.setFailedAttempts(0);
      lockout.setLockoutLevel(0);
    }

    lockout.setFailedAttempts(lockout.getFailedAttempts() + 1);
    lockout.setLastAttemptAt(now);

    boolean justLocked = false;
    if (lockout.getFailedAttempts() >= maxAttempts
        && (lockout.getLockedUntil() == null || !lockout.getLockedUntil().isAfter(now))) {
      int newLevel = Math.min(lockout.getLockoutLevel() + 1, 3);
      lockout.setLockoutLevel(newLevel);
      lockout.setLockedUntil(now.plusMinutes(durationForLevel(newLevel)));
      lockout.setFailedAttempts(0);
      justLocked = true;
    }

    loginLockoutRepository.save(lockout);

    if (justLocked) {
      maybeSendAlert(lockout, username, ip, userAgent, now);
    }
  }

  private LoginLockout getOrCreate(IdentifierType type, String identifier) {
    return loginLockoutRepository
        .findByIdentifierTypeAndIdentifier(type, identifier)
        .orElseGet(
            () -> {
              try {
                return loginLockoutRepository.save(
                    LoginLockout.builder().identifierType(type).identifier(identifier).build());
              } catch (DataIntegrityViolationException e) {
                return loginLockoutRepository
                    .findByIdentifierTypeAndIdentifier(type, identifier)
                    .orElseThrow(() -> e);
              }
            });
  }

  private void resetLockout(IdentifierType type, String identifier) {
    loginLockoutRepository
        .findByIdentifierTypeAndIdentifier(type, identifier)
        .ifPresent(
            lockout -> {
              lockout.setFailedAttempts(0);
              lockout.setLockoutLevel(0);
              lockout.setLockedUntil(null);
              loginLockoutRepository.save(lockout);
            });
  }

  private boolean isLocked(Optional<LoginLockout> lockout, LocalDateTime now) {
    return lockout.isPresent()
        && lockout.get().getLockedUntil() != null
        && lockout.get().getLockedUntil().isAfter(now);
  }

  private AccountLockedException lockedException(LoginLockout lockout, LocalDateTime now) {
    long retryAfterSeconds = Duration.between(now, lockout.getLockedUntil()).getSeconds();
    return new AccountLockedException(GENERIC_LOCK_MESSAGE, Math.max(retryAfterSeconds, 0));
  }

  private long durationForLevel(int level) {
    return switch (level) {
      case 1 -> level1Minutes;
      case 2 -> level2Minutes;
      default -> level3Minutes;
    };
  }

  private void maybeSendAlert(
      LoginLockout lockout, String username, String ip, String userAgent, LocalDateTime now) {
    if (alertEmails == null || alertEmails.isBlank()) {
      return;
    }
    if (lockout.getLastEmailSentAt() != null
        && lockout.getLastEmailSentAt().isAfter(now.minusMinutes(alertThrottleMinutes))) {
      return;
    }

    lockout.setLastEmailSentAt(now);
    loginLockoutRepository.save(lockout);

    Map<String, String> variables = new HashMap<>();
    variables.put(
        "IDENTIFIER_TYPE",
        lockout.getIdentifierType() == IdentifierType.ACCOUNT ? "Conta" : "Endereço IP");
    variables.put("IDENTIFIER", lockout.getIdentifier());
    variables.put("USERNAME", username);
    variables.put("IP", ip);
    variables.put("USER_AGENT", userAgent);
    variables.put("TIMESTAMP", now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    variables.put("ATTEMPT_COUNT", String.valueOf(maxAttempts));
    variables.put("LOCKOUT_LEVEL", String.valueOf(lockout.getLockoutLevel()));
    variables.put("LOCKOUT_DURATION", formatDuration(durationForLevel(lockout.getLockoutLevel())));

    String subject = "Alerta de segurança: bloqueio de login ativado";
    String body = emailTemplateService.processTemplate("login-lockout-alert", variables);

    for (String email : alertEmails.split(",")) {
      String trimmed = email.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      try {
        notificationCoordinator.sendEmailOnly(trimmed, subject, body, null, null);
      } catch (Exception e) {
        log.error("Erro ao enviar alerta de bloqueio de login para {}", trimmed, e);
      }
    }
  }

  private String formatDuration(long minutes) {
    if (minutes % (24 * 60) == 0) {
      long days = minutes / (24 * 60);
      return days + (days == 1 ? " dia" : " dias");
    }
    if (minutes % 60 == 0) {
      long hours = minutes / 60;
      return hours + (hours == 1 ? " hora" : " horas");
    }
    return minutes + " minutos";
  }

  private void saveAudit(
      String username, String ip, String userAgent, boolean success, FailureReason reason) {
    loginAuditLogRepository.save(
        LoginAuditLog.builder()
            .username(username)
            .ipAddress(ip)
            .userAgent(userAgent)
            .success(success)
            .failureReason(reason)
            .build());
  }
}
