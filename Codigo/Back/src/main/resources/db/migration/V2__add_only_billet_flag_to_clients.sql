-- Adiciona a flag "somente boleto" (cliente sem nota fiscal) em clients.
-- Default FALSE preserva o comportamento atual (boleto + nota fiscal) para clientes existentes.
--
-- Guarda por existência da tabela: num banco novo (setup local do zero), o Flyway roda antes do
-- Hibernate ddl-auto=update criar as tabelas das entidades — "clients" ainda não existe nesse
-- momento. Sem essa guarda o migrate() falharia logo no boot; com ela, vira no-op e o Hibernate
-- cria a tabela já com a coluna certa (a entidade Client já declara only_billet).

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'clients') > 0,
    'ALTER TABLE clients ADD COLUMN only_billet BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
