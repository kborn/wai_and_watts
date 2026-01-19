-- V3: Add created_at columns to lineage tables

-- Add created_at to dataset_source
ALTER TABLE dataset_source
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add created_at to dataset_release
ALTER TABLE dataset_release
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
