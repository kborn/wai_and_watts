# Phase 8 — LAWA State (Multi-Year) Ingestion Spec

## Dataset Identity

- **Taxonomy Form:** v2 (`<publisher>.<domain>.<dataset>.<variant>`)
- **Dataset Source Code:** `lawa.water_quality.state.multi_year`
- **Temporal Grain:** 5-year rolling **hydrological-year** window (Jul–Jun), ending in `hYear`

---

## Purpose

Phase 8 extends Wai & Watts ingestion to LAWA water quality **state** results.
The goal is to prove ingestion extensibility across environmental domains, not just temporal granularity.

---

## Source of Truth

- **Workbook:** https://www.lawa.org.nz/download-data#download-data-river-water-quality-state  
- **Sheet:** `State Attribute Band`  
- **Semantics:** multi-year state (band) classification for river monitoring sites.

---

## Period Semantics (Decision)

For each row with `hYear = Y`:

- `period_type = 'HYDRO_5YR_ROLLING'`
- `period_start_year = Y - 5`
- `period_end_year = Y`

This encodes LAWA’s rolling 5-year hydrological-year window (Jul–Jun).

---

## Scope

- Ingest LAWA river water quality **state** (attribute band) results from `State Attribute Band`
- Fixture-first ingestion (no live downloads in Phase 8)
- Full lineage tracking via `dataset_source` + `dataset_release`
- Read-only API exposure for querying persisted facts

---

## Non-Goals

- Ingesting LAWA **Trend** tab (deferred to Phase 9)
- Raw monitoring telemetry ingestion
- Geospatial queries or PostGIS
- Time-series modeling or trend detection by Wai & Watts
- Forecasting or causal analysis
- Integration joins with MBIE datasets (presentation-layer joining only)

---

## Required Field Mapping (Sheet → Domain)

| Sheet Column | Domain Column |
|---|---|
| `LawaSiteID` | `lawa_site_id` |
| `SiteID` | `site_name` |
| `Region` | `region` |
| `Latitude` | `latitude` |
| `Longitude` | `longitude` |
| `Indicator / Attribute` | `indicator_raw` |
| (normalized) | `indicator_norm` |
| `UnitsOfMeasure` | `units` |
| `Attribute Band` | `attribute_band` |
| (derived) | `state_norm` |
| `Median` | `median` |
| `95th Percentile` | `p95` |
| `RecHealth_% exceedances over 260_numeric attribute statistic` | `rec_health_exceed_260_pct` |
| `RecHealth_% exceedances over 540_numeric attribute statistic` | `rec_health_exceed_540_pct` |
| `hYear` | `period_end_year` |
| (derived from `hYear`) | `period_start_year`, `period_type` |

---

## Acceptance Criteria

- Fixture CSV committed under test resources for `lawa.water_quality.state.multi_year`
- Parser reads fixture and:
  - preserves `indicator_raw`
  - derives `indicator_norm`
  - derives `state_norm` from `attribute_band`
  - derives period fields from `hYear` per the Period Semantics decision
- New LAWA domain table populated via ingestion pipeline
- Lineage idempotency enforced (dataset_source_id + content_hash)
- Domain uniqueness enforced per site/indicator/period (see schema constraints)
- MBIE Phase 6/7 ingestion remains unchanged
- Read-only API endpoint returns LAWA state data

---

## Extensibility Proof

Extensibility is proven when:

- Shared ingestion lifecycle/orchestrator is reused unchanged
- Dataset-specific parser + schema are added without modifying MBIE logic
- Integration tests validate MBIE annual, MBIE quarterly, and LAWA state independently


## Fixture Generation
The Phase 8 fixture CSV is derived from the LAWA “River water quality state” workbook (sheet State Attribute Band) using a deterministic normalization script (scripts/normalize_lawa_state_attribute_band.py). 
The script filters to a small, stable slice of regions/sites/indicators and then derives required fields (indicator_norm, state_norm, and hydrological rolling-window period fields) per the Phase 8 schema contract. 
The fixture is treated as the canonical test input; live download ingestion is deferred to Phase 10.