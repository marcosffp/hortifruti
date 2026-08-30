-- Tabela de preços do cliente (ex.: LLinea) por competência (mês/ano), importada de um CSV
-- oficial do cliente — ver TabelaPrecoClienteImportService. `versao` permite reimport de uma
-- competência já CONFIRMADA sem sobrescrever o histórico (cliente reenvia tabela corrigida).
CREATE TABLE IF NOT EXISTS tabelas_preco_cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    competencia_mes INT NOT NULL,
    competencia_ano INT NOT NULL,
    vigencia_inicio DATE NOT NULL,
    vigencia_fim DATE NOT NULL,
    versao INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    origem_arquivo_nome VARCHAR(255) NULL,
    importado_em DATETIME NOT NULL,
    importado_por BIGINT NULL,
    confirmado_em DATETIME NULL,
    confirmado_por BIGINT NULL,
    UNIQUE KEY uk_tabela_preco_cliente_competencia_versao
        (cliente_id, competencia_ano, competencia_mes, versao),
    INDEX idx_tabela_preco_cliente_vigencia (cliente_id, vigencia_inicio, vigencia_fim, status)
);

-- Uma linha por item do CSV do cliente dentro de uma tabela de preços. `preco` NULL = não cotado
-- esse mês (célula em branco no arquivo do cliente), nunca confundir com preço real 0.
-- `fiscal_product_id` só é considerado oficial quando status_match é CONFIRMADO/EDITADO_MANUALMENTE.
CREATE TABLE IF NOT EXISTS tabela_preco_cliente_itens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tabela_preco_cliente_id BIGINT NOT NULL,
    codigo_produto_cliente VARCHAR(100) NOT NULL,
    nome_produto_cliente VARCHAR(255) NOT NULL,
    preco NUMERIC(10,4) NULL,
    fiscal_product_id BIGINT NULL,
    confianca_matching DOUBLE NULL,
    status_match VARCHAR(30) NOT NULL,
    INDEX idx_tabela_preco_cliente_itens_tabela (tabela_preco_cliente_id),
    INDEX idx_tabela_preco_cliente_itens_fiscal_product (fiscal_product_id)
);

-- Memória persistente "De/Para" cliente->catálogo interno: uma vez confirmado por humano, o
-- próximo import do mesmo cliente aplica o vínculo automaticamente pra esse código, sem passar
-- por matching fuzzy de novo. Chave é o código do cliente (estável mês a mês), não o nome.
CREATE TABLE IF NOT EXISTS cliente_produto_mapeamento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    codigo_produto_cliente VARCHAR(100) NOT NULL,
    fiscal_product_id BIGINT NOT NULL,
    confirmado_em DATETIME NOT NULL,
    confirmado_por BIGINT NULL,
    UNIQUE KEY uk_cliente_produto_mapeamento (cliente_id, codigo_produto_cliente)
);
