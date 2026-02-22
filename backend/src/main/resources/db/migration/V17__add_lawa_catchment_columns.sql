ALTER TABLE lawa_state_multi_year_record
    ADD COLUMN IF NOT EXISTS catchment TEXT;

ALTER TABLE lawa_water_quality_trend_multi_year_record
    ADD COLUMN IF NOT EXISTS catchment TEXT;
