# Phase 8 Fixture Generation — LAWA State (Multi-Year)

_*Note!  Historical doc. Used to create early fixtures but now not the current ingestion path. Replaced by scripts/transform.sh_

- Dataset Source Code: `lawa.water_quality.state.multi_year`
- Workbook: LAWA River Water Quality State download
- Sheet: `State Attribute Band`

## Purpose

The Phase 8 fixture is a small, deterministic slice of the LAWA workbook used to validate the end-to-end ingestion lifecycle
without requiring live downloads (live ingestion is deferred to Phase 10).

## Script

Use `scripts/normalize_lawa_state_attribute_band.py` to generate the fixture CSV.

The script:
- filters to a stable subset of regions and a deterministic set of sites per region
- preserves `indicator_raw` as published
- derives:
  - `indicator_norm` via a small, stable mapping table
  - `state_norm` from `Attribute Band` (A..E)
  - period fields from `hYear` per the Phase 8 contract

## Period contract (Phase 8)

For `hYear = Y`:
- `period_type = HYDRO_5YR_ROLLING`
- `period_start_year = Y - 5`
- `period_end_year = Y`

## Output

The script writes a CSV with the exact header expected by the Phase 8 schema.
