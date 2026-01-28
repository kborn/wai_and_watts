# Wai & Watts — Project Progress

This document tracks **phases and current status**.
It is the canonical answer to: “Where are we in the plan?”

---

## Session Recovery
If session context is lost, read in order:
1) `project-context.md`
2) `roles.md`
3) `progress.md`
4) `decisions.md`
5) `ai_usage.md`

---

## Phase Entry Format (Required)

### Phase N — `Name` `Status`
Goal: `one sentence`

Definition of Done:
- [ ] `objective criteria`

Work Items:
- [ ] `tasks`

Notes (optional; keep short):
- `temporary reminders / blockers; link to decisions.md for rationale`

Links (optional):
- Implementation: `branch/PR`
- Tests: `path`
- Decisions: `section link`

---

## Current Position
- **Active Phase:** Phase 7 — Second Dataset Ingestion (Prove Extensibility) 🟡 
- **Status:** In Progress

---

## Phase Overview

### Phase 1 — Project & Documentation Foundations ✅
Goal: Establish scope, constraints, and AI workflow.

Definition of Done:
- [x] `project-context.md` exists and is accurate
- [x] `roles.md` defines allowed actions per role
- [x] `progress.md` defines phases + current position
- [x] `decisions.md` exists and is append-only
- [x] `ai_usage.md` documents AI boundaries

Work Items:
- [x] Create foundational docs
- [x] Record initial decisions and workflow guardrails

---

### Phase 2 — Backend Scaffolding ✅
Goal: Bootable backend with no domain logic.

Definition of Done:
- [x] Spring Boot app compiles and runs locally
- [x] Postgres configured for local dev
- [x] Flyway migrations run successfully
- [x] Minimal test harness in place

Work Items:
- [x] Create backend module and basic configuration
- [x] Add Flyway + initial migration

---

### Phase 3 — Dataset Lineage & Read APIs ✅
Goal: Model provenance and expose read-only metadata APIs.

Definition of Done:
- [x] DatasetSource + DatasetRelease lineage models exist
- [x] Uniqueness/idempotency constraints exist at DB layer
- [x] Controller → Service → Repository layering is enforced
- [x] DTOs are separate from entities for API responses

Work Items:
- [x] Implement lineage entities and repositories
- [x] Implement read-only service and controller endpoints
- [x] Add basic DTOs

---

### Phase 4 — Ingestion Lifecycle Skeleton ✅
Goal: Track ingestion lifecycle end-to-end without parsing real datasets.

Definition of Done:
- [x] Dev-only internal ingestion trigger exists (`/api/v1/internal/...`, guarded)
- [x] Lifecycle transitions implemented (PENDING → IMPORTED/FAILED)
- [x] Idempotency enforced via DB uniqueness; duplicates treated as no-op
- [x] At least one integration test verifies lifecycle + idempotency

Work Items:
- [x] Implement internal ingestion trigger controller
- [x] Implement DatasetIngestionService lifecycle plumbing
- [x] Add integration test for idempotency + status transitions

Links:
- Commit: https://github.com/kborn/wai_and_watts/commit/2112393c481f6ad2113f32d4ffcb0fceb4447358

Notes:
- The internal ingestion endpoint is a plumbing trigger, not a mock of external providers. It simulates “a new release arrived” to exercise lifecycle, timestamps, and idempotency without fetching/parsing.
- For tests, the controller is enabled under the `test` profile (H2) to avoid hitting the dev Postgres.
---

### Phase 5 — Build & Test Guardrails ✅
Goal: Prevent broken builds from being merged; establish baseline CI discipline.

Definition of Done:
- [x] CI runs on push + pull_request
- [x] CI builds backend successfully
- [x] CI runs tests; failures fail the workflow
- [x] README includes “how to build/test locally” instructions

Work Items:
- [x] Add GitHub Actions workflow
- [x] Document local build/test commands

Links:
- PR: https://github.com/kborn/wai_and_watts/pull/1

Notes:
- CI uses in-memory DB (H2) initially
- Postgres/Testcontainers may be added later if dialect fidelity is required
- Workflow file: `.github/workflows/ci.yml`
- Local pre-push hooks are optional convenience; CI is authoritative.
---

