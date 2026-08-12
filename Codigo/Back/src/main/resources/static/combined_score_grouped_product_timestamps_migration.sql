-- Migration: adiciona timestamps de auditoria (created_at/updated_at) em combined_scores e
-- grouped_product. As colunas são NOT NULL nas entidades (CombinedScore/GroupedProduct), então
-- não podem ser criadas diretamente como NOT NULL pelo ddl-auto=update em tabelas já populadas
-- (MySQL exige um default para preencher as linhas existentes).
--
-- Roda automaticamente em todo start da aplicação (spring.sql.init.schema-locations, em
-- application.properties) ANTES do Hibernate ddl-auto=update tentar criar essas colunas. MySQL
-- puro (diferente de MariaDB/Postgres) não suporta "ADD COLUMN IF NOT EXISTS" — o idioma padrão
-- pra ALTER condicional é checar INFORMATION_SCHEMA.COLUMNS e montar o ALTER TABLE dinamicamente
-- via PREPARE/EXECUTE. É seguro reexecutar em todo deploy: cada bloco vira no-op depois da 1ª vez.

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'combined_scores' AND COLUMN_NAME = 'created_at') = 0,
    'ALTER TABLE combined_scores ADD COLUMN created_at DATETIME NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'combined_scores' AND COLUMN_NAME = 'updated_at') = 0,
    'ALTER TABLE combined_scores ADD COLUMN updated_at DATETIME NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE combined_scores SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL;

ALTER TABLE combined_scores
    MODIFY COLUMN created_at DATETIME NOT NULL,
    MODIFY COLUMN updated_at DATETIME NOT NULL;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'grouped_product' AND COLUMN_NAME = 'created_at') = 0,
    'ALTER TABLE grouped_product ADD COLUMN created_at DATETIME NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'grouped_product' AND COLUMN_NAME = 'updated_at') = 0,
    'ALTER TABLE grouped_product ADD COLUMN updated_at DATETIME NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE grouped_product SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL;

ALTER TABLE grouped_product
    MODIFY COLUMN created_at DATETIME NOT NULL,
    MODIFY COLUMN updated_at DATETIME NOT NULL;
