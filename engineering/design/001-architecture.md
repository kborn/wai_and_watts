# Wai & Watts — 001 Architecture

Status: Source of truth
Last updated: 2026-02-23
Audience: Engineers and AI builders

This document describes the current runtime architecture, module boundaries, and execution paths.

---

## 1) System Shape
- Spring Boot monolith with explicit internal layering.
- Postgres as runtime database.
- Flyway-managed forward-only schema evolution.
- Public versioned REST API under `/api/v1/...`.
- Operator ingestion workflow is CLI-first (`transform` then `ingest`).
- Explanation layer is Fact-Pack grounded; no direct LLM access to repositories.

---

## 2) Module and Package Boundaries

Primary backend packages (`backend/src/main/java/nz/waiwatts`):
- `api/` public REST endpoints for datasets, MBIE, LAWA, health, context, and insights.
- `explanations/` ask/explanation pipeline (intent parsing, validation, selection, fact-pack builders, generators, shared LLM client code).
- `service/` read services used by API controllers.
- `ingestion/` orchestration, dataset parsers, dataset ingestion services, transformer utilities.
- `cli/` manual transform and ingestion command entrypoints.
- `domain/` JPA entities (lineage + domain records).
- `persistence/repositories/` JPA repositories.
- `config/` cross-cutting web/logging/error configuration.

### Hard dependency rules
Allowed:
- `api` -> `service` and `explanations/service` interfaces
- `service` -> `persistence`, `domain`
- `explanations/*` -> `service`, `persistence`, `domain` only through established boundaries
- `ingestion` -> `domain`, `persistence`, ingestion utilities
- `cli` -> ingestion services and repositories

Disallowed:
- Controllers calling repositories directly.
- Domain entities leaking as API response payloads.
- Frontend/business semantics implemented in UI in place of backend contracts.

---

## 3) Core Data Model

### 3.1 Cross-cutting lineage
- `dataset_source` is the stable source catalog (`code` is stable identifier).
- `dataset_release` is the ingestion lineage boundary for one artifact version.
- Idempotency is enforced by DB uniqueness on `(dataset_source_id, content_hash)`.

### 3.2 MBIE domain tables
- `mbie_generation_annual_record`
- `mbie_generation_quarterly_record`

Both tables reference `dataset_release_id` and persist raw + normalized fuel values.

### 3.3 LAWA domain tables
- `lawa_state_multi_year_record`
- `lawa_water_quality_trend_multi_year_record`

Both tables reference `dataset_release_id`, include region metadata normalization, and support filterable read paths.

### 3.4 Explanation data boundary
- Fact Packs are the only payload passed to explanation generators.
- Generators return explanation text + citations or explicit refusal.
- Citation mapping/validation occurs in service/controller pipeline before response.

---

## 4) Runtime Execution Paths

### 4.1 Operator ingestion path (authoritative)
1. `ManualTransformCommand` (via `scripts/transform.sh`) converts XLSX to contract CSV.
2. `ManualIngestionCommand` (via `scripts/ingest.sh`) ingests CSV into release + domain tables.
3. `IngestionOrchestrator` and dataset-specific ingestion classes enforce lifecycle + idempotency.

### 4.2 Read API path
1. Controller validates request-shape basics.
2. Service applies filter semantics.
3. Repository executes DB-filtered query.
4. DTO response returns persisted rows (with release provenance fields where defined).

### 4.3 Ask/explanations path
1. `POST /api/v1/explanations/ask` receives natural-language question.
2. Intent parser -> structured request.
3. Dataset selection + request validation enforce supported envelope.
4. FactPackBuilder resolves one canonical dataset release and constructs facts.
5. Generator produces grounded explanation/refusal.
6. Citation mapping + refusal normalization produce API response.

---

## 5) Public API Surface (high level)
- Dataset catalog: `/api/v1/datasets/...`
- MBIE: `/api/v1/mbie/generation/annual`, `/api/v1/mbie/generation/quarterly`, fuel type endpoints.
- LAWA: `/api/v1/lawa/water-quality/state/multiyear`, `/trend/multiyear`, plus regions/indicators endpoints.
- Explanations: `/api/v1/explanations`, `/api/v1/explanations/ask`.
- Capability discovery: `/api/v1/capabilities`.
- Context: `/api/v1/region-context`.
- Health/insights: `/api/v1/health`, `/api/v1/insights`.

---

## 6) Architecture Notes
- Internal ingestion HTTP routes are not part of the operator workflow.
- `dataset_release` semantics intentionally differ by path:
  - Ask is release-pinned for deterministic grounding.
  - Read APIs are release-transparent retrieval interfaces.
- Schema changes must be forward-only migrations.
