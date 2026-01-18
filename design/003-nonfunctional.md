# Wai & Watts — 003 Non-Functional Requirements (MVP Only)

**Status:** Implemented NFRs only  
**Last updated:** 2026-01-17  
**Audience:** Builder GPT

This document contains only NFRs we will actually implement in the MVP.

---

## 1. Idempotent ingestion (required)
- Each ingestion computes a **content hash**.
- Unique constraint: (`dataset_source_id`, `content_hash`).
- Re-ingesting identical content:
  - must not duplicate facts
  - must return a clear “already imported” response.

---

## 2. Basic observability (required)
- Structured logs for:
  - ingestion start/end
  - dataset_release_id, dataset name, content hash
  - duration
  - rows read/inserted/updated
  - warnings count
- Health endpoint reports app + DB connectivity (e.g., Actuator health).

---

## 3. Tests + CI (required)
- Integration tests using **Testcontainers Postgres**:
  - ingest → query happy path
  - idempotent re-ingest
  - malformed input / parse failure yields structured error
- CI runs:
  - build
  - tests
  - formatting/linting if configured

---

## 4. LLM grounding rule (required)
Any endpoint that returns derived “insights” must adhere to:

- **Cite or refuse**: only include statements derivable from stored facts + dataset metadata.
- If the system cannot ground a claim (missing data, no releases ingested), it must:
  - omit the claim, or
  - return an explicit “unavailable” field with a clear reason.
- Every insights response must include a `factPack` with:
  - sources used, published dates, imported timestamps
  - caveats (plain language)

---

## Definition of Done (NFRs)
- Idempotency enforced at DB + service level.
- Ingestion runs are diagnosable from logs + persisted release metadata.
- Testcontainers suite runs locally and in CI.
- Insights endpoints always include fact pack and never include ungrounded claims.
