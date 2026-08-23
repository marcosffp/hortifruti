-- V12 rodou de verdade (Flyway agora está ativo — ver V13 vs a troca para spring-boot-starter-flyway
-- no pom.xml), mas o UPDATE não bateu em nenhuma linha mesmo assim: comparar por um literal exato
-- (acentuado ou em hex) contra a coluna não casou, provável pegadinha de padding/coleção de um
-- schema legado (ver comentário de V9 sobre os 11 scripts originais criados fora do Flyway). Em vez
-- de tentar acertar o valor "errado" byte a byte de novo, esta migration inverte a lógica: atualiza
-- qualquer linha cujo valor não seja um dos nomes de enum válidos — todos ASCII puro, então não tem
-- ambiguidade de encoding nem do lado esquerdo nem do direito da comparação.
--
-- Guarda por existência da tabela pelo mesmo motivo de V9/V12.

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'transactions') > 0,
    'UPDATE transactions SET category = ''FAMILIA'' WHERE BINARY category NOT IN (''VENDAS_CARTAO'', ''VENDAS_PIX'', ''SERVICOS_BANCARIOS'', ''FORNECEDOR'', ''FAMILIA'', ''FUNCIONARIO'', ''SERVICOS_TELEFONICOS'', ''CEMIG'', ''COPASA'', ''FISCAL'', ''IMPOSTOS'')',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
