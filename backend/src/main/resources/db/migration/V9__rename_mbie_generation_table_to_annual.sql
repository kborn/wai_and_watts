-- V9: Rename MBIE generation table to variant-explicit annual name
-- From: mbie_generation_record
-- To:   mbie_generation_annual_record

-- H2 and Postgres both support simple table rename
ALTER TABLE mbie_generation_record RENAME TO mbie_generation_annual_record;
