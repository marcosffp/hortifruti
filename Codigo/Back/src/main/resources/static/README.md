# src/main/resources/static

Migrations SQL avulsas (a aplicação usa `spring.jpa.hibernate.ddl-auto=update`, então mudanças estruturais maiores/backfills são feitas via estes scripts) e imagens estáticas.

A maioria é executada uma única vez, manualmente, no banco de cada ambiente (hml/prod) — exceto as três marcadas "auto" abaixo, que são idempotentes (checam `INFORMATION_SCHEMA` via `PREPARE`/`EXECUTE` antes de alterar, ou `WHERE ... IS NULL`; viram no-op depois da 1ª vez) e rodam sozinhas em todo start via `spring.sql.init.schema-locations` (ver `application.properties`), antes do Hibernate `ddl-auto=update`. As demais não foram convertidas para esse mecanismo porque não são todas seguras para reexecução — ver item E-C1 no `AUDITORIA.md` sobre adotar uma ferramenta de migração de verdade (Flyway) para o conjunto todo.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `category_familia_accent_migration.sql` | Backfill SQL (auto) | Atualiza `transactions.category` de `'FAMÍLIA'` para `'FAMILIA'` (renomeação do valor do enum `Category`, que era o único acentuado). |
| `combined_score_grouped_product_timestamps_migration.sql` | Migration SQL (auto) | Adiciona `created_at`/`updated_at` (NOT NULL) em `combined_scores` e `grouped_product`, com backfill (`NOW()`) das linhas existentes antes de aplicar a constraint NOT NULL. |
| `update_at_typo_rename_migration.sql` | Migration SQL (auto) | Renomeia a coluna `update_at` (typo) para `updated_at` em `statements`, `fiscal_products`, `invoice_products` e `purchases`, preservando os valores já gravados. |
| `billet_files_migration.sql` | Migration SQL | Cria a tabela `billet_files` (armazenamento de PDF de boleto no R2): `object_key` único, `status` (ACTIVE por padrão) e vínculo com `combined_score_id`. |
| `client_only_billet_migration.sql` | Migration SQL | Adiciona a coluna `only_billet` (BOOLEAN, default FALSE) em `clients` — flag "somente boleto" (cliente sem nota fiscal), preservando o comportamento atual para clientes existentes. |
| `fiscal_note_danfe_r2_migration.sql` | Migration SQL | Adiciona `danfe_object_key` em `fiscal_note_xml_storage` para armazenar o PDF (DANFE) no R2; linhas antigas ficam com o campo NULL até o DANFE ser buscado e salvo sob demanda. |
| `fiscal_note_xml_storage_migration.sql` | Migration SQL | Cria a tabela `fiscal_note_xml_storage` (armazenamento do XML da NF-e), com `ref` único, dados do cliente/valor/data de emissão e o conteúdo XML (`xml_content`). |
| `fiscal_note_xml_storage_r2_migration.sql` | Migration SQL | Migra `fiscal_note_xml_storage` do armazenamento de blob no banco para o Cloudflare R2: torna `xml_content` opcional (fallback legado) e adiciona `object_key`, `status` e `cancelled_at`. |
| `google_oauth_token_column_size_migration.sql` | Migration SQL | Amplia `google_oauth_tokens.encrypted_value` para `LONGBLOB` — corrige erro "Data too long" ao salvar credenciais OAuth do Google, causado pelo ddl-auto=update nunca redimensionar uma coluna BLOB já existente. |
| `statement_origin_backfill.sql` | Backfill SQL | Preenche a coluna `origin` (criada automaticamente pelo Hibernate) com `PDF_UPLOAD` para extratos existentes antes da coluna existir; só afeta linhas com `origin` NULL. |
| `statements_r2_migration.sql` | Migration SQL | Migra `statements` do armazenamento de blob no banco (`file_path`) para o Cloudflare R2: torna `file_path` opcional (fallback legado) e adiciona `object_key`. |

## Subpacotes

- `images/` — imagens estáticas usadas nos templates de e-mail (ver `images/README.md`).
