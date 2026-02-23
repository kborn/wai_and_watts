# Phase 9 — LAWA Trend (Multi-Year) Ingestion Spec

## Dataset Identity

- **Taxonomy Form:** v2 (`<publisher>.<domain>.<dataset>.<variant>`)
- **Dataset Source Code:** `lawa.water_quality.trend.multi_year`
- **Source Workbook:** `lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx`
- **Sheet:** `Trend`

---

## Purpose

Phase 9 ingests LAWA river water quality **trend** results to complement Phase 8 “state” results.
Together, Phase 8 + Phase 9 enable grounded storytelling like:

- “State is poor AND trend is degrading”
- “State is poor BUT trend is improving”

---

## Period Semantics (Decision)

The Trend sheet provides a window length (`TrendPeriod (year)`), but not an explicit end-year per row.

Phase 9 contract:
- `as_of_year = MAX(hYear)` from `State Attribute Band` in the same workbook
- `period_type = 'HYDRO_NYR_WINDOW'`
- `period_end_year = as_of_year`
- `period_start_year = as_of_year - trend_period_years`

For the 30 Oct 2025 export, `as_of_year = 2024`.

---

## Scope

- Ingest LAWA trend results from sheet `Trend`
- Preserve raw labels (`indicator_raw`, `trend_raw`) for auditability
- Normalize `indicator_norm` and `trend_norm` for stable querying
- Store LAWA-published `trend_score`, `trend_period_years`, and `trend_data_frequency`
- Full lineage tracking via `dataset_source` + `dataset_release`

---

## Non-Goals

- Computing trends from raw measurements
- Converting trend scores into numeric slopes or p-values (not published in this export)
- Joining state + trend at ingestion time
- Live download pipeline changes (can remain fixture-first here; live ingestion is handled in a later phase)

---

## Required Field Mapping (Sheet → Domain)

| Sheet Column | Domain Column |
|---|---|
| `LawaSiteID` | `lawa_site_id` |
| `SiteID` | `site_name` |
| `Region` | `region` |
| `Latitude` | `latitude` |
| `Longitude` | `longitude` |
| `Indicator` | `indicator_raw` |
| (normalized) | `indicator_norm` |
| `TrendDescription` | `trend_raw` |
| (normalized) | `trend_norm` |
| `TrendScore` | `trend_score` |
| `TrendPeriod (year)` | `trend_period_years` |
| `TrendDataFrequency` | `trend_data_frequency` |
| (derived) | `period_type`, `period_start_year`, `period_end_year` |

---

## Acceptance Criteria

- Fixture CSV committed under test resources for `lawa.water_quality.trend.multi_year`
- Parser reads fixture and:
  - preserves `indicator_raw` + `trend_raw`
  - derives `indicator_norm` + `trend_norm`
  - derives period fields per the Period Semantics decision
- New LAWA trend domain table populated via ingestion pipeline
- Lineage idempotency enforced (dataset_source_id + content_hash)
- Domain uniqueness enforced per site/indicator/window (see schema constraints)
- Phase 8 LAWA state ingestion remains unchanged
