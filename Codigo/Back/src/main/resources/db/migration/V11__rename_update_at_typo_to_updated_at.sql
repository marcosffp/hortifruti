-- Renomeia a coluna "update_at" (typo, faltando o "d") para "updated_at" em statements,
-- fiscal_products, invoice_products e purchases — as 4 entidades que tinham esse typo isolado,
-- diferente do "updated_at" correto usado no resto do sistema (User, Client, LoginLockout, etc.).
-- RENAME COLUMN preserva os valores já gravados (sem precisar de backfill).
--
-- Guarda por existência da coluna antiga "update_at" (implica também checar a tabela — numa
-- consulta a INFORMATION_SCHEMA.COLUMNS, tabela inexistente só retorna zero linhas, sem erro).
-- Num banco novo, nenhuma das 4 tabelas tem esse typo: o Flyway roda antes do Hibernate criá-las,
-- e quando o Hibernate as cria, já usa "updated_at" (nome correto na entidade) direto.

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'statements' AND COLUMN_NAME = 'update_at') > 0,
    'ALTER TABLE statements RENAME COLUMN update_at TO updated_at',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'fiscal_products' AND COLUMN_NAME = 'update_at') > 0,
    'ALTER TABLE fiscal_products RENAME COLUMN update_at TO updated_at',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'invoice_products' AND COLUMN_NAME = 'update_at') > 0,
    'ALTER TABLE invoice_products RENAME COLUMN update_at TO updated_at',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'purchases' AND COLUMN_NAME = 'update_at') > 0,
    'ALTER TABLE purchases RENAME COLUMN update_at TO updated_at',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
