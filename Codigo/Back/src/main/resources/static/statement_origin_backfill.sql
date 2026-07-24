-- Backfill: marca extratos existentes (antes da coluna "origin" existir) como PDF_UPLOAD.
-- Run this script once in your database (hml/prod) after the app has started at least once
-- with Hibernate ddl-auto=update (which creates the "origin" column automatically).
-- Non-destructive: only fills rows where origin is still NULL.

UPDATE statements SET origin = 'PDF_UPLOAD' WHERE origin IS NULL;
