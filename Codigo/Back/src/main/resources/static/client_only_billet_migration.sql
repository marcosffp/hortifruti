-- Migration: Add only_billet flag to clients
-- Run this script once in your database (hml/prod) to add the "somente boleto" flag.
-- Default FALSE preserves current behavior (boleto + nota fiscal) for existing clients.

ALTER TABLE clients ADD COLUMN only_billet BOOLEAN NOT NULL DEFAULT FALSE;
