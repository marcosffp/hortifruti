-- Peso de referência (kg) de uma caixa de cada produto, cadastrado/mantido pelo dono da loja
-- (import via CSV — ver ConversaoCaixaImportService) e usado pra converter determinísticamente
-- itens em caixa (CX) pra kg na extração de nota (GeminiExtractionService/ConversaoCaixaService),
-- em vez de deixar o Gemini "chutar" um peso a cada chamada.
--
-- Guarda por existência da tabela: num banco novo (setup local do zero), o Flyway roda antes do
-- Hibernate ddl-auto=update criar as tabelas das entidades — "fiscal_products" ainda não existe
-- nesse momento. Sem essa guarda o migrate() falharia logo no boot; com ela, vira no-op e o
-- Hibernate cria a tabela já com as colunas certas (a entidade FiscalProduct já as declara).

SET @dbname = DATABASE();

SET @stmt = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'fiscal_products') > 0,
    'ALTER TABLE fiscal_products
       ADD COLUMN peso_caixa_kg NUMERIC(10,3) NULL,
       ADD COLUMN peso_caixa_kg_atualizado_em DATETIME NULL',
    'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Histórico de mudanças de peso de caixa por produto (útil se um fornecedor mudar o padrão de
-- caixa, e pra dar rastro de cada import). Sem FK explícita, seguindo o padrão já usado no projeto
-- (ex.: billet_files.combined_score_id) — só BIGINT + índice.
CREATE TABLE IF NOT EXISTS product_box_weight_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fiscal_product_id BIGINT NOT NULL,
    peso_anterior NUMERIC(10,3) NULL,
    peso_novo NUMERIC(10,3) NOT NULL,
    origem VARCHAR(255) NOT NULL,
    criado_em DATETIME NOT NULL,
    INDEX idx_fiscal_product_id (fiscal_product_id)
);
