-- Phase 9: LAWA Water Quality Trend (Multi-Year)
CREATE TABLE IF NOT EXISTS lawa_water_quality_trend_multi_year_record (
    id BIGSERIAL PRIMARY KEY,
    dataset_release_id UUID NOT NULL REFERENCES dataset_release(id) ON DELETE CASCADE,
    lawa_site_id TEXT NOT NULL,
    site_name TEXT NOT NULL,
    region TEXT NOT NULL,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    indicator_raw TEXT NOT NULL,
    indicator_norm TEXT NOT NULL,
    units TEXT,
    trend_raw TEXT NOT NULL,
    trend_norm TEXT NOT NULL,
    trend_score SMALLINT NOT NULL,
    trend_period_years SMALLINT NOT NULL,
    trend_data_frequency TEXT,
    period_type TEXT NOT NULL,
    period_start_year INT NOT NULL,
    period_end_year INT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_lawa_trend_window
ON lawa_water_quality_trend_multi_year_record (dataset_release_id, lawa_site_id, indicator_norm, period_type, trend_period_years, period_end_year);
