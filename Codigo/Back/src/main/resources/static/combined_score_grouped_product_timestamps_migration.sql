-- Migration: adiciona timestamps de auditoria (created_at/updated_at) em combined_scores e
-- grouped_product. As colunas são NOT NULL nas entidades (CombinedScore/GroupedProduct), então
-- não podem ser criadas diretamente como NOT NULL pelo ddl-auto=update em tabelas já populadas
-- (MySQL exige um default para preencher as linhas existentes).
--
-- Roda automaticamente em todo start da aplicação (spring.sql.init.schema-locations, em
-- application.properties) ANTES do Hibernate ddl-auto=update tentar criar essas colunas — daí
-- os "IF NOT EXISTS"/"WHERE ... IS NULL": é seguro reexecutar em todo deploy, vira no-op depois
-- da 1ª vez.

ALTER TABLE combined_scores
    ADD COLUMN IF NOT EXISTS created_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

UPDATE combined_scores SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL;

ALTER TABLE combined_scores
    MODIFY COLUMN created_at DATETIME NOT NULL,
    MODIFY COLUMN updated_at DATETIME NOT NULL;

ALTER TABLE grouped_product
    ADD COLUMN IF NOT EXISTS created_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

UPDATE grouped_product SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL;

ALTER TABLE grouped_product
    MODIFY COLUMN created_at DATETIME NOT NULL,
    MODIFY COLUMN updated_at DATETIME NOT NULL;
