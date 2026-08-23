-- V9/V12/V13/V14 tentaram corrigir as linhas antigas com 'FAMÍLIA' (acentuado), mas nenhuma
-- funcionou de fato: `transactions.category` é um ENUM NATIVO do MySQL (não VARCHAR), criado
-- automaticamente pelo Hibernate a partir do enum Java Category na época em que a constante ainda
-- se chamava FAMÍLIA. O ddl-auto=update nunca reescreve a lista de valores permitidos de um ENUM já
-- existente quando o enum Java muda — então a coluna ficou congelada com 'FAMÍLIA' na lista e nunca
-- ganhou 'FAMILIA' como valor válido.
--
-- Isso significa que TODO `UPDATE ... SET category = 'FAMILIA'` contra essa coluna sempre falhou
-- com o erro do MySQL 1265 ("Data truncated for column") sob sql_mode estrito — reproduzido de forma
-- isolada (tabela temporária) antes de escrever esta migration. Não era um problema de
-- encoding/collation como V9/V12/V13/V14 suspeitavam: era a própria definição da coluna barrando
-- o valor certo.
--
-- Esta migration converte a coluna para VARCHAR (o que @Enumerated(EnumType.STRING) deveria gerar
-- por padrão) antes de tentar o UPDATE de novo — ver Transaction.java para o columnDefinition
-- explícito que evita o Hibernate recriar isso como ENUM nativo no futuro.
--
-- Guarda por existência da tabela pelo mesmo motivo de V9/V12/V13/V14.

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'transactions') > 0,
    'ALTER TABLE transactions MODIFY category VARCHAR(32) NOT NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt2 = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'transactions') > 0,
    'UPDATE transactions SET category = ''FAMILIA'' WHERE LENGTH(category) <> CHAR_LENGTH(category)',
    'SELECT 1'));
PREPARE stmt2 FROM @stmt2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
