# Phase 9 Fixture Generation — LAWA Trend (Multi-Year)

_*Note!  Historical doc. Used to create early fixtures but now not the current ingestion path. Replaced by scripts/transform.sh_

- Dataset Source Code: `lawa.water_quality.trend.multi_year`
- Workbook: `lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx`
- Sheet: `Trend`

## Purpose

The Phase 9 fixture is a deterministic slice of the LAWA Trend sheet used to validate the end-to-end ingestion lifecycle.
Trend complements Phase 8 State, enabling joinable facts for narrative and API usage.

## Script

Use `scripts/normalize_lawa_trend.py` to generate the fixture CSV.

The script:
- derives `as_of_year` from `MAX(hYear)` in the `State Attribute Band` sheet
- filters to a stable subset of regions and a deterministic set of sites per region
- preserves `indicator_raw` and `trend_raw` exactly as published
- derives `indicator_norm` and `trend_norm`
- derives period fields from `as_of_year` and `trend_period_years`

## Period contract (Phase 9)

- `period_type = HYDRO_NYR_WINDOW`
- `period_end_year = as_of_year` (derived from State sheet)
- `period_start_year = period_end_year - trend_period_years`
