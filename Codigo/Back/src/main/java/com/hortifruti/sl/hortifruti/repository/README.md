# com.hortifruti.sl.hortifruti.repository

Repositórios Spring Data JPA de entidades transversais (frete, autenticação/usuário). Repositórios específicos de cada domínio de negócio ficam em subpacotes.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `FreightConfigRepository.java` | `JpaRepository<FreightConfig, Long>` | CRUD simples da configuração de frete; sem queries customizadas. |
| `LoginAuditLogRepository.java` | `JpaRepository<LoginAuditLog, Long>` | CRUD simples do log de auditoria de tentativas de login; sem queries customizadas. |
| `LoginLockoutRepository.java` | `JpaRepository<LoginLockout, Long>` | Entidade `LoginLockout`. `findByIdentifierTypeAndIdentifier(IdentifierType, String)` busca o bloqueio de login por tipo de identificador (ex: usuário/IP) e valor, usado no mecanismo de lockout após tentativas falhas. |
| `RefreshTokenRepository.java` | `JpaRepository<RefreshToken, Long>` | Entidade `RefreshToken`. `findByTokenHash(String)` localiza token por hash; `revokeAllActiveByUserId` (`@Modifying @Query`) revoga todos os refresh tokens ativos de um usuário; `deleteAllExpiredBefore` (`@Modifying @Query`) remove tokens expirados antes de uma data. |
| `UserRepository.java` | `JpaRepository<User, Long>` | Entidade `User`. `findByUsername(String)` busca usuário por login; `findByRole` (`@Query`) lista usuários por `Role`; `getUsersCount`/`getUsersCountByRole` (`@Query`) retornam contagens totais e por papel. |

## Subpacotes

- `billet/` — repositório de arquivos de boleto armazenados (ver billet/README.md).
- `chatbot/` — repositório de sessões de conversa do chatbot (ver chatbot/README.md).
- `climate/` — repositório de produtos por categoria de temperatura (ver climate/README.md).
- `finance/` — repositórios de extratos e transações bancárias (ver finance/README.md).
- `invoice/` — repositório de XML de notas fiscais armazenadas (ver invoice/README.md).
- `purchase/` — repositórios de clientes, compras, agrupamentos e produtos de nota (ver purchase/README.md).
