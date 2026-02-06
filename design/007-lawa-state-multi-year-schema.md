# LAWA Water Quality — State (Multi-Year) — Schema & Fixture Contract (Phase 8)

## Dataset Identity

- **Taxonomy Form:** v2 (`<publisher>.<domain>.<dataset>.<variant>`)
- **Dataset Source Code:** `lawa.water_quality.state.multi_year`
- **Temporal Grain:** 5-year rolling **hydrological-year** window (Jul–Jun), ending in `hYear`

---

## Source

- **Workbook:** https://www.lawa.org.nz/media/ftmb4fjn/lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx  
- **Sheet:** `State Attribute Band`  
- **Units:** Mixed categorical + numeric indicators

---

## Period Semantics (Decision)

LAWA “state” results are published for a **rolling 5-year hydrological-year window**.

For each row with `hYear = Y`:

- `period_type = 'HYDRO_5YR_ROLLING'`
- `period_start_year = Y - 5`
- `period_end_year = Y`

> Interpretation note (informational, not stored as dates): the window corresponds to **01-Jul-(Y-5)** through **30-Jun-Y**.

---

## Semantics Guarantees

- `attribute_band` is the guideline band classification published by LAWA (A..E).
- `state_norm` is a deterministic normalization of `attribute_band`:
  - A → EXCELLENT
  - B → GOOD
  - C → FAIR
  - D → POOR
  - E → VERY_POOR
- Results are published/derived by LAWA (not computed by Wai & Watts).
- **Trend is not ingested in Phase 8** (deferred to Phase 9).

---

## Domain Table

**Table Name:** `lawa_water_quality_state_multi_year_record`

### Columns

| Column | Type | Description |
|---|---:|---|
| `id` | BIGINT | Surrogate primary key |
| `dataset_release_id` | UUID FK | Lineage reference → `dataset_release(id)` |
| `lawa_site_id` | TEXT | LAWA monitoring site identifier (`LawaSiteID`) |
| `site_name` | TEXT | Human-readable site name (`SiteID`) |
| `region` | TEXT | Region / council grouping (`Region`) |
| `latitude` | DECIMAL | Site latitude (nullable) |
| `longitude` | DECIMAL | Site longitude (nullable) |
| `indicator_raw` | TEXT | Raw indicator / attribute label (`Indicator / Attribute`) |
| `indicator_norm` | TEXT | Canonical indicator key (see normalization rules) |
| `units` | TEXT | Units of measure (`UnitsOfMeasure`) (nullable) |
| `attribute_band` | TEXT | Raw band A..E |
| `state_norm` | TEXT | Normalized state: EXCELLENT / GOOD / FAIR / POOR / VERY_POOR |
| `median` | DECIMAL | Published median (nullable) |
| `p95` | DECIMAL | Published 95th percentile (nullable) |
| `rec_health_exceed_260_pct` | DECIMAL | % exceedances over 260 (nullable) |
| `rec_health_exceed_540_pct` | DECIMAL | % exceedances over 540 (nullable) |
| `period_type` | TEXT | Always `HYDRO_5YR_ROLLING` |
| `period_start_year` | INT | `hYear - 5` |
| `period_end_year` | INT | `hYear` |

---

## Constraints

- **UNIQUE**(`dataset_release_id`, `lawa_site_id`, `indicator_raw`, `period_type`, `period_end_year`)
- **NOT NULL** on: `dataset_release_id`, `lawa_site_id`, `site_name`, `region`, `indicator_raw`, `indicator_norm`, `attribute_band`, `state_norm`, `period_type`, `period_start_year`, `period_end_year`
- All numeric statistics (`median`, `p95`, exceedance % fields) may be NULL.
- `latitude` / `longitude` may be NULL.

---

## Fixture Contract

### CSV Header

```csv
lawa_site_id,site_name,region,latitude,longitude,indicator_raw,indicator_norm,units,attribute_band,state_norm,median,p95,rec_health_exceed_260_pct,rec_health_exceed_540_pct,period_type,period_start_year,period_end_year
```

### Example Row

```csv
arc-00036,Avondale @ Shadbolt,auckland,-36.9232796,174.69177898,E.coli,ECOLI,#/100 mL,E,VERY_POOR,1750,13000,94.8275862068966,81.0344827586207,HYDRO_5YR_ROLLING,2019,2024
```

---

## Normalization Rules

- Preserve `indicator_raw` exactly as published.
- `indicator_norm` is an uppercase canonical key intended to be **stable** (not a full chemistry ontology).
- Unknown indicators map to `OTHER`.
- Preserve `units` as published (nullable).
- Preserve `attribute_band` as published; derive `state_norm` deterministically.

Recommended initial `indicator_norm` mappings (expand as needed):

- `E.coli` → `ECOLI`
- `Clarity / Suspended fine sediment` → `CLARITY`
- `Dissolved reactive phosphorus` → `DRP`
- `NO3N` → `NO3N`
- `TON` → `TON`
- `Ammonical nitrogen / Ammonia (toxicity)` → `AMMONIA_TOXICITY`
- `Nitrate nitrogen / Nitrate (toxicity)` → `NITRATE_TOXICITY`
- otherwise → `OTHER`

---

## Notes

- This dataset is intentionally separate from MBIE electricity tables.
- No raw telemetry ingestion in Phase 8.
- No geospatial modeling (PostGIS) in Phase 8; lat/long are stored only as attributes for later UI use.
