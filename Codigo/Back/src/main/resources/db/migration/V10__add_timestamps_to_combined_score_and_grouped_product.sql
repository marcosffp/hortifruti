-- Adiciona timestamps de auditoria (created_at/updated_at) em combined_scores e grouped_product.
-- As colunas são NOT NULL nas entidades (CombinedScore/GroupedProduct), então não podem ser
-- criadas diretamente como NOT NULL pelo ddl-auto=update em tabelas já populadas (MySQL exige um
-- default para preencher as linhas existentes) — por isso adiciona como NULL, faz backfill (NOW())
-- e só então aplica a constraint NOT NULL.
--
-- Guarda por existência da tabela em cada bloco (ver V2 para o motivo: bootstrap num banco novo
-- roda o Flyway antes do Hibernate criar essas tabelas — nesse caso elas já nascem com as colunas
-- NOT NULL direto da entidade, sem linhas antigas pra popular).

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'combined_scores') > 0,
    'ALTER TABLE combined_scores ADD COLUMN created_at DATETIME NULL, ADD COLUMN updated_at DATETIME NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'combined_scores') > 0,
    'UPDATE combined_scores SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'combined_scores') > 0,
    'ALTER TABLE combined_scores MODIFY COLUMN created_at DATETIME NOT NULL, MODIFY COLUMN updated_at DATETIME NOT NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'grouped_product') > 0,
    'ALTER TABLE grouped_product ADD COLUMN created_at DATETIME NULL, ADD COLUMN updated_at DATETIME NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'grouped_product') > 0,
    'UPDATE grouped_product SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'grouped_product') > 0,
    'ALTER TABLE grouped_product MODIFY COLUMN created_at DATETIME NOT NULL, MODIFY COLUMN updated_at DATETIME NOT NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
