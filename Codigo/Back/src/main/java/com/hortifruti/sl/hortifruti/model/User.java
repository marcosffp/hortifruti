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
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "username"))
public class User implements UserDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * A restrição de unicidade é declarada em {@code @Table(uniqueConstraints=...)} acima com nome
   * explícito ({@code uk_users_username}), em vez de {@code @Column(unique = true)} deixando o
   * Hibernate gerar um nome hash — {@link
   * com.hortifruti.sl.hortifruti.exception.GlobalExceptionHandler} depende desse nome estável para
   * identificar a violação sem parsear texto de mensagem de driver.
   */
  @Column(nullable = false)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String position;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  /**
   * Marca contas cuja senha foi definida por outra pessoa (ex.: admin de bootstrap criado pelo
   * {@code UserInitializer}) e ainda não foi trocada pelo próprio usuário — {@code SecurityFilter}
   * bloqueia qualquer rota fora de {@code /auth/**}/{@code PUT /users/update} enquanto esta flag
   * for {@code true}.
   */
  @Builder.Default
  @Column(nullable = false)
  private boolean mustChangePassword = false;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }
}
