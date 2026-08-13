-- Move fiscal_note_xml_storage do armazenamento de blob no banco para o Cloudflare R2.
-- Não destrutivo: xml_content é mantido (agora opcional) como fallback legado para XMLs salvos
-- antes desta migration; linhas novas usam object_key.

ALTER TABLE fiscal_note_xml_storage
    MODIFY COLUMN xml_content LONGTEXT NULL,
    ADD COLUMN object_key VARCHAR(500) NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN cancelled_at DATETIME NULL;
