-- V8: Backport taxonomy for Phase 6 dataset source code
-- Convention: <publisher>.<domain>.<variant>
-- Target: mbie.generation.annual

-- Try update by existing code (if known from earlier phases)
UPDATE dataset_source
SET code = 'mbie.generation.annual'
WHERE code IN ('mbie-generation', 'MBIE_ELECTRICITY_GENERATION_ANNUAL');

-- Fallback: by publisher if code matched an older placeholder
UPDATE dataset_source
SET code = 'mbie.generation.annual'
WHERE publisher = 'MBIE' AND code NOT LIKE 'mbie.generation.%';
