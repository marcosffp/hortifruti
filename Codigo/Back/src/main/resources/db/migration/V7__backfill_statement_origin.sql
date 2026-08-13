-- Backfill: marca extratos existentes (anteriores à coluna "origin") como PDF_UPLOAD.
-- Não destrutivo: só preenche linhas com origin ainda NULL.
--
-- Guarda por existência da tabela E da coluna "origin" (coluna criada pelo Hibernate
-- ddl-auto=update, não por nenhuma migration — num banco novo nenhuma das duas existe ainda
-- quando o Flyway roda; nesse caso não há nada a fazer, já que um banco novo não tem statements
-- pré-existentes pra corrigir).

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'statements' AND COLUMN_NAME = 'origin') > 0,
    'UPDATE statements SET origin = ''PDF_UPLOAD'' WHERE origin IS NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
