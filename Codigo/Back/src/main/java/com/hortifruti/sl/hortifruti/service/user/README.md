# com.hortifruti.sl.hortifruti.service.user

Cadastro e administração de usuários do sistema (papéis MANAGER/EMPLOYEE), incluindo validação de senha e nome de usuário único. Autenticação/geração de JWT ficam em outro pacote (`config`/`security`); este serviço cuida apenas do CRUD de `User`.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `UserService.java` | `@Service` | Cria usuário com senha codificada (`PasswordEncoder`); atualiza usuário por username ou por ID (valida tamanho de senha entre 4-20 caracteres, unicidade de username ao trocar); exclui por username; lista usuários por papel ou todos; retorna contagem total de usuários e por papel (`UsersCountResponse`). |
