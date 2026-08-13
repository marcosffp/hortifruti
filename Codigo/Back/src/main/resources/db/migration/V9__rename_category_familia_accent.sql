-- Renomeia o valor do enum Category.FAMÍLIA (único valor acentuado) para FAMILIA.
-- Category é persistido como EnumType.STRING em transactions.category, então linhas antigas com
-- o literal "FAMÍLIA" passam a não bater com Category.valueOf(...) depois do deploy da versão
-- com o enum renomeado.
--
-- Guarda por existência da tabela (ver V2 para o motivo: bootstrap num banco novo roda o Flyway
-- antes do Hibernate criar "transactions" — nesse caso não há linhas antigas pra corrigir).

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'transactions') > 0,
    'UPDATE transactions SET category = ''FAMILIA'' WHERE category = ''FAMÍLIA''',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
