# Wai & Watts — 001 Architecture

**Status:** Source of truth  
**Last updated:** 2026-01-17  
**Audience:** Builder GPT

This document defines the system shape: modules, domain model, and ingestion lifecycle.

---

## 1. High-level architecture

- **Spring Boot** monolith with strong internal modularization
- **Postgres** for storage
- **REST** APIs (JSON)
- **Batch ingestion** of public datasets (local-only)

Key principle: **fact-pack-first** — any derived insight must be grounded in ingested facts + explicit dataset metadata.

---

## 2. Module / package architecture

Recommended package layout:

```
nz.waiwatts
├── WaiWattsApplication.java
├── config/
├── api/
│   ├── health/
│   ├── datasets/
│   ├── ingest/
│   ├── lawa/
│   ├── energy/
│   └── insights/
├── service/
│   ├── datasets/
│   ├── ingest/
│   ├── lawa/
│   ├── energy/
│   └── insights/
├── domain/
│   ├── datasets/
│   ├── lawa/
│   └── energy/
├── persistence/
│   ├── repositories/
│   └── mappings/
├── ingestion/
│   ├── core/
│   ├── lawa/
│   └── mbie/
└── util/
```

### Dependency rules (hard rules)
Allowed:
- `api` → `service`
- `service` → `domain`, `persistence`, `ingestion` (to trigger ingestion)
- `ingestion` → `domain`, `persistence`, `util`
- `persistence` → `domain`

Disallowed:
- `api` must not talk to `persistence` directly
- `domain` must not depend on `api`/`service`/`ingestion`
- `api` must not implement business logic or parsing

---

## 3. Domain model (v0)

### 3.1 Cross-cutting lineage

**dataset_source**
- `id` (UUID PK)
- `name` (TEXT)
- `publisher` (LAWA | MBIE)
- `source_url` (TEXT, unique)
- `expected_format` (CSV | XLSX | ZIP)
- `update_cadence` (TEXT, nullable)

**dataset_release**
- `id` (UUID PK)
- `dataset_source_id` (FK)
- `published_date` (DATE)
- `release_label` (TEXT, nullable)
- `retrieved_at` (TIMESTAMP)
- `imported_at` (TIMESTAMP, nullable)
- `content_hash` (TEXT)
- `status` (PENDING | IMPORTED | FAILED)
- `notes` (TEXT, nullable)

Uniqueness (idempotency):
- Unique: (`dataset_source_id`, `content_hash`)

---

### 3.2 LAWA: state & trend results (derived assessments)

**lawa_site**
- `id` (UUID PK)
- `external_site_id` (TEXT, unique)
- `site_name` (TEXT)
- `region` (TEXT, nullable)
- `council` (TEXT, nullable)
- `river_name` (TEXT, nullable)
- `lat`, `lon` (NUMERIC, nullable)

**lawa_indicator**
- `id` (UUID PK)
- `code` (TEXT, unique)
- `display_name` (TEXT)
- `unit` (TEXT, nullable)
- `description` (TEXT, nullable)

**lawa_assessment**
- `id` (UUID PK)
- `dataset_release_id` (FK)
- `site_id` (FK)
- `indicator_id` (FK)
- `trend_window_years` (INT) — expected 5/10/15/20
- `trend_direction` (IMPROVING | WORSENING | NO_TREND | INSUFFICIENT_DATA)
- `trend_slope` (NUMERIC, nullable)
- `trend_significance` (NUMERIC, nullable)
- `state_class` (TEXT, nullable)
- `state_value` (NUMERIC, nullable)
- `period_start`, `period_end` (DATE, nullable)

Uniqueness:
- (`dataset_release_id`, `site_id`, `indicator_id`, `trend_window_years`)

Indexes (minimum viable):
- (`indicator_id`, `trend_window_years`, `trend_direction`)
- (`site_id`)
- (`dataset_release_id`)

---

### 3.3 Energy: fuels and MBIE generation series

