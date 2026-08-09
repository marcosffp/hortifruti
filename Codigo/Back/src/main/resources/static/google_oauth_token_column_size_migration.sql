-- Migration: Widen google_oauth_tokens.encrypted_value to LONGBLOB
-- Run this script once in your database (hml/prod) to fix "Data too long for column
-- 'encrypted_value'" errors when saving Google OAuth credentials.
--
-- The table is normally auto-created by Hibernate (ddl-auto=update), but ddl-auto=update never
-- widens an existing BLOB-family column: MySQL reports TINYBLOB/BLOB/MEDIUMBLOB/LONGBLOB under the
-- same generic JDBC type, so Hibernate's schema diff can't tell them apart and never emits an ALTER,
-- even after redeploys where the entity already declares LONGBLOB.

ALTER TABLE google_oauth_tokens MODIFY COLUMN encrypted_value LONGBLOB NOT NULL;
