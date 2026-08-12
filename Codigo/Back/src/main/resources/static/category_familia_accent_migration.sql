-- Migration: renomeia o valor do enum Category.FAMÍLIA (único valor acentuado) para FAMILIA.
-- Category é persistido como EnumType.STRING em transactions.category, então linhas antigas com
-- o literal "FAMÍLIA" passam a não bater com Category.valueOf(...) depois do deploy da versão
-- com o enum renomeado.
--
-- Roda automaticamente em todo start da aplicação (spring.sql.init.schema-locations, em
-- application.properties) — depois da 1ª execução o WHERE nunca mais casa nenhuma linha, então é
-- seguro deixar rodando em todo deploy.

UPDATE transactions SET category = 'FAMILIA' WHERE category = 'FAMÍLIA';
