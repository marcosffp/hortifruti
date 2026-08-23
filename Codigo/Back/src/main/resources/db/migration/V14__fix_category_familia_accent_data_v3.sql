-- V12 (comparação por hex) e V13 (BINARY category NOT IN (...)) rodaram sem erro no Flyway, mas
-- não corrigiram nenhuma linha: o log da V13 mostrou um aviso do MySQL 9.4 ("'BINARY expr' is
-- deprecated and will be removed in a future release. Please use CAST instead"), indicando que
-- esse prefixo BINARY não está forçando comparação byte-a-byte nessa versão — a comparação seguiu
-- usando a collation padrão (accent-insensitive), então 'FAMÍLIA' continuou "igual" a 'FAMILIA' e
-- nunca caía no NOT IN.
--
-- Esta migration não compara strings: usa LENGTH() (bytes) vs CHAR_LENGTH() (caracteres). Para
-- 'FAMILIA' (só ASCII) os dois valem 7. Para 'FAMÍLIA' o Í ocupa 2 bytes em UTF-8, então
-- LENGTH()=8 e CHAR_LENGTH()=7 — divergem. É aritmética pura, imune a collation/charset.
--
-- Guarda por existência da tabela pelo mesmo motivo de V9/V12/V13.

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'transactions') > 0,
    'UPDATE transactions SET category = ''FAMILIA'' WHERE LENGTH(category) <> CHAR_LENGTH(category)',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
