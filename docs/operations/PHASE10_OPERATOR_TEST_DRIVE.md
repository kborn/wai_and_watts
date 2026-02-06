# Phase 10 --- Operator Test Drive Checklist

## Purpose

Validate that Wai & Watts Phase 10 ingestion behaves like a real
operator-facing data ingestion tool, not just a developer pipeline.

This exercise is intentionally **manual and end-to-end**.

Goals: - Verify real publisher → transform → contract → ingestion →
query lifecycle - Validate failure behavior and operator UX - Capture
real-world usage notes for portfolio and design reflection

------------------------------------------------------------------------

## Success Criteria

By the end of this exercise you should have:

-   Successfully ingested at least one real publisher dataset
-   Verified idempotent re-ingestion
-   Observed at least one failure scenario intentionally
-   Measured time-to-first-success
-   Captured operator notes

------------------------------------------------------------------------

## Pre-Requisites

### Environment

-   JDK 21 installed and active
-   Maven working locally
-   Backend builds successfully:


    mvn -f backend clean verify

------------------------------------------------------------------------

### Publisher Artifact Acquisition

You must be able to obtain a real publisher XLSX using **one of the
following methods**:

#### Preferred --- Download Scripts (Phase 10 Canonical)

Use project download tooling to fetch publisher artifacts.

Example (conceptual):

    ./scripts/download_mbie.sh 2025Q3
    ./scripts/download_lawa.sh state

Output should land in a raw artifact location such as:

    data/raw/<dataset>/<publisher_file>.xlsx

------------------------------------------------------------------------

#### Acceptable --- Manual Download (Fallback)

If download scripts are unavailable or under development: - Download
publisher XLSX manually - Store locally for transform testing

------------------------------------------------------------------------

### Why Download Is Separate From Transform

Phase 10 intentionally separates:

    Acquire Artifact → Transform → Ingest

This preserves: - Deterministic transforms - Offline debugging
capability - Reproducibility of contract CSV generation - Ability to
regression test transforms using stored artifacts - Clear separation
between network failures and schema/data failures

Transform scripts and transformers must: - Never fetch network
resources - Operate only on provided local artifacts

------------------------------------------------------------------------

## Phase 1 --- Clean Build

    mvn -f backend clean package

Verify: - Build succeeds - Backend jar exists in:

    backend/target/

------------------------------------------------------------------------

## Phase 2 --- Transform (Publisher → Contract CSV)

Example:

    ./scripts/transform.sh \
      mbie.generation.annual \
      data/raw/mbie/electricity-sept-2025-q3.xlsx \
      /tmp/mbie_annual_contract.csv

Verify: - Script exits successfully - CSV output exists - File is
non-empty - Headers match contract schema

Optional:

    head /tmp/output.csv
    wc -l /tmp/output.csv

------------------------------------------------------------------------

## Phase 3 --- Ingest Contract CSV

Run ingestion using existing manual ingestion workflow.

Verify: - Dataset release created - Status transitions complete - Rows
persisted

------------------------------------------------------------------------

## Phase 4 --- Query Validation

Validate domain tables contain expected data.

Check: - Row counts match expectation - Sample rows match publisher
source values - No duplicate records

------------------------------------------------------------------------

## Phase 5 --- Idempotency Test

Re-run transform + ingest with same input.

Expected: - No duplicate dataset release - No duplicate domain rows -
System reports existing release reused

------------------------------------------------------------------------

## Phase 6 --- Failure Testing (Intentional)

Test at least 3:

### Wrong Dataset Code

Expect: - Fast validation failure - Clear error message

### Corrupt XLSX

Expect: - Transform failure - No partial CSV output

### Missing Column / Schema Drift

Expect: - Transform fails fast - Clear message indicating missing field

### Missing Input File

Expect: - Validation error before transform starts

------------------------------------------------------------------------

## Phase 7 --- Operator UX Review

Ask:

-   Are errors understandable?
-   Are commands discoverable?
-   Is documentation sufficient?
-   Are failure modes safe?

------------------------------------------------------------------------

## Phase 8 --- Time-To-First-Success Measurement

Measure:

Time from: Download publisher file\
→ Transform\
→ Ingest\
→ Query verification

Record result.

------------------------------------------------------------------------

## Phase 9 --- Reflection Notes

Capture:

### What Worked Well

-   Transform reliability
-   Error clarity
-   Operator simplicity

### What Was Confusing

-   Docs gaps
-   Error messages
-   Command UX

### What Surprised You

-   Performance
-   Failure behavior
-   Data quirks

------------------------------------------------------------------------

## Phase 10 --- Portfolio Capture (Optional but Recommended)

Capture 3 metrics:

### Operator Bootstrap Time

Example:

> Raw publisher XLSX → queryable domain data in 12 minutes.

### Safety Guarantees

Example:

> Contract-first ingestion prevents publisher format drift from breaking
> downstream models.

### Reliability Features

Example:

> Content-hash idempotency prevents duplicate dataset releases and
> duplicate domain rows.

------------------------------------------------------------------------

## Non-Goals

This is NOT: - Performance benchmarking - Load testing - Automation
work - Scheduler design

------------------------------------------------------------------------

## Completion Checklist

-   [ ] Real dataset successfully ingested
-   [ ] Idempotent re-run verified
-   [ ] At least 3 failure scenarios tested
-   [ ] Time-to-first-success recorded
-   [ ] Reflection notes captured

------------------------------------------------------------------------

## Notes

Phase 10 intentionally prioritizes: - Deterministic ingestion - Contract
schema stability - Operator clarity - Real publisher artifact handling

Automation, scheduling, and orchestration are future phases.
