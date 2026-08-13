-- Adiciona a coluna de armazenamento do DANFE (PDF) em fiscal_note_xml_storage.
-- Linhas criadas antes desta migration ficam com o campo NULL até o DANFE ser buscado e salvo sob
-- demanda (ver FiscalNoteXmlStorageService.saveDanfeIfAbsent).

ALTER TABLE fiscal_note_xml_storage
    ADD COLUMN danfe_object_key VARCHAR(500) NULL;
