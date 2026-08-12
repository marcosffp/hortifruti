-- Migration: renomeia a coluna "update_at" (typo, faltando o "d") para "updated_at" em
-- statements, fiscal_products, invoice_products e purchases — as 4 entidades que tinham esse
-- typo isolado, diferente do "updated_at" correto usado no resto do sistema (User, Client,
-- LoginLockout, etc.). RENAME COLUMN preserva os valores já gravados (sem precisar de backfill).
--
-- Roda automaticamente em todo start da aplicação (spring.sql.init.schema-locations, em
-- application.properties) ANTES do Hibernate ddl-auto=update — que, sem essa migration, criaria
-- uma coluna "updated_at" NOVA (a entidade já foi corrigida para esse nome), ficando com duas
-- colunas de timestamp na tabela em vez de uma renomeada. Idempotente via checagem em
-- INFORMATION_SCHEMA: só renomeia se "update_at" (nome antigo) ainda existir.

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
