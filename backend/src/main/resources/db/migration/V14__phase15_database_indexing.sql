-- Phase 15: Database & Indexing hardening
-- 1) Enforce annual MBIE row uniqueness at release/year/raw-fuel granularity.
-- 2) Align read indexes with API filter shapes after query pushdown.

CREATE UNIQUE INDEX IF NOT EXISTS uq_mbie_annual_release_period_source
    ON mbie_generation_annual_record (dataset_release_id, period_year, fuel_type_raw);

CREATE INDEX IF NOT EXISTS ix_mbie_annual_period_source
    ON mbie_generation_annual_record (period_year, fuel_type_norm);

DROP INDEX IF EXISTS idx_mbie_gen_release_period_source;

CREATE INDEX IF NOT EXISTS ix_lawa_state_read_region_indicator_period
    ON lawa_state_multi_year_record (region, indicator_norm, period_end_year, period_start_year);

DROP INDEX IF EXISTS ix_lawa_state_year_indicator;

CREATE INDEX IF NOT EXISTS ix_lawa_state_indicator_norm
    ON lawa_state_multi_year_record (indicator_norm);

CREATE INDEX IF NOT EXISTS ix_lawa_trend_read_region_indicator_period
    ON lawa_water_quality_trend_multi_year_record (region, indicator_norm, period_end_year, period_start_year);

CREATE INDEX IF NOT EXISTS ix_lawa_trend_indicator_norm
    ON lawa_water_quality_trend_multi_year_record (indicator_norm);
