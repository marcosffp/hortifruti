-- Migration: Move fiscal_note_xml_storage from DB blob storage to Cloudflare R2
-- Run this script once in your database (hml/prod).
-- Non-destructive: xml_content is kept (now nullable) as a legacy fallback for XMLs
-- saved before this migration; new rows use object_key instead.

ALTER TABLE fiscal_note_xml_storage
    MODIFY COLUMN xml_content LONGTEXT NULL,
    ADD COLUMN object_key VARCHAR(500) NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN cancelled_at DATETIME NULL;
