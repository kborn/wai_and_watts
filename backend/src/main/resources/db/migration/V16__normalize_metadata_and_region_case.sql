-- Phase 15 Data Normalization:
-- 1) Normalize metadata columns across domain tables.
-- 2) Normalize persisted LAWA region casing for historical rows.

ALTER TABLE mbie_generation_quarterly_record
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE lawa_state_multi_year_record
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE lawa_water_quality_trend_multi_year_record
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Backfill region normalization for legacy rows that may predate ingestion normalization.
UPDATE lawa_state_multi_year_record
SET region = LOWER(TRIM(region))
WHERE region IS NOT NULL;

UPDATE lawa_water_quality_trend_multi_year_record
SET region = LOWER(TRIM(region))
WHERE region IS NOT NULL;
