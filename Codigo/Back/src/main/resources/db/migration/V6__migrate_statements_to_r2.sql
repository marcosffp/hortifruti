-- Move statements do armazenamento de blob no banco (file_path) para o Cloudflare R2.
-- Não destrutivo: file_path é mantido (agora opcional) como fallback legado para extratos
-- enviados antes desta migration; linhas novas usam object_key.
--
-- Guarda por existência da tabela (ver V2 para o motivo: bootstrap num banco novo roda o Flyway
-- antes do Hibernate criar "statements").

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'statements') > 0,
    'ALTER TABLE statements MODIFY COLUMN file_path LONGBLOB NULL, ADD COLUMN object_key VARCHAR(500) NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
