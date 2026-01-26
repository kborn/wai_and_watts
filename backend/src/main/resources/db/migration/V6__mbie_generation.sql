-- Phase 6: MBIE electricity generation domain table
-- Annual-only period modeling per decisions.md

CREATE TABLE IF NOT EXISTS mbie_generation_record (
    id BIGSERIAL PRIMARY KEY,
    dataset_release_id UUID NOT NULL,
    period_year INT NOT NULL,
    fuel_type_raw TEXT NOT NULL,
    fuel_type_norm TEXT NOT NULL,
    generation_gwh NUMERIC(18,3) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_mbie_generation_release
        FOREIGN KEY (dataset_release_id)
        REFERENCES dataset_release(id)
        ON DELETE RESTRICT
);

-- Helpful index for lookups
CREATE INDEX IF NOT EXISTS idx_mbie_gen_release_period_source
    ON mbie_generation_record (dataset_release_id, period_year, fuel_type_norm);
