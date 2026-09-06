-- Adiciona o apelido opcional do cliente, usado só como termo alternativo de busca/identificação
-- (nunca em documentos oficiais, onde continua valendo client_name).
--
-- Guarda por existência da tabela: num banco novo (setup local do zero), o Flyway roda antes do
-- Hibernate ddl-auto=update criar as tabelas das entidades — "clients" ainda não existe nesse
-- momento. Sem essa guarda o migrate() falharia logo no boot; com ela, vira no-op e o Hibernate
-- cria a tabela já com a coluna certa (a entidade Client já declara nickname).

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'clients') > 0,
    'ALTER TABLE clients ADD COLUMN nickname VARCHAR(255) NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