**energy_fuel**
- `id` (UUID PK)
- `code` (TEXT, unique)
- `display_name` (TEXT)
- `is_renewable` (BOOLEAN)

**mbie_electricity_generation**
- `id` (UUID PK)
- `dataset_release_id` (FK)
- `period_type` (QUARTER)  // MVP
- `period_start` (DATE)
- `period_end` (DATE)
- `fuel_id` (FK energy_fuel)
- `generation_gwh` (NUMERIC)

Uniqueness:
- (`dataset_release_id`, `period_start`, `fuel_id`)

Indexes:
- (`period_start`)
- (`fuel_id`, `period_start`)

---

### 3.4 MBIE renewables (flexible metric rows)

**mbie_renewables_stat**
- `id` (UUID PK)
- `dataset_release_id` (FK)
- `year` (INT)
- `resource_type` (TEXT)
- `metric` (TEXT)
- `value` (NUMERIC)
- `unit` (TEXT, nullable)

Indexes:
- (`year`)
- (`resource_type`, `year`)

---

## 4. Ingestion lifecycle (source of truth)

### 4.1 Steps
1. **Request received**: validate `sourceUrl` + `publishedDate`.
2. **Retrieve**: download artifact over HTTPS; compute `content_hash`.
3. **Create release**: insert `dataset_release` with status `PENDING`.
4. **Idempotency check**:
   - if (`dataset_source_id`, `content_hash`) exists and is `IMPORTED`, return “already imported”.
5. **Parse**:
   - dataset-specific parsing using contracts in `design/002-contracts.md`.
6. **Validate**:
   - required fields present; invalid rows skipped with warnings unless systemic.
7. **Persist (transactional)**:
   - upsert dimensions first, then facts.
8. **Complete**:
   - mark release `IMPORTED`, set `imported_at`, persist row counts/notes.
9. **Failure**:
   - rollback transaction; mark release `FAILED`; persist error details.

### 4.2 Idempotency boundary
- Idempotency is enforced at the **dataset_release** level using content hash.
- Domain facts are always tied to a release; re-ingestion of identical content must not create duplicates.

### 4.3 Observability hooks
Each ingestion run logs:
- dataset name, release id, content hash
- duration
- rows read/inserted/updated
- warnings count

---

## 5. Derived computations (no extra tables in MVP)
- **Renewable share** per quarter:
  - sum(generation_gwh where fuel.is_renewable) / sum(all fuels)
- **Trend counts**:
  - grouped counts of lawa_assessment.trend_direction filtered by indicator/window/region/site

---

## 6. Architectural definition of done
- Package boundaries respected (thin controllers; services own use-cases; ingestion owns parsing).
- Domain + migrations exist and match this document.
- Ingestion and insights can be explained end-to-end using lineage tables.


---

## 7. Implementation order (Builder execution checklist)

The following order is intentional and should be followed to minimize churn and rework:

1. **Database foundation**
   - Flyway migrations for all domain tables
   - Seed static reference data (e.g. `energy_fuel`)

2. **Persistence layer**
   - JPA entities / mappings
   - Repositories for all domain aggregates

3. **Ingestion core**
   - HTTP download
   - Content hashing
   - `dataset_release` lifecycle (PENDING → IMPORTED / FAILED)
   - Transaction boundaries and idempotency checks

4. **Dataset ingestion**
   - LAWA parser → persist sites, indicators, assessments
   - MBIE electricity parser → persist fuels + generation series
   - MBIE renewables parser → persist flexible metrics

5. **Query APIs**
   - LAWA query endpoints
   - Energy generation + renewable share endpoints

6. **Insights**
   - `/insights/overview` endpoint
   - Fact-pack composition and grounding enforcement

7. **Integration tests**
   - Testcontainers Postgres
   - Ingest → query happy path
   - Idempotent re-ingest
   - Failure diagnostics

Deviation from this order should be intentional and justified.
