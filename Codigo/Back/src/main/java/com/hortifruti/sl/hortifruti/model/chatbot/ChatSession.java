package com.hortifruti.sl.hortifruti.model.chatbot;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "chat_session",
    indexes = @Index(name = "idx_chat_session_phone_number", columnList = "phone_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "phone_number", nullable = false, length = 20)
  private String phoneNumber;

  @Column(name = "client_id")
  private Long clientId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private SessionStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "context", length = 50)
  private SessionContext context;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "paused_until")
  private LocalDateTime pausedUntil;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    if (this.status == null) {
      this.status = SessionStatus.MENU;
    }
  }

  public boolean isPaused() {
    return this.pausedUntil != null && LocalDateTime.now().isBefore(this.pausedUntil);
  }

  public void pauseBot(int hours) {
    this.pausedUntil = LocalDateTime.now().plusHours(hours);
  }
}