### Phase 6 — First Real Dataset Ingestion (Fixture-first) 🟡
Goal: Ingest and persist the first *real* interpreted dataset end-to-end, using fixtures (not live downloads) to prove the pipeline.

Definition of Done:
- [x] A first domain schema (tables) exists for the chosen dataset (LAWA or MBIE)
- [x] A fixture file (or set of fixtures) is committed under `backend/src/test/resources/fixtures/<dataset>/...`
- [x] A dataset-specific parser ingests the fixture into domain objects
- [x] Domain rows are persisted and linked to `dataset_release_id`
- [x] Ingestion uses the existing lifecycle plumbing:
    - creates/uses DatasetRelease (PENDING)
    - dedupes via (dataset_source_id, content_hash)
    - marks IMPORTED on success / FAILED on error
- [x] One integration test proves:
    - ingest fixture → rows written → status IMPORTED
    - repeat ingest (same content) → no duplicate rows/releases
- [x] Read-only API endpoint(s) expose the ingested domain data

Work Items:
- [x] Choose first dataset: MBIE electricity generation
- [x] Add domain tables + Flyway migration (V6__mbie_generation.sql)
- [x] Add JPA entities + repositories for domain tables
- [x] Implement parser for fixture format (CSV)
- [x] Implement “dataset ingester” service that:
    - loads fixture bytes
    - computes content hash (SHA-256 of raw fixture bytes)
    - calls lifecycle/orchestrator
    - persists domain data
- [x] Add integration test using fixture(s)
- [x] Add read-only controller/service for domain data

Notes:
- **Fixtures are required in Phase 6.** Do not add HTTP fetch/download yet.
- If the fixture format is XLSX, consider CSV export fixtures to keep parsing lighter early.
 - Fixture path convention for MBIE: `backend/src/test/resources/fixtures/mbie/generation/`
 - Source code handling: use placeholder strings initially with normalization; persist both `source_code_raw` and `source_code_norm`; introduce enums later when vocabulary stabilizes.
 - Dataset source code: `mbie.generation.annual`

API sketch (non-binding, to reduce micro-decisions later):
- Endpoint: `GET /api/v1/mbie/generation`
- DTO: `MbieGenerationRecordDto { period (YYYY-MM), source (normalized), sourceRaw (original), generationMwh }`

