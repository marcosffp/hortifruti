-- V9 tentou fazer esse mesmo fix, mas nunca rodou de verdade em hml/prod: baseline-version=11
-- (application.properties) faz o Flyway marcar tudo até a versão 11 como "já aplicada" sem
-- reexecutar nada em bancos pré-existentes (ver comentário em application.properties sobre
-- baseline-on-migrate). Como V9 = versão 9 (<= 11), ela foi baselineada e pulada silenciosamente,
-- então linhas antigas com o literal "FAMÍLIA" continuaram quebrando Category.valueOf(...) no
-- /dashboard. V12 está acima da baseline, então roda de verdade no próximo deploy.
--
-- Guarda por existência da tabela pelo mesmo motivo de V9 (bootstrap num banco novo roda o
-- Flyway antes do Hibernate criar "transactions").

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'transactions') > 0,
    'UPDATE transactions SET category = ''FAMILIA'' WHERE CAST(category AS BINARY) = 0x46414DC38D4C4941',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
