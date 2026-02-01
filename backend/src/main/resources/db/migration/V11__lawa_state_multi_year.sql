-- V11: Create LAWA state multi-year domain table

CREATE TABLE IF NOT EXISTS lawa_state_multi_year_record (
    id BIGSERIAL PRIMARY KEY,
    dataset_release_id UUID NOT NULL REFERENCES dataset_release (id) ON DELETE CASCADE,
    lawa_site_id TEXT NOT NULL,
    site_name TEXT NOT NULL,
    region TEXT NOT NULL,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    indicator_raw TEXT NOT NULL,
    indicator_norm TEXT NOT NULL,
    units TEXT,
    attribute_band TEXT NOT NULL,
    state_norm TEXT NOT NULL,
    median NUMERIC(14,8),
    p95 NUMERIC(16,8),
    rec_health_exceed_260_pct NUMERIC(7,4),
    rec_health_exceed_540_pct NUMERIC(7,4),
    period_type TEXT NOT NULL,
    period_start_year INT NOT NULL,
    period_end_year INT NOT NULL
);

-- Ensure uniqueness per release / site / indicator / period
-- Per policy: uniqueness at raw indicator level
CREATE UNIQUE INDEX IF NOT EXISTS uq_lawa_state_multi_year
    ON lawa_state_multi_year_record (dataset_release_id, lawa_site_id, indicator_raw, period_type, period_end_year);

CREATE INDEX IF NOT EXISTS ix_lawa_state_year_indicator
    ON lawa_state_multi_year_record (period_end_year, indicator_norm, region);