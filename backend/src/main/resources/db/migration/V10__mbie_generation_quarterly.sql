-- V10: Create MBIE quarterly generation domain table

CREATE TABLE IF NOT EXISTS mbie_generation_quarterly_record (
    id BIGSERIAL PRIMARY KEY,
    dataset_release_id UUID NOT NULL REFERENCES dataset_release (id) ON DELETE CASCADE,
    period_year INT NOT NULL,
    period_quarter INT NOT NULL,
    fuel_type_raw TEXT NOT NULL,
    fuel_type_norm TEXT NOT NULL,
    generation_gwh NUMERIC(18,3) NOT NULL
);

-- Ensure uniqueness per release / period / source
-- Per policy: uniqueness at raw fuel level (matches annual table semantics)
CREATE UNIQUE INDEX IF NOT EXISTS uq_mbie_quarterly_release_period_source
    ON mbie_generation_quarterly_record (dataset_release_id, period_year, period_quarter, fuel_type_raw);

-- Read-side helper index
CREATE INDEX IF NOT EXISTS ix_mbie_quarterly_period_source
    ON mbie_generation_quarterly_record (period_year, period_quarter, fuel_type_norm);
