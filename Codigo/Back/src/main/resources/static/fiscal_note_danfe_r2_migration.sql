-- Migration: Add DANFE (PDF) storage column to fiscal_note_xml_storage
-- Run this script once in your database (hml/prod).
-- New rows will have danfe_object_key populated pointing to the R2 object;
-- rows created before this migration will have it NULL until the DANFE is
-- fetched/downloaded once, at which point it's lazily saved (see
-- FiscalNoteXmlStorageService.saveDanfeIfAbsent).

ALTER TABLE fiscal_note_xml_storage
    ADD COLUMN danfe_object_key VARCHAR(500) NULL;
