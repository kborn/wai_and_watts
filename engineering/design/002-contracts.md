# Wai & Watts — 002 Contracts

Status: Interface contract reference
Last updated: 2026-02-23
Audience: Engineers and AI builders

This document defines API and ingestion contracts for current runtime behavior.

---

## 1) API Contract

### 1.1 API versioning
- Public API routes use `/api/v1/...` path versioning.
- Internal/dev-only routes, if present, must remain outside operator workflow contracts.

### 1.2 Core public endpoints

System:
- `GET /api/v1/health`
- `GET /api/v1/insights`

Dataset catalog:
- `GET /api/v1/datasets/sources`
- `GET /api/v1/datasets/sources/{id}/releases`

MBIE:
- `GET /api/v1/mbie/generation/annual`
- `GET /api/v1/mbie/generation/annual/fuel-types`
- `GET /api/v1/mbie/generation/quarterly`
- `GET /api/v1/mbie/generation/quarterly/fuel-types`

LAWA:
- `GET /api/v1/lawa/water-quality/state/multiyear`
- `GET /api/v1/lawa/water-quality/state/multiyear/regions`
- `GET /api/v1/lawa/water-quality/state/multiyear/indicators`
- `GET /api/v1/lawa/water-quality/trend/multiyear`
- `GET /api/v1/lawa/water-quality/trend/multiyear/regions`
- `GET /api/v1/lawa/water-quality/trend/multiyear/indicators`

Context:
- `GET /api/v1/region-context`

Explanations:
- `POST /api/v1/explanations` (structured request)
- `POST /api/v1/explanations/ask` (natural-language request)
- `GET /api/v1/explanations/health`
- `POST /api/v1/explanations/fact-pack` (debug/development helper)

Capabilities:
- `GET /api/v1/capabilities`

### 1.3 Query filter conventions
- MBIE read endpoints: `fromYear`, `toYear`, `fuelType`
- LAWA read endpoints: `fromYear`, `toYear`, `indicator`, `region`
- Context endpoint: `region`, `indicator`, `trendWindow`
- Invalid year ranges (`fromYear > toYear`) return HTTP 400.

### 1.4 Error/refusal behavior
- Generic controller errors use HTTP 4xx/5xx with map-style error payloads.
- `/api/v1/explanations/ask` returns deterministic refusal-shaped payloads (including internal error refusal shape) instead of raw HTTP 500 leakage.
- Supported refusal code set:
  - `UNSUPPORTED_INTENT`
  - `UNABLE_TO_PARSE`
  - `UNSUPPORTED_CAPABILITY`
  - `DATASET_MISMATCH`
  - `MISSING_REQUIRED_FILTERS`
  - `INTERNAL_ERROR` (runtime fault path)

---

## 2) Ingestion Contracts

### 2.1 Supported dataset source codes
- `mbie.generation.annual`
- `mbie.generation.quarterly`
- `lawa.water_quality.state.multi_year`
- `lawa.water_quality.trend.multi_year`

### 2.2 Operator contract
- Transform and ingest are CLI-driven operations.
- Backend server runtime is not required to ingest files.
- Ingestion requires dataset source to exist in `dataset_source` catalog.

### 2.3 File transform contract
- Input: publisher XLSX
- Output: contract CSV for one dataset source code
- Transform output schema must match dataset parser expectations.

### 2.4 Idempotency contract
- Content hash is computed at ingestion request boundary.
- Duplicate `(dataset_source_id, content_hash)` must behave as successful no-op and reuse existing release lineage.

---

## 3) Contract Change Rules
- Additive fields/endpoints may be introduced without changing API major path.
- Breaking API contract changes require explicit versioning decision.
- DB/schema contract changes must be forward migration only.
- Contract updates must be reflected in `engineering/design/` docs in the same change set.
