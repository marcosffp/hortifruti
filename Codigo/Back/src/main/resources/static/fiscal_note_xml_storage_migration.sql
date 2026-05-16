-- Migration: Create fiscal_note_xml_storage table
-- Run this script once in your database to enable XML fiscal note storage.

CREATE TABLE IF NOT EXISTS fiscal_note_xml_storage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ref VARCHAR(100) NOT NULL UNIQUE,
    nf_number VARCHAR(50) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    total_value DECIMAL(15, 2) NOT NULL,
    issued_at DATE NOT NULL,
    xml_content LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_issued_at (issued_at),
    INDEX idx_ref (ref)
);
