-- Amplia google_oauth_tokens.encrypted_value para LONGBLOB para corrigir "Data too long for
-- column 'encrypted_value'" ao salvar credenciais OAuth do Google.
--
-- A tabela é normalmente auto-criada pelo Hibernate (ddl-auto=update), mas ddl-auto=update nunca
-- amplia uma coluna BLOB-family já existente: o MySQL reporta TINYBLOB/BLOB/MEDIUMBLOB/LONGBLOB
-- sob o mesmo tipo JDBC genérico, então o diff de schema do Hibernate não consegue diferenciá-los
-- e nunca emite um ALTER, mesmo com a entidade já declarando LONGBLOB.
--
-- Guarda por existência da tabela (ver V2 para o motivo: bootstrap num banco novo roda o Flyway
-- antes do Hibernate criar "google_oauth_tokens" — nesse caso a tabela já nasce com LONGBLOB,
-- direto da entidade, sem precisar deste ALTER).

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'google_oauth_tokens') > 0,
    'ALTER TABLE google_oauth_tokens MODIFY COLUMN encrypted_value LONGBLOB NOT NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
