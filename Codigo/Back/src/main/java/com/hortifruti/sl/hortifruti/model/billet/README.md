# com.hortifruti.sl.hortifruti.model.billet

Entidade referente ao arquivo PDF de boleto (Sicoob) armazenado no Cloudflare R2.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BilletFile.java` | `@Entity` (`billet_files`) | Representa um arquivo de boleto armazenado, referenciado por `combinedScoreId` (FK crua para `model.purchase.CombinedScore`, indexada). Guarda `objectKey` (chave única no R2) e `status` (`@Enumerated` de `model.FileStatus`, default `ACTIVE` no `@PrePersist`), além de `createdAt`/`cancelledAt`. |
