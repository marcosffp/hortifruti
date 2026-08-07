# com.hortifruti.sl.hortifruti.model

Pacote raiz das entidades JPA e enums de domínio. Contém entidades transversais (usuário, autenticação/lockout, configuração de frete) e enums compartilhados entre subpacotes, além das entidades específicas de cada domínio de negócio nas subpastas.

`package-info.java` define a convenção de modelagem do projeto: usar `@ManyToOne`/`@OneToMany` quando a entidade "pertence" a outra no mesmo domínio (ex.: `Purchase.client`), e usar uma FK crua (`Long xId`) quando a referência é fraca ou cross-domain (ex.: `CombinedScore.clientId`, `BilletFile.combinedScoreId`).

## Arquivos

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `User.java` | `@Entity` (`users`) | Usuário do sistema, implementa `UserDetails` (Spring Security). Campos: `username` (único), `password`, `position`, `role` (`@Enumerated` de `Role`). Gera `GrantedAuthority` como `ROLE_<role>`. |
| `Role.java` | Enum | Papéis de usuário: `MANAGER`, `EMPLOYEE`. Método `fromString` lança `IllegalArgumentException` se valor inválido/vazio. |
| `RefreshToken.java` | `@Entity` (`refresh_tokens`) | Token de refresh de sessão, referenciado por `userId` (FK crua, sem `@ManyToOne`). Guarda `tokenHash` (único), `expiresAt`, `revokedAt`. |
| `LoginAuditLog.java` | `@Entity` (`login_audit_logs`) | Log de auditoria de tentativas de login (sucesso/falha), indexado por `username`, `ip_address` e `created_at`. Enum interno `FailureReason` (`USER_NOT_FOUND`, `BAD_PASSWORD`, `ACCOUNT_LOCKED`, `IP_LOCKED`). |
| `LoginLockout.java` | `@Entity` (`login_lockouts`) | Estado de bloqueio por tentativas de login malsucedidas, por conta ou IP (`IdentifierType`: `ACCOUNT`, `IP`), com constraint única em `(identifier_type, identifier)`. Controla `failedAttempts`, `lockoutLevel` e `lockedUntil`. |
| `FreightConfig.java` | `@Entity` (`freight_config`) | Configuração de parâmetros para cálculo de custo de frete (consumo, preço de combustível, custos de manutenção/pneu/depreciação/seguro, salário base, encargos, margem, taxa fixa). |
| `FileStatus.java` | Enum | Status de arquivos armazenados (boletos, XMLs de nota fiscal): `ACTIVE`, `CANCELLED`. Usado por `model.billet.BilletFile` e `model.invoice.FiscalNoteXmlStorage`. |
| `package-info.java` | Documentação de pacote | Define a convenção de modelagem de relacionamentos (associação JPA vs. FK crua) usada em todo o pacote `model` e subpacotes. |

## Subpacotes

- `billet/` — arquivo de boleto (`BilletFile`) armazenado no Cloudflare R2.
- `climate/` — produto climático, categorias de temperatura e recomendação sazonal.
- `finance/` — extrato bancário, transação e enums de conciliação financeira.
- `googleauth/` — credencial OAuth2 do Google (Drive/Gmail) persistida no banco, criptografada em repouso.
- `invoice/` — armazenamento de XML/DANFE de nota fiscal.
- `notification/` — enums de canal, tipo e destinatário de notificações.
- `purchase/` — cliente, compra, produtos de nota fiscal e agrupamento de pontuação combinada (boleto + nota fiscal).
