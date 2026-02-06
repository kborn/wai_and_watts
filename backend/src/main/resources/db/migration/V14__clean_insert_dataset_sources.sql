-- V14: Clean insert of dataset sources (replaces failed V13)
-- All dataset sources required for operator ingestion
-- Handles unique constraints properly

-- Insert MBIE annual if not exists
INSERT INTO dataset_source (id, name, publisher, source_url, expected_format, update_cadence, code, created_at)
SELECT 
    gen_random_uuid(),
    'MBIE Electricity Generation (Fuel Type, Annual)',
    'MBIE',
    'https://www.mbie.govt.nz/assets/Data-Files/Energy/nz-energy-quarterly-and-energy-in-nz/electricity-sept-2025-q3.xlsx',
    'XLSX',
    'quarterly',
    'mbie.generation.annual',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM dataset_source 
    WHERE code = 'mbie.generation.annual'
);

-- Insert MBIE quarterly if not exists (different URL to avoid unique constraint)
INSERT INTO dataset_source (id, name, publisher, source_url, expected_format, update_cadence, code, created_at)
SELECT 
    gen_random_uuid(),
    'MBIE Electricity Generation (Fuel Type, Quarterly)',
    'MBIE',
    'https://www.mbie.govt.nz/assets/Data-Files/Energy/nz-energy-quarterly-and-energy-in-nz/electricity-sept-2025-q3-quarterly.xlsx',
    'XLSX',
    'quarterly',
    'mbie.generation.quarterly',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM dataset_source 
    WHERE code = 'mbie.generation.quarterly'
);

-- Update existing LAWA records to use proper codes
UPDATE dataset_source 
SET 
    code = 'lawa.water_quality.state.multi_year',
    name = 'LAWA River Water Quality State (Multi-Year)',
    expected_format = 'XLSX',
    update_cadence = 'annual'
WHERE publisher = 'LAWA' AND code LIKE 'lawa%' AND code IS NOT NULL;

-- Insert LAWA trend with unique URL to avoid constraint violation
INSERT INTO dataset_source (id, name, publisher, source_url, expected_format, update_cadence, code, created_at)
SELECT 
    gen_random_uuid(),
    'LAWA River Water Quality Trend (Multi-Year)',
    'LAWA',
    'https://www.lawa.org.nz/media/ftmb4fjn/lawa-river-water-quality-state-and-trend-results_30oct2025-trend.xlsx',
    'XLSX',
    'annual',
    'lawa.water_quality.trend.multi_year',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM dataset_source 
    WHERE code = 'lawa.water_quality.trend.multi_year'
);