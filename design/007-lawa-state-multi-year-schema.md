# LAWA State & Trend (Multi-Year) --- Schema & Fixture Contract (Phase 8)

## Dataset Identity

**Taxonomy Form:** v2 (publisher.domain.dataset.variant)
**Dataset Source Code:** `lawa.water_quality.state.multi_year`
**Temporal Grain:** Multi-year statistical aggregation window
(`period_start_year` → `period_end_year`)

------------------------------------------------------------------------

## Source

Workbook:
https://www.lawa.org.nz/download-data#download-data-river-water-quality-state

Sheet: `State Attribute Band` 

Units: Mixed categorical + numeric indicators

------------------------------------------------------------------------

## Semantics Guarantees

-   `state` represents guideline band classification (e.g., Good, Fair,
    Poor, Very Poor)
-   Results are calculated by LAWA using national statistical methods
-   Data is aggregated over multi-year hydrological/statistical periods

------------------------------------------------------------------------

## Domain Table

**Table Name:** `lawa_water_quality_state_multi_year_record`

### Columns

  ------------------------------------------------------------------------
  Column                Type            Description
  --------------------- --------------- ----------------------------------
  id                    BIGINT          Surrogate primary key

  dataset_release_id    UUID FK         Lineage reference →
                                        dataset_release(id)

  site_id               TEXT            LAWA monitoring site identifier

  site_name             TEXT            Human-readable site name

  region                TEXT            Regional council / catchment

  indicator_raw         TEXT            Raw indicator name (e.g.,
                                        Nitrate-N, E. coli)

  indicator_norm        TEXT            Canonical indicator category
                                        (NITRATE, ECOLI, CLARITY, MCI,
                                        OTHER)

  state                 TEXT            Good / Fair / Poor / Very Poor

  trend                 TEXT            Improving / Degrading / No Trend

  period_start_year     INT             Start of aggregation window

  period_end_year       INT             End of aggregation window

  value                 DECIMAL         Optional numeric metric (if
                                        present)
  ------------------------------------------------------------------------

------------------------------------------------------------------------

## Constraints

-   UNIQUE(dataset_release_id, site_id, indicator_norm,
    period_start_year, period_end_year)
-   NOT NULL on all domain columns except `value`

------------------------------------------------------------------------

## Fixture Contract

### CSV Header

    site_id,site_name,region,indicator_raw,indicator_norm,state,trend,period_start_year,period_end_year,value

### Example Row

    12345,Waikato River at Hamilton,Waikato,Nitrate-N,NITRATE,Fair,Degrading,2019,2024,1.32

------------------------------------------------------------------------

## Normalization Rules

-   Preserve indicator_raw exactly as published
-   indicator_norm maps to uppercase canonical set: NITRATE, ECOLI,CLARITY, MCI, OTHER
-   Unknown indicators map to OTHER
-   state and trend preserved as published strings

------------------------------------------------------------------------

## Notes

-   LAWA dataset intentionally separate from MBIE tables
-   No geospatial modeling in Phase 8
-   Raw telemetry ingestion deferred beyond Phase 8
