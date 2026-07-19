-- Migration: Move statements from DB blob storage to Cloudflare R2
-- Run this script once in your database (hml/prod).
-- Non-destructive: file_path is kept (now nullable) as a legacy fallback for extratos
-- uploaded before this migration; new rows use object_key instead.

ALTER TABLE statements
    MODIFY COLUMN file_path LONGBLOB NULL,
    ADD COLUMN object_key VARCHAR(500) NULL;
