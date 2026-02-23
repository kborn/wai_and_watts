# LAWA Water Quality — Trend (Multi-Year) — Schema & Fixture Contract (Phase 9)

## Dataset Identity

- **Taxonomy Form:** v2 (`<publisher>.<domain>.<dataset>.<variant>`)
- **Dataset Source Code:** `lawa.water_quality.trend.multi_year`
- **Temporal Grain:** multi-year window (variable length), hydrological-year basis

---

## Source

- **Workbook:** `lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx`
- **Sheet:** `Trend`

---

## Period Semantics (Decision)

The LAWA Trend sheet provides a **window length** (`TrendPeriod (year)`), but does not publish an explicit end-year
per row.

Phase 9 contract:
- `as_of_year` is derived from the same workbook’s State sheet (`State Attribute Band`) as `MAX(hYear)`.
- For each Trend row with `trend_period_years = N`:
  - `period_type = 'HYDRO_NYR_WINDOW'`
  - `period_end_year = as_of_year`
  - `period_start_year = as_of_year - N`

For the 30 Oct 2025 export, `as_of_year = 2024`.

---

## Trend Semantics Guarantees

- `trend_raw` is preserved exactly as published in `TrendDescription`.
- `trend_norm` is a deterministic normalization of `trend_raw` to a small, stable enum.

Allowed values for `trend_norm`:

- `IMPROVING`
- `DEGRADING`
- `NO_CHANGE` (reserved; only used if LAWA publishes an explicit “no change” classification)
- `INSUFFICIENT_DATA` (covers “Not determined”, “Indeterminate”, and any non-directional classifications)

---

## Domain Table

**Table Name:** `lawa_water_quality_trend_multi_year_record`

### Columns

| Column | Type | Description                                                                                                                           |
|---|---:|---------------------------------------------------------------------------------------------------------------------------------------|
| `id` | BIGINT | Surrogate primary key                                                                                                                 |
| `dataset_release_id` | UUID FK | Lineage reference → `dataset_release(id)`                                                                                             |
| `lawa_site_id` | TEXT | LAWA monitoring site identifier (`LawaSiteID`)                                                                                        |
| `site_name` | TEXT | Human-readable site name (`SiteID`)                                                                                                   |
| `region` | TEXT | Region / council grouping (`Region`)                                                                                                  |
| `latitude` | DECIMAL(9,6) | Site latitude (nullable)                                                                                                              |
| `longitude` | DECIMAL(9,6) | Site longitude (nullable)                                                                                                             |
| `indicator_raw` | TEXT | Raw indicator label (`Indicator`)                                                                                                     |
| `indicator_norm` | TEXT | Canonical indicator key (see normalization rules)                                                                                     |
| `units` | TEXT | Not published in Trend sheet export (nullable; keep for consistency)                                                                  |
| `trend_raw` | TEXT | Raw LAWA trend description (`TrendDescription`)                                                                                       |
| `trend_norm` | TEXT | Normalized trend enum                                                                                                                 |
| `trend_score` | SMALLINT | LAWA trend score (`TrendScore`). Stored exactly as published by LAWA and must not be interpreted as a linear slope or rate of change. |
| `trend_period_years` | SMALLINT | Window length (`TrendPeriod (year)`)                                                                                                  |
| `trend_data_frequency` | TEXT | Data frequency (`TrendDataFrequency`) (nullable)                                                                                      |
| `period_type` | TEXT | Always `HYDRO_NYR_WINDOW`                                                                                                             |
| `period_start_year` | INT | `as_of_year - trend_period_years`                                                                                                     |
| `period_end_year` | INT | `as_of_year`                                                                                                                          |

---

## Constraints

- **UNIQUE**(`dataset_release_id`, `lawa_site_id`, `indicator_norm`, `period_type`, `trend_period_years`, `period_end_year`)
- **NOT NULL** on: `dataset_release_id`, `lawa_site_id`, `site_name`, `region`, `indicator_raw`, `indicator_norm`, `trend_raw`, `trend_norm`, `trend_score`, `trend_period_years`, `period_type`, `period_start_year`, `period_end_year`
- `latitude` / `longitude` may be NULL.
- `trend_data_frequency` may be NULL.
- `units` may be NULL.

---

## Fixture Contract

### CSV Header

```csv
lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,trend_raw,trend_norm,trend_score,trend_period_years,trend_data_frequency,period_type,period_start_year,period_end_year
```

### Example Row

```csv
arc-00001,Cascades LTB,auckland,-36.88888973,174.52211474,Ammoniacal nitrogen,AMMONIACAL_N,,Not determined,INSUFFICIENT_DATA,-99,5,,HYDRO_NYR_WINDOW,2019,2024
```

---

## Normalization Rules

- Preserve `indicator_raw` and `trend_raw` exactly as published.
- `indicator_norm` is an uppercase canonical key intended to be stable (not a full ontology).
- Unknown indicators map to `OTHER`.

Recommended initial `indicator_norm` mappings (expand as needed):

- `E.coli` → `ECOLI`
- `Clarity` → `CLARITY`
- `Dissolved reactive phosphorus` → `DRP`
- `Nitrate nitrogen` → `NITRATE_N`
- `Total nitrogen` → `TOTAL_N`
- `Ammoniacal nitrogen` → `AMMONIACAL_N`
- otherwise → `OTHER`

`trend_norm` mapping:
- contains “improving” → `IMPROVING`
- contains “degrading” → `DEGRADING`
- “Not determined” / “Indeterminate” / non-directional → `INSUFFICIENT_DATA`
- explicit “no change” (if present) → `NO_CHANGE`

---

## Notes

- This dataset is ingesting LAWA-published trend classifications and scores (no trend computation in Wai & Watts).
- Live downloading remains a separate concern; Phase 9 can be fixture-first if desired.
- Trend fixtures must be generated from the same workbook export as the paired State fixture slice to guarantee deterministic period derivation.
- Trend ingestion derives period fields using workbook context only and must not query State domain tables or releases.