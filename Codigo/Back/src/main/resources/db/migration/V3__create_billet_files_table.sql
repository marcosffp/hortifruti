-- Cria a tabela billet_files (armazenamento de PDF de boleto no R2).

CREATE TABLE IF NOT EXISTS billet_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    combined_score_id BIGINT NOT NULL,
    object_key VARCHAR(500) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    cancelled_at DATETIME NULL,
    INDEX idx_combined_score_id (combined_score_id),
    INDEX idx_combined_score_id_status (combined_score_id, status)
);
