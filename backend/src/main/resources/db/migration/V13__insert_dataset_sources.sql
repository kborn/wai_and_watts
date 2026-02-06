-- V15: Simple dataset source insertion (resolves missing dataset issue)
-- All dataset sources required for operator ingestion

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
UNION ALL
SELECT 
    gen_random_uuid(),
    'MBIE Electricity Generation (Fuel Type, Quarterly)',
    'MBIE',
    'https://www.mbie.govt.nz/assets/Data-Files/Energy/nz-energy-quarterly-and-energy-in-nz/electricity-sept-2025-q3-quarterly.xlsx',
    'XLSX',
    'quarterly',
    'mbie.generation.quarterly',
    CURRENT_TIMESTAMP
UNION ALL
SELECT 
    gen_random_uuid(),
    'LAWA River Water Quality State (Multi-Year)',
    'LAWA',
    'https://www.lawa.org.nz/media/ftmb4fjn/lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx',
    'XLSX',
    'annual',
    'lawa.water_quality.state.multi_year',
    CURRENT_TIMESTAMP
UNION ALL
SELECT 
    gen_random_uuid(),
    'LAWA River Water Quality Trend (Multi-Year)',
    'LAWA',
    'https://www.lawa.org.nz/media/ftmb4fjn/lawa-river-water-quality-state-and-trend-results_30oct2025-trend.xlsx',
    'XLSX',
    'annual',
    'lawa.water_quality.trend.multi_year',
    CURRENT_TIMESTAMP;