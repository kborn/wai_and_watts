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
- **Active Phase:** Phase 10 — Live Ingestion 🟡
- **Status:** In progress

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

### Phase 6 — First Real Dataset Ingestion (Fixture-first) ✅
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

### Phase 7 — MBIE Quarterly Generation Ingestion (Prove Extensibility) ✅
Goal: Add a second dataset ingestion that reuses the same lifecycle and patterns, proving the architecture scales across sources.

Definition of Done:
- [x] `specs/phase-7-<dataset>-ingestion.md` exists (dataset rationale + scope + acceptance criteria)
- [x] `design/<dataset>-schema.md` exists (schema + constraints + fixture contract + normalization rules)
- [x] `decisions.md` includes Phase 7 decision entries (dataset selection + any modeling decisions)
- [x] Second dataset domain schema exists (Flyway migration)
- [x] Fixture(s) committed for second dataset (test resources)
- [x] Parser + ingester implemented for second dataset (reuses lifecycle)
- [x] Integration test proves:
  - lineage idempotency (dataset_source_id + content_hash), and
  - domain persistence with no duplicate rows per release
- [x] Read-only API endpoint(s) expose second dataset data
- [x] Shared ingestion abstractions remain clean (no copy/paste drift)

Work Items:
- [x] Record Phase 7 decision brief in decisions.md
- [x] Create specs/phase-7-mbie-quarterly-ingestion.md (`specs/003-phase-7-mbie-quarterly-ingestion.md`)
- [x] Create design/mbie-quarterly-schema.md
- [x] Select second dataset + exact source URL + table/sheet name(s) + unit semantics
- [x] Define second dataset schema + migration
- [x] Create fixture(s) matching the canonical contract for this dataset
- [x] Implement parser/ingester using fixtures
- [x] Add read APIs + integration tests

Extensibility proof criteria:
- [x] Shared ingestion lifecycle reused without modification
- [x] Dataset-specific parser implements a common ingestion interface
- [x] Domain schema remains separate from lineage schema
- [x] Integration test proves idempotency for dataset #2
- [x] Read-only API exposes dataset #2 without touching dataset #1 code paths
 - [x] Introduce MBIE Quarterly with variant-explicit naming (`mbie.generation.quarterly`), parallel table and API (`/api/v1/mbie/generation/quarterly`)
 - [x] Ensure fixtures follow variant-aware paths: `fixtures/mbie/generation/{annual|quarterly}/...`

Notes:
- Still fixture-first. Consider live download only after Phase 7 is stable.
- Avoid “Phase 7 refactors Phase 6”: keep Phase 6 passing unchanged; record any exceptions in decisions.md.
- Fixture review: header and field names match contract (`period_year,period_quarter,fuel_type_raw,fuel_type_norm,generation_gwh`). Biogas normalization aligned to `OTHER` for consistency with Phase 6 annual.

