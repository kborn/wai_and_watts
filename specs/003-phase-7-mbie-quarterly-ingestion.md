# Phase 7 --- MBIE Quarterly Generation Ingestion Spec

Dataset Source Code: `mbie.generation.quarterly`

## Purpose

Phase 7 extends Wai & Watts ingestion to quarterly MBIE electricity
generation data.
The goal is to prove ingestion extensibility by reusing the existing
lifecycle, lineage, and orchestration patterns with a new temporal
granularity.

## Source of Truth

Workbook:https://www.mbie.govt.nz/assets/Data-Files/Energy/nz-energy-quarterly-and-energy-in-nz/electricity-sept-2025-q3.xlsx

Sheet: `1 - Fuel type (GWh)`

Semantics: Quarterly net electricity generation by fuel type, measured
in GWh.

## Scope

-   Quarterly electricity generation by fuel type
-   Fixture-first ingestion (no live downloads in Phase 7)
-   Full lineage tracking via dataset_source + dataset_release
-   Read-only API exposure

## Non-Goals

-   Unifying annual and quarterly tables into a polymorphic temporal
    model
-   Live ingestion from MBIE endpoints
-   LAWA or cross-sector energy modeling (Phase 8+)
-   Predictive analytics or forecasting

## Acceptance Criteria

-   Quarterly fixture CSV committed under test resources
-   Parser reads fixture and normalizes fuel types
-   New quarterly domain table populated via ingestion pipeline
-   Lineage idempotency enforced (dataset_source_id + content_hash)
-   Domain uniqueness enforced (no duplicate period_year/period_quarter/fuel per
    release)
-   Phase 6 annual ingestion remains unchanged and tests pass
-   Read-only API endpoint returns quarterly data

## Extensibility Proof

Extensibility is proven when: - Shared ingestion lifecycle/orchestrator
code is reused unchanged - Dataset-specific parser and schema are added
without modifying Phase 6 logic - Integration tests validate both
datasets independently
