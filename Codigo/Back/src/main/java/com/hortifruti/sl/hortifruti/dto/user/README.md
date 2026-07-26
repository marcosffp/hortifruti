# com.hortifruti.sl.hortifruti.dto.user

DTOs de autenticação e gestão de usuários do sistema: login, cadastro, edição e contagem de
usuários por papel (role).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `AuthRequest.java` | record | Payload de login: `username` e `password`, ambos `@NotBlank` com mensagens de validação customizadas. |
| `AuthUserResponse.java` | record | Resposta de autenticação: id, username, nome e lista de roles do usuário autenticado. |
| `UserRequest.java` | record | Payload de request para cadastro de usuário: username (`@NotBlank`), senha (`@NotBlank`, `@Size` entre 4 e 20 caracteres), cargo (`position`) e papel (`Role`, `@NotNull`). |
| `UserResponse.java` | record | Resposta com dados do usuário: id, username, cargo e papel (`Role`). |
| `UserUpdateRequest.java` | record | Payload de atualização de usuário: username, senha, cargo e papel — todos opcionais (sem validação `@NotBlank`/`@NotNull`), permitindo atualização parcial. |
| `UsersCountResponse.java` | record | Contagem de usuários por papel: total, gestores e funcionários. |
