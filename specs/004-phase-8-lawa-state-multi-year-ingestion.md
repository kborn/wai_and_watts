# Phase 8 --- LAWA State(Multi-Year) Ingestion Spec

## Dataset Identity

**Taxonomy Form:** v2 (publisher.domain.dataset.variant)
**Dataset Source Code:** `lawa.water_quality.state.multi_year`
**Temporal Grain:** Multi-year statistical aggregation window

------------------------------------------------------------------------

## Purpose

Phase 8 extends Wai & Watts ingestion to LAWA water quality state results.
The goal is to prove ingestion extensibility across **environmental
domains**, not just temporal granularity.

------------------------------------------------------------------------

## Source of Truth

Workbook:
https://www.lawa.org.nz/download-data#download-data-river-water-quality-state

Sheet: `State Attribute Band`

Semantics: Multi-year state classification for river monitoring sites.

------------------------------------------------------------------------

## Scope

-   River water quality state results
-   Fixture-first ingestion (no live downloads)
-   Full lineage tracking via dataset_source + dataset_release
-   Read-only API exposure

------------------------------------------------------------------------

## Non-Goals

-   Raw monitoring telemetry ingestion
-   Geospatial queries or PostGIS
-   Time-series modeling
-   Forecasting or causal analysis
-   Integration with MBIE datasets

------------------------------------------------------------------------

## Acceptance Criteria

-   Fixture CSV committed under test resources
-   Parser reads fixture and normalizes indicators
-   New LAWA domain table populated via ingestion pipeline
-   Lineage idempotency enforced (dataset_source_id + content_hash)
-   Domain uniqueness enforced per site/indicator/period
-   MBIE Phase 6/7 ingestion remains unchanged
-   Read-only API endpoint returns LAWA data

------------------------------------------------------------------------

## Extensibility Proof

Extensibility is proven when:

-   Shared ingestion lifecycle/orchestrator reused unchanged
-   Dataset-specific parser and schema added without modifying MBIE logic
-   Integration tests validate all three datasets independently
