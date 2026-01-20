-- V5: Add stable code to dataset_source for lookups (H2 + Postgres compatible)
ALTER TABLE dataset_source ADD COLUMN IF NOT EXISTS code TEXT;

-- Use a unique index for portability across H2/Postgres
CREATE UNIQUE INDEX IF NOT EXISTS uq_dataset_source_code_idx ON dataset_source(code);