Links:
- Commit: [docs(progress, decisions): refine Phase 6 MBIE plan — fixture-first contract, normalization, API sketch](https://github.com/kborn/wai_and_watts/commit/2334bafaf238353a70d866302088e30619ff7fd4)
- Commit: [chore(docs): lock Phase 6 MBIE contract + clarify doc responsibilities](https://github.com/kborn/wai_and_watts/commit/f05725f7a7a902f03696f3b641480c21192330a5)
- PR: [feat(db): add MBIE generation schema, entity/repo, and test](https://github.com/kborn/wai_and_watts/pull/2)
- PR: [feat(ingestion): add MBIE CSV parser with normalization and unit test](https://github.com/kborn/wai_and_watts/pull/3)
- PR: [feat(ingestion): wire MBIE fixture ingestion with persistence and idempotency](https://github.com/kborn/wai_and_watts/pull/4)
- PR: [feat(service/mbie): add MBIE generation read service and DTO with unit tests](https://github.com/kborn/wai_and_watts/pull/5)
- PR: [feat(api/mbie): add public read endpoint GET /api/v1/mbie/generation with validation and tests](https://github.com/kborn/wai_and_watts/pull/6)

---

### Phase 7 — MBIE Quarterly Generation Ingestion (Prove Extensibility)
Goal: Add a second dataset ingestion that reuses the same lifecycle and patterns, proving the architecture scales across sources.

Definition of Done:
- [ ] `specs/phase-7-<dataset>-ingestion.md` exists (dataset rationale + scope + acceptance criteria)
- [x] `design/<dataset>-schema.md` exists (schema + constraints + fixture contract + normalization rules)
- [x] `decisions.md` includes Phase 7 decision entries (dataset selection + any modeling decisions)
- [x] Second dataset domain schema exists (Flyway migration)
- [x] Fixture(s) committed for second dataset (test resources)
- [ ] Parser + ingester implemented for second dataset (reuses lifecycle)
- [ ] Integration test proves:
  - lineage idempotency (dataset_source_id + content_hash), and
  - domain persistence with no duplicate rows per release
- [ ] Read-only API endpoint(s) expose second dataset data
- [ ] Shared ingestion abstractions remain clean (no copy/paste drift)

Work Items:
- [x] Record Phase 7 decision brief in decisions.md
- [ ] Create specs/phase-7-mbie-quarterly-ingestion.md
- [x] Create design/mbie-quarterly-schema.md
- [ ] Select second dataset + exact source URL + table/sheet name(s) + unit semantics
- [x] Define second dataset schema + migration
- [x] Create fixture(s) matching the canonical contract for this dataset
- [ ] Implement parser/ingester using fixtures
- [ ] Add read APIs + integration tests
- [ ] Refactor common ingestion utilities only if needed (Phase 6 tests must pass unchanged)

Extensibility proof criteria:
- Shared ingestion lifecycle reused without modification
- Dataset-specific parser implements a common ingestion interface
- Domain schema remains separate from lineage schema
- Integration test proves idempotency for dataset #2
- Read-only API exposes dataset #2 without touching dataset #1 code paths
 - [ ] Remove legacy alias `GET /api/v1/mbie/generation` after quarterly lands; keep `/api/v1/mbie/generation/annual` as canonical
 - [ ] Introduce MBIE Quarterly with variant-explicit naming (`mbie.generation.quarterly`), parallel table and API (`/api/v1/mbie/generation/quarterly`)
 - [ ] Ensure fixtures follow variant-aware paths: `fixtures/mbie/generation/{annual|quarterly}/...`

Notes:
- Still fixture-first. Consider live download only after Phase 7 is stable.
- Avoid “Phase 7 refactors Phase 6”: keep Phase 6 passing unchanged; record any exceptions in decisions.md.
- Fixture review: header and field names match contract (`period_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh`). Biogas normalization aligned to `OTHER` for consistency with Phase 6 annual.

Links:
- PR: [feat(db): add MBIE quarterly schema + entity/repo and repository test](https://github.com/kborn/wai_and_watts/pull/12)
- PR: [feat(ingestion/mbie-quarterly): add CSV parser with normalization and unit test](https://github.com/kborn/wai_and_watts/pull/13)

---

### Phase 8 — Insights & LLM Layer (Grounded Explanations)
Goal: Provide grounded explanations based on persisted facts (no free-form reasoning).

Definition of Done:
- [ ] “Fact Pack” object defined for each dataset type
- [ ] Endpoint returns explanation grounded strictly in fact pack
- [ ] Guardrails: citations/attribution strategy, refusal behavior, and input validation
- [ ] Basic evaluation: canned examples to catch regressions

Work Items:
- [ ] Define FactPack schema(s) for domain data (dataset-specific)
- [ ] Implement FactPack builder from persisted tables
- [ ] Implement explanation service that calls an LLM with fact-pack-first prompt
- [ ] Add tests for grounding constraints (structural checks)
- [ ] Document AI safety constraints in `ai_usage.md`

Notes:
- No forecasting/prediction. Explanations must ground to stored facts.

---

### Phase 9 — Polish & Presentation (Portfolio-Ready)
Goal: Make the repo recruiter-friendly and easy to run/demo.

Definition of Done:
- [ ] README: what it is, why it matters, how to run, how to test
- [ ] Sample queries / curl examples
- [ ] Short architecture overview (or diagram)
- [ ] Clear “AI usage” disclosure and workflow explanation
- [ ] A 5-minute demo script section

Work Items:
- [ ] Improve README and add usage examples
- [ ] Add a short demo walkthrough
- [ ] Final pass on docs (context/progress/decisions/ai usage)
- [ ] Optional: thin frontend or screenshots

---
