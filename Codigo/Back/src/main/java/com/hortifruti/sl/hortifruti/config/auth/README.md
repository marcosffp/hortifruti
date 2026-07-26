# com.hortifruti.sl.hortifruti.config.auth

Autenticação e autorização da API: login com JWT, proteção contra brute-force com lockout progressivo, rate limiting por IP/endpoint, filtro de segurança do Spring Security e ciclo de vida dos refresh tokens. Toda a API (exceto rotas públicas explícitas) passa por este pacote.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `Auth.java` | `@Component` | Orquestra o login: valida credenciais via `LoginProtectionService`/`PasswordEncoder`, gera o JWT em caso de sucesso e usa mensagem de erro genérica idêntica para usuário inexistente, senha incorreta e conta/IP bloqueados (evita enumeration attack). |
| `LoginProtectionService.java` | `@Component` | Brute-force protection: contadores independentes por conta e por IP, lockout progressivo (15min/1h/24h), reset automático após período limpo, e-mail de alerta com throttle, e auditoria de toda tentativa em `LoginAuditLog`/`LoginLockout` (persistido em MySQL, sem Redis). |
| `RateLimitingFilter.java` | `@Component` (`OncePerRequestFilter`) | Limita requisições por combinação IP+endpoint usando Bucket4j (10 requisições/minuto por bucket), retornando HTTP 429 quando excedido. |
| `RefreshTokenCleanupService.java` | `@Service` | Job agendado (`@Scheduled`, cron diário às 3h) que remove do banco os refresh tokens já expirados. |
| `RefreshTokenService.java` | `@Component` | Emissão e rotação de refresh tokens: cada uso revoga o token apresentado e emite um novo na mesma chamada; reuso de um token já revogado é tratado como vazamento e revoga todas as sessões ativas do usuário. |
| `SecurityConfig.java` | `@Configuration` (`@EnableWebSecurity`, `@EnableMethodSecurity`) | Define a `SecurityFilterChain`: CORS, CSRF desabilitado (API stateless), sessão stateless, lista de rotas públicas (`/auth/**`, Swagger, callback OAuth2 do Google) e regras de role por domínio (produtos/recomendações exigem MANAGER; notificações aceitam EMPLOYEE ou MANAGER exceto os endpoints de teste/diagnóstico; leitura de clientes/usuários aceita EMPLOYEE). Registra `RateLimitingFilter` e `SecurityFilter` na cadeia. |
| `SecurityFilter.java` | `@Component` (`OncePerRequestFilter`) | Valida a origem da requisição (defesa contra CSRF via cookie cross-site), extrai o token (header `Authorization` ou cookie `auth_token`) e popula o `SecurityContextHolder` a partir do JWT validado. |
| `TokenBlocklist.java` | `@Component` | Denylist em memória de JWTs revogados, usada para que `/auth/logout` invalide o token imediatamente. Válido apenas para deployment de instância única (perde estado em restart). |
| `TokenConfiguration.java` | `@Component` | Geração (HMAC256), validação e revogação de JWT (biblioteca auth0), incluindo claims `id` e `role`. Consulta o `TokenBlocklist` antes de validar. |