Links:
- PR: [feat(db): add MBIE quarterly schema + entity/repo and repository test](https://github.com/kborn/wai_and_watts/pull/12)
- PR: [feat(ingestion/mbie-quarterly): add CSV parser with normalization and unit test](https://github.com/kborn/wai_and_watts/pull/13)
- PR: [feat(ingestion/mbie-quarterly): wire fixture ingestion with lifecycle + idempotency](https://github.com/kborn/wai_and_watts/pull/14)
- PR: [feat(api/mbie-quarterly): add public read endpoint with filters and tests](https://github.com/kborn/wai_and_watts/pull/15)
- PR: [test(service/mbie-quarterly): add read service unit tests for mapping and filters](https://github.com/kborn/wai_and_watts/pull/16)
---

### Phase 8 — LAWA Water Quality “State” Ingestion (Cross-Domain Proof) ✅
Goal: Add a third dataset ingestion in a new domain (water quality), proving lifecycle + patterns generalize beyond MBIE electricity.

Definition of Done:
- [x] `specs/phase-8-<dataset>-ingestion.md` exists (dataset rationale + scope + acceptance criteria)
- [x] `design/<dataset>-schema.md` exists (schema + constraints + fixture contract + normalization rules)
- [x] `decisions.md` includes Phase 8 decision entries (dataset selection + modeling boundary decisions)
- [x] LAWA `dataset_source.code` created (e.g., `lawa.water_quality.state.multi_year`)
- [x] LAWA domain schema exists (Flyway migration)
- [x] Fixture(s) committed for LAWA dataset (test resources)
- [x] Parser + ingester implemented for LAWA dataset (reuses lifecycle)
- [x] Integration test proves:
  - lineage idempotency (dataset_source_id + content_hash), and
  - domain persistence with no duplicate rows per release
- [x] Read-only API endpoint(s) expose LAWA state data
- [x] Shared ingestion abstractions remain clean (no copy/paste drift)

Work Items:
- [x] Record Phase 8 decision brief in decisions.md:
  - dataset selection (exact source URL + sheet/table)
  - minimal modeling boundary (published state interpretations, not raw telemetry)
  - normalization approach for indicators/grades/units (as applicable)
- [x] Create `specs/phase-8-lawa-state-trend-multi-year-ingestion.md` (or numbered equivalent)
- [x] Create `design/lawa-state-multi-year-schema`
- [x] Select LAWA table/sheet(s) and lock:
  - exact URL(s)
  - table/sheet name(s)
  - unit semantics + time period semantics
- [x] Define LAWA schema + migration (minimal, interview-friendly)
- [x] Create fixture(s) matching canonical contract for LAWA
- [x] Implement parser/ingester using fixtures
- [x] Add read APIs + integration tests
- [x] Ensure variant-aware paths:
  - fixtures: `backend/src/test/resources/fixtures/lawa/state/multi_year/...`
  - APIs: `/api/v1/lawa/state/multiyear`

Cross-domain proof criteria:
- [x] Shared ingestion lifecycle reused without modification
- [x] Dataset-specific parser implements a common ingestion interface
- [x] Domain schema remains separate from lineage schema
- [x] Integration tests validate Phases 6/7 still pass unchanged
- [x] LAWA ingestion adds minimal new shared utilities only if justified (record in decisions.md)

Notes:
- Builder GPT must implement Phase 8 strictly per specs/004 and design/007; no schema or lifecycle refactors allowed.
- Still fixture-first. Live download deferred until after Phase 8 stabilizes.
- Keep LAWA modeling intentionally narrow: “state” tables only (no raw monitoring time series).
- Avoid “Phase 8 refactors Phase 6/7”: keep earlier phases passing unchanged; record any exceptions in decisions.md.

Links:
- PR: [feat(db/lawa): add LAWA state (multi‑year) schema + entity/repo and repository test](https://github.com/kborn/wai_and_watts/pull/18)
- PR: [feat(ingestion/lawa-state-multi-year): add CSV parser normalization and unit test](https://github.com/kborn/wai_and_watts/pull/19)
- PR: [feat(ingestion/lawa-state): add multi‑year ingestion service + idempotency and integration test](https://github.com/kborn/wai_and_watts/pull/20)
- PR: [feat(api/lawa-state): add public read endpoint with filters and tests](https://github.com/kborn/wai_and_watts/pull/21)

---

### Phase 9 — LAWA Trend Ingestion 🟡
Goal: Add a second LAWA dataset ingestion (water quality trend), proving lifecycle + patterns generalize beyond MBIE electricity and complement Phase 8 “state” data.

Definition of Done:
- [x] `specs/phase-9-lawa-trend-multi-year-ingestion.md` exists
- [x] `design/lawa-trend-multi-year-schema.md` exists
- [x] `decisions.md` includes Phase 9 decision entries (dataset + period semantics + normalization + cross-sheet derivation contract)
- [x] `dataset_source.code` created:
  - `lawa.water_quality.trend.multi_year`
- [x] LAWA trend domain schema exists (Flyway migration)
- [x] Fixture committed:
  - `backend/src/test/resources/fixtures/lawa/water_quality/trend/multi_year/...`
- [x] Parser + ingester implemented (reuses lifecycle)
 - [x] Integration test proves:
  - lineage idempotency
  - domain persistence
  - deterministic period derivation from paired State fixture slice
- [x] Read-only API exposes LAWA trend data
- [x] Phase 8 LAWA state ingestion remains unchanged

Work Items:
- [x] Create dataset source record
- [x] Implement schema migration
- [x] Implement Trend parser
- [x] Implement trend normalization layer
- [x] Implement ingestion wiring
- [x] Generate deterministic fixture slice aligned to Phase 8
- [x] Add integration test coverage
- [x] Add API read exposure

Notes:
- Fixtures must:
  - Use same regions/sites as Phase 8 fixture
  - Be derived from the same workbook snapshot as Phase 8 fixture slice
  - Preserve multiple trend window lengths
  - Preserve raw indicator + trend text
  - Be deterministic for integration tests
- Trend period derivation uses State sheet context from the same workbook artifact; Trend ingestion must not query State domain tables.

Non-Goals:
- No trend computation
- No joining state + trend at ingestion time
- No live download orchestration yet
- No taxonomy expansion

Links:
- PR: [feat(ingestion/lawa-trend): add CSV parser + DTO with normalization and fixture test](https://github.com/kborn/wai_and_watts/pull/22)
- PR: [feat(ingestion/lawa-trend): wire multi‑year ingestion with schema + idempotent test](https://github.com/kborn/wai_and_watts/pull/23)
- PR: [feat(api/lawa-trend): add read service + controller with filters; tests; update progress](https://github.com/kborn/wai_and_watts/pull/24)

### Phase 10 — Live Ingestion 🟡
Goal: Fetch and ingest real datasets end-to-end (not just fixtures).  
This is a **manual operator-triggered process**. Wai & Watts intentionally does **not** include scheduling, orchestration, or automated polling of data publishers.

Definition of Done:
- [ ] Live download script exists per dataset family (MBIE, LAWA)
- [ ] Raw source files can be stored locally or in configured storage path
- [ ] Dataset release creation supports ingestion from real files
- [ ] Content hashing works for real source files
- [ ] Ingestion pipeline runs successfully against real files
- [ ] Re-running ingestion on same file is idempotent
- [ ] Re-running ingestion on new file creates new dataset release
- [ ] Parser logic contains no fixture-only assumptions
- [ ] Failure modes are documented (schema drift, missing columns, corrupt download)
- [ ] Manual operator runbook exists
- [ ] Example real ingestion execution documented in repo

Work Items:
- [ ] Implement MBIE live download script
- [ ] Implement LAWA live download script
- [ ] Implement dataset release registration for real files
- [ ] Validate content hashing + deduplication behavior
- [ ] Validate parser compatibility with real-world file variation
- [ ] Add manual ingestion CLI entrypoints or scripts
- [ ] Document manual ingestion workflow
- [ ] Add example live ingestion execution documentation
- [ ] Add integration test using real file snapshot (optional)

Notes:
- Live ingestion must preserve dataset lineage, dataset immutability per release, and idempotent ingestion behavior.
- Schema drift must surface as explicit ingestion failure (no silent coercion).
- Live ingestion is intentionally **manual + reproducible**, not automated infrastructure.

Non-Goals:
- No scheduler (Airflow, cron, event triggers)
- No automated publisher polling
- No incremental ingestion platform
- No freshness SLAs or monitoring pipelines
- No data lake / raw zone architecture  

### Phase 11 — Insights & LLM Layer (Grounded Explanations)
Goal: Produce grounded, non-hallucinatory explanations over persisted facts (MBIE annual + quarterly + LAWA) and publish a small set of curated insights.

Definition of Done:
- [ ] `design/fact-pack-contract.md` exists (fact pack schema + provenance rules)
- [ ] Grounding rules documented (no guessing, no forecasting, cite fact pack fields)
- [ ] Explanation endpoint(s) implemented that:
  - build fact packs from DB queries
  - call the LLM with fact-pack-first prompting
  - return explanation + citations to fact pack fields
- [ ] Refusal behavior documented and tested at least once (e.g., unsupported question)
- [ ] `Insights.md` exists with 3–5 grounded findings:
  - at least 2 MBIE (annual/quarterly)
  - at least 1 LAWA
  - each insight links to the query/fact pack used
- [ ] No autonomous agents committing code; human-in-the-loop remains enforced

Work Items:
- [ ] Record Phase 9 decision brief in decisions.md (fact pack contract + grounding + refusal posture)
- [ ] Implement fact pack builders per dataset (MBIE annual, MBIE quarterly, LAWA)
- [ ] Implement explanation service (pluggable LLM provider)
- [ ] Add minimal tests for:
  - fact pack correctness (query returns expected fields)
  - explanation output includes citations/field references
- [ ] Write `Insights.md` with grounded tables/charts (static ok)
- [ ] Update `docs/ai-dev/ai_usage.md` with Phase 9 practices and examples

Notes:
- Explanations must be tied to persisted DB rows and explicit fact pack fields.
- The LLM explains; the database remains source of truth.

---

### Phase 12 — Frontend (Thin Storytelling Client) [Intentionally Minimal]
Goal: Provide a thin UI for demo and interviews without shifting business logic into the frontend.

Definition of Done:
- [ ] Frontend skeleton exists (framework TBD)
- [ ] Minimal views for:
  - MBIE generation (annual + quarterly toggle)
  - LAWA state/trend browse view
  - “Explain this” call to explanation endpoint
- [ ] No business logic in frontend (frontend is a client only)
- [ ] README/DEMO docs updated with how to run frontend + backend together

Work Items:
- [ ] Choose minimal frontend approach (React/Vite or Next.js; defer decision until Phase 10 starts)
- [ ] Implement 2–3 simple pages + API wiring
- [ ] Add screenshots for README (optional)

Notes:
- Frontend is for demoability, not product completeness.
---

### Phase 13 — Polish & Presentation (Portfolio-Ready)
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

### Final Polish & Deferred Portfolio Tasks

These tasks were intentionally deferred to prioritize core engineering velocity.  
Complete before marking Wai & Watts “portfolio-ready.”

#### Tier 1 — Must-Have Portfolio Narrative
- [ ] Add **Project Scope & Narrative** and **Non-Goals & Tradeoffs** to README
- [ ] Add **Architecture Evolution Narrative** (Phase 6 → Phase 7 → future phases)
- [ ] Add **Repo Map** (REPO_MAP.md or README section describing key directories)

#### Tier 1 — Demo-First Entry Point
- [ ] Add DEMO.md or README Demo section:
  - [ ] Local run instructions
  - [ ] Example curl/Postman queries (annual + quarterly)
  - [ ] Example grounded LLM explanation request
- [ ] Add a **5-minute interview walkthrough script**

#### Tier 2 — Engineering Documentation Polish
- [ ] Replace PR links in progress.md with canonical repo-relative paths
- [ ] Add architectural diagram (ingestion lifecycle + domain persistence)
- [ ] Add dataset taxonomy table (mbie.generation.annual, mbie.generation.quarterly, etc.)
- [ ] Normalize and backfill decisions.md for early implicit decisions
- [ ] Add decision format contract section for future contributors/GPTs
- [ ] Cohesion across docs. Where do table schema specs belong? design or or spec? Do we have all tables defined?

#### Tier 2 — Environmental Storytelling Deliverables
- [ ] Add Insights.md with 3–5 grounded findings (charts/tables allowed):
  - renewables trend
  - fossil backup spikes
  - seasonal hydro variability
- [ ] Ensure insights link directly to persisted DB queries

#### Tier 3 — AI & System Retrospective
- [ ] Add AI development retrospective in docs/ai-dev/
  - roles used (Builder, Staff, PM)
  - guardrails and human-in-the-loop policy
  - examples of AI-assisted engineering decisions
  - friction and how it was handled i.e. chat gpt sessions become unusably slow over time. Is there enough context in repo docs to start a new agent?
    - can this friction be reduced in onboarding in future projects?
  - Can we create a template of AI docs to be re-used in future projects to onboard agents?
- [ ] Document AI grounding contract (Fact Pack, refusal behavior, citation rules)
- [ ] We noticed in step 8 that the architecture and contracts as well as the README were forgotten about since they were created. 
  - Who should have been reading and updating these? 
  - Did it matter in the end? Was it even worthwhile to create them?
  - Do we go back and retrofit the to the code now? 

#### Tier 3 — Production-Readiness Signals (Doc-Only)
- [ ] Document intended ingestion error handling and retries
- [ ] Document reprocessing/backfill strategy
- [ ] Document data quality validation approach
- [ ] Document logging/observability strategy

#### Codebase Hygiene
- [ ] Ensure naming consistency (mbieGenerationAnnual vs mbieGenerationQuarterly)
- [ ] Add schema migration history summary
- [ ] Add README pointers to fixtures and test strategy
- [ ] Write a 1-paragraph “Roadmap Philosophy” ection that explains why this sequencing mirrors real platform evolution (PM suggestion)


#### General TODOs
- [ ] Unique index on MBIE annual
- [ ] idx for READ API queries for LAWA state multi year
- [ ] normalize meta data columns across all tables
- [ ] create abstract csv parser
---

#### AI Onboarding & Documentation Validation Tasks
These tasks validate that repository documentation is sufficient for new AI agents.
- [x] Add AI Onboarding Runbook (`docs/ai-dev/AI_ONBOARDING_RUNBOOK.md`)
- [x] Add AI Onboarding Checklist (`docs/ai-dev/AI_ONBOARDING_CHECKLIST.md`)
- [ ] Validate onboarding with a fresh AI session (no bootstrap)
- [ ] Record onboarding friction points in decisions.md
- [ ] Backfill missing documentation based on onboarding failures

---


---
