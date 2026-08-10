# com.hortifruti.sl.hortifruti.controller.user

Endpoints de autenticação (login, refresh, logout, usuário autenticado) e de gestão de usuários do sistema.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `AuthController.java` | `@RestController` (`/auth`) | `POST /auth` autentica (`Auth.autenticar`) e define cookies HttpOnly de access token e refresh token; `GET /auth/me` retorna o usuário autenticado a partir do `SecurityContext`; `POST /auth/refresh` rotaciona o refresh token (via `RefreshTokenService.rotate`) e emite novo access token; `POST /auth/logout` revoga access token e refresh token e limpa os cookies. |
| `UserController.java` | `@RestController` (`/users`), `@PreAuthorize hasRole('MANAGER')` em todas as rotas | `POST /users/register` cadastra usuário; `GET /users/all` lista todos; `PUT /users` atualiza o usuário autenticado; `PUT /users/{id}` atualiza usuário por ID; `GET /users/count` retorna contagem de usuários; `DELETE /users/{username}` remove usuário por username. |
