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
- **Active Phase:** Phase 17 — Capability declaration and NL determinism
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
- DTO: `MbieGenerationRecordDto { period (YYYY-MM), fuelType (normalized), fuelTypeRaw (original), generationMwh }`


---

### Phase 7 — MBIE Quarterly Generation Ingestion (Prove Extensibility) ✅
Goal: Add a second dataset ingestion that reuses the same lifecycle and patterns, proving the architecture scales across sources.

Definition of Done:
- [x] `engineering/specs/phase-7-<dataset>-ingestion.md` exists (dataset rationale + scope + acceptance criteria)
- [x] `engineering/design/<dataset>-schema.md` exists (schema + constraints + fixture contract + normalization rules)
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
- [x] Create `engineering/specs/003-phase-7-mbie-quarterly-ingestion.md`
- [x] Create `engineering/design/005-mbie-quarterly-schema.md`
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

---

### Phase 8 — LAWA Water Quality “State” Ingestion (Cross-Domain Proof) ✅
Goal: Add a third dataset ingestion in a new domain (water quality), proving lifecycle + patterns generalize beyond MBIE electricity.

Definition of Done:
- [x] `engineering/specs/phase-8-<dataset>-ingestion.md` exists (dataset rationale + scope + acceptance criteria)
- [x] `engineering/design/<dataset>-schema.md` exists (schema + constraints + fixture contract + normalization rules)
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
- [x] Create `engineering/specs/phase-8-lawa-state-trend-multi-year-ingestion.md` (or numbered equivalent)
- [x] Create `engineering/design/lawa-state-multi-year-schema`
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
- Builder GPT must implement Phase 8 strictly per `engineering/specs/004-phase-8-lawa-state-multi-year-ingestion.md` and `engineering/design/007-lawa-state-multi-year-schema.md`; no schema or lifecycle refactors allowed.
- Still fixture-first. Live download deferred until after Phase 8 stabilizes.
- Keep LAWA modeling intentionally narrow: “state” tables only (no raw monitoring time series).
- Avoid “Phase 8 refactors Phase 6/7”: keep earlier phases passing unchanged; record any exceptions in decisions.md.


---

### Phase 9 — LAWA Trend Ingestion ✅
Goal: Add a second LAWA dataset ingestion (water quality trend), proving lifecycle + patterns generalize beyond MBIE electricity and complement Phase 8 “state” data.

Definition of Done:
- [x] `engineering/specs/phase-9-lawa-trend-multi-year-ingestion.md` exists
- [x] `engineering/design/lawa-trend-multi-year-schema.md` exists
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


### Phase 10 — Live Ingestion ✅
Goal: Fetch and ingest real datasets end-to-end (not just fixtures).
This is a **manual operator-triggered process**. Wai & Watts intentionally does **not** include scheduling, orchestration, or automated polling of data publishers.

Definition of Done:
- [x] Live download script exists per dataset family (MBIE, LAWA)
- [x] Raw source files are stored on the local filesystem and provided to ingestion via explicit file path
- [x] Dataset release creation supports ingestion from real files
- [x] Content hashing works for real source files
- [x] Ingestion pipeline runs successfully against real files
- [x] Re-running ingestion on same file is idempotent
- [x] Re-running ingestion on new file creates new dataset release
- [x] Parser logic contains no fixture-only assumptions
- [x] Validate parser compatibility with real-world file variation
- [x] Failure modes are documented (schema drift, missing columns, corrupt download)
- [x] Manual operator runbook exists
- [x] Example real ingestion execution documented in repo

Work Items:
- [x] Implement local file ingestion foundation
- [x] Add file ingestion methods to all dataset ingestion classes
- [x] Add comprehensive file ingestion tests for all datasets
- [x] Add findByDatasetReleaseId methods to all repositories
- [x] Implement MBIE live download script
- [x] Implement LAWA live download script
- [x] Implement dataset release registration for real files
- [x] Validate content hashing + deduplication behavior
- [x] Add manual ingestion CLI entrypoints or scripts
- [x] Document manual ingestion workflow
- [x] Add example live ingestion execution documentation
- [x] Add integration test using real file snapshot (optional)

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


### Phase 11 — Insights & LLM Layer (Grounded Explanations) ✅
Goal: Produce grounded, non-hallucinatory explanations over persisted facts (MBIE annual + quarterly + LAWA) and publish a small set of curated insights.

Definition of Done:
- [x] `engineering/design/fact-pack-contract.md` exists (fact pack schema + provenance rules)
- [x] Grounding rules documented (no guessing, no forecasting, cite fact pack fields)
- [x] Explanation endpoint(s) implemented that:
- build fact packs from DB queries
- call the LLM with fact-pack-first prompting
- return explanation + citations to fact pack fields
- [x] Refusal behavior documented and tested at least once (e.g., unsupported question)
- [x] `Insights.md` exists with 3–5 grounded findings:
- at least 2 MBIE (annual/quarterly)
- at least 2 LAWA (state/trend)
- each insight links to the query/fact pack used
- [x] No autonomous agents committing code; human-in-the-loop remains enforced

Work Items:
- [x] Record Phase 11 decision brief in decisions.md (fact pack contract + grounding + refusal posture)
- [x] Implement fact pack builders per dataset (MBIE annual, MBIE quarterly, LAWA)
- [x] Implement explanation service (pluggable LLM provider)
- [x] Add minimal tests for:
- fact pack correctness (query returns expected fields)
- explanation output includes citations/field references
- [x] Write `Insights.md` with grounded tables/charts (static ok)
- [x] Update `engineering/ai_usage.md` with Phase 11 practices and examples

Notes:
- Explanations must be tied to persisted DB rows and explicit fact pack fields.
- The LLM explains; the database remains source of truth.


---
## Phase 12 — Natural Language Query Interface ✅

### Goal
Add natural language question support while maintaining all fact-pack grounding guarantees

This phase introduces **intent parsing only**. It does NOT change fact pack construction, data lineage, or explanation safety architecture.

---

### Definition of Done

- [x] Natural language endpoint exists: `POST /api/v1/explanations/ask`
- [x] Intent parsing maps natural language → structured ExplanationRequest
- [x] Parsed intent is validated against supported `question_type` enum
- [x] Invalid or ambiguous intents trigger deterministic refusal
- [x] Structured endpoint (`POST /api/v1/explanations`) remains supported and unchanged
- [x] Integration test: NL → intent → fact pack → explanation
- [x] Refusal test: unsupported NL question → explicit refusal

---

### Work Items

- [x] Implement IntentParserService (LLM structured output or function calling)
- [x] Add NL endpoint routing to existing explanation pipeline
- [x] Add validation layer for parsed intents (question type + filters)
- [x] Add logging of raw question + parsed intent for audit/debug
- [x] Test with "minimal ship" question subset
- [x] Document NL parsing contract (design doc optional)

---

### Guardrails

- Natural language layer may ONLY output:
- questionType
- datasetSource(s)
- filters
- Natural language layer may NOT:
- Generate facts
- Access database
- Bypass fact pack builders
- Bypass refusal model
- Supported question types are spec-owned:
- IntentParser must map only to an existing question_type enum value;
- it must never introduce new question types via “best guess” or synonyms.
- Unknown intents → refusal.
- UI is single-turn and stateless:
- each submission is evaluated independently;
- no conversation history, no follow-up context, no multi-turn flows.
- IntentParser may only emit existing question_type enum values.

---

### Non-Goals

- Conversational chat
- Multi-turn reasoning
- Forecasting or causal inference
- Client-side intent parsing

---

### Architecture Reminder

```
User NL Question
 → Intent Parser (LLM - routing only)
 → Structured Explanation Request
 → Fact Pack Builder (DB + deterministic computation)
 → Explanation Provider (Stub or LLM renderer)
 → Explanation + Citations
```


---

### Implementation Summary ✅

**Delivered in single focused PR:**
- `POST /api/v1/explanations/ask` natural language endpoint
- IntentParserService with rule-based parsing (Phase 12 compliant)
- RequestValidationService enforcing contract schemas
- IntentParseResponse DTO with deterministic refusal handling
- Comprehensive test coverage (6/6 tests passing)
- All guardrails maintained and no architectural drift

**Architecture Compliance:**
✅ Natural language layer only outputs: questionType, datasetSource, filters
✅ No database access in intent parser
✅ Existing `/api/v1/explanations` endpoint preserved unchanged
✅ Fact Pack construction, data lineage, explanation safety untouched
✅ Deterministic refusals for ambiguous/unsupported requests

---

## Phase 13 — Frontend (Thin Storytelling Client) ✅
Goal: Deliver a production-credible client surface over the existing backend, with clean boundaries and room to grow.

Canonical requirements:
- PR Guideline: ../PHASE_13_PR_EXECUTION_GUIDELINE.md
- This phase must be broken out in PRs according to the phase 13 guideline doc
- Product Slice: `archive/docs_legacy/product/phase_13_product_slice.md`
- SE Constraints: `engineering/design/014_frontend_design_constraints.md`

Definition of Done:
- [x] React + TypeScript + Vite frontend scaffold
- [x] TanStack Query used for server state
- [x] Tailwind used for styling
- [x] React Router configured
- [x] Ask flow implemented (NL question → explanation OR refusal)
- [x] Results view implemented (explanation + citations + refusal variants)
- [x] Browse views implemented (table-first):
- [x] MBIE generation: annual/quarterly toggle (+ optional fuelType filter)
- [x] LAWA water quality: state/trend toggle (+ optional region/indicator filters)
- [x] "Explain this" entry point from browse views
- [x] Frontend contains ZERO domain/explanation logic (backend remains authoritative)
- [x] Playwright smoke tests implemented:
- [x] Ask success → explanation + citations render
- [x] Ask refusal → refusal UI renders
- [x] README updated with frontend + backend run instructions

Work Items:
- [x] Scaffold Vite + React + TS
- [x] Implement typed API client layer
- [x] Implement routes: Ask, Results, Browse (MBIE, LAWA)
- [x] Implement "Explain this" using existing endpoints (no new backend computation)
- [x] Add Playwright harness + 2 smoke tests
- [x] Optional: simple charts if time permits (must be purely presentational; table view remains primary)

Notes:
- Frontend is a production-credible client surface; scope is intentionally constrained to keep iteration fast and boundaries clean.
- Progress.md is the execution checklist; page behaviors and UX expectations live in the [Product Slice](../archive/docs_legacy/product/phase_13_product_slice.md) doc.


---

## Phase 14 — UI / UX Styling (Portfolio-Ready UI) ✅

Goal: Make the UI feel like a real product surface (clean, modern, consistent) while keeping frontend architecture thin and backend-authoritative.

Definition of Done:
- [x] Global layout + typography polished
- [x] Navigation styled with clear active states
- [x] Form controls styled consistently
- [x] Loading states upgraded (skeletons/spinners)
- [x] Error + refusal states upgraded (cards/callouts)
- [x] Tables styled with empty states + scroll handling
- [x] MBIE + LAWA pages include charts based on selected filters (table remains primary)
- [x] Basic responsive pass complete
- [ ] Screenshots added to README (optional)

Work Items:
- [x] Establish Tailwind layout + spacing + typography tokens
- [x] Build reusable UI components (Button, Select, Card, Table, Callout)
- [x] Upgrade Ask + Results page styling
- [x] Upgrade MBIE + LAWA page styling
- [x] Add presentational charts using existing data (no new backend computation)
- [ ] Optional light/dark mode

Notes:
- Phase completed across 5 PRs:
- PR 14.1: Styling foundation + layout shell
- PR 14.2: Core reusable UI components
- PR 14.3: Ask + Results polish
- PR 14.4: LAWA browse page polish
- PR 14.5: Charts (presentational)
- Charts are presentational only, table remains primary




## Phase 14A — UI / UX Styling (Portfolio-Ready UI) ✅

- [x] MBIE timeline visualization upgrade (ECharts)
- [x] Replace MBIE bar chart primary visualization with time-series line chart
- [x] Implement brush zoom interaction with reset to full timeline
- [x] Support multi-fuel line comparison
- [x] Implement optional "Total (sum of displayed fuels)" display-aggregate line
- [x] Move MBIE table to secondary surface (tab / collapse)
- [x] Ensure table reflects page filters AND zoom window

Notes:
- MBIE visualization enhancement follows 2026-02-12 decisions:
- MBIE timeline visualization becomes primary exploration surface
- ECharts adopted as interactive charting standard (starting with MBIE)
- Frontend may compute display-only aggregates derived from already-fetched rows
- No backend API changes required



## Phase 14B --- LAWA Unified Visualization (Trend + State)

Goal: Implement unified LAWA visualization model aligned to dataset
semantics using:

- Trend: Classification Distribution visualization\
- State: State Band Distribution visualization (snapshot condition
    distribution)


#### Definition of Done

- [x] Implement filter gating rules
- [x] Implement sentinel normalization (-99 → NULL)
- [x] Implement Trend classification distribution chart
- [x] Implement Trend table required column contract
- [x] Implement Trend chart bucket → table filter interaction
- [x] Implement State table required column contract
- [x] Implement State band distribution chart
- [x] Implement State chart band → table filter interaction
- [x] Implement table horizontal scroll + dataset column switching
- [x] Preserve explanation workflow compatibility


#### Work Items

- [x] Add filter gating enforcement (Trend: Indicator; State: Region + Indicator)
- [x] Add sentinel normalization layer
- [x] Build Trend classification distribution chart
- [x] Build Trend table columns and sorting rules
- [x] Build Trend chart click-to-filter behavior
- [x] Build State table columns
- [x] Build State Attribute Band distribution aggregation
- [x] Build State band distribution chart
- [x] Implement State chart click-to-filter integration
- [x] Validate performance and table pagination strategy


#### Notes

- Trend is ordinal classification, not numeric magnitude
- State represents snapshot condition classification, not time-series measurement telemetry
- Tables remain canonical audit and explanation surfaces
- State chart direction changed per REPLACE_builder_spec_state_chart_14B.md:
- State is snapshot (not time-series)
- Band distribution replaces measurement-over-time



## Phase 14C --- Regional Environmental & Infrastructure Context Panel

Goal: Implement a cross-dataset situational awareness panel that
synthesizes water monitoring and energy system signals into a unified
product context surface using the established fact-pack aggregation
pattern.

This phase introduces **product narrative cohesion** without introducing
cross-dataset analytics, correlation, or composite environmental
scoring.


### Definition of Done

- [x] Backend region context fact-pack endpoint implemented and
    documented
- [x] Water Trend distribution summary aggregation implemented and
    validated
- [x] Water State band distribution summary aggregation implemented
    and validated
- [x] Energy system context summary aggregation implemented and
    validated
- [x] Frontend Context Panel UI component implemented
- [x] Required disclaimer text rendered exactly per decision contract
- [x] Panel correctly gated on region selection
- [x] Empty state handling implemented
- [x] Loading state handling implemented
- [x] Error state handling implemented
- [x] No regressions introduced to existing Trend or State
    visualization behavior
- [x] Fact-pack contract reviewed for stability and future
    extensibility

### Work Items

Backend

- [x] Create Region Context aggregation service
- [x] Implement Water Trend summary calculator
- [x] Implement Water State band distribution summary calculator
- [x] Implement MBIE energy context summary calculator
- [x] Implement region context API endpoint
- [x] Add endpoint validation and error handling
- [x] Add unit tests for summary aggregation logic

Frontend

- [x] Create Context Panel UI container component
- [x] Implement Water Monitoring Confidence section
- [x] Implement Water Direction Signal section
- [x] Implement Water Condition Signal section
- [x] Implement Energy System Context section
- [x] Implement disclaimer rendering
- [x] Implement loading skeleton state
- [x] Implement empty data state
- [x] Implement API error fallback UI

Integration

- [x] Validate region filter → context endpoint contract
- [x] Validate panel refresh behavior on filter change
- [x] Validate performance against large region datasets
- [x] Validate panel behavior when one dataset unavailable


### Notes

This phase intentionally introduces cross-dataset *contextual synthesis*
only.

It does NOT introduce:
- Cross-dataset statistical analysis
- Environmental correlation modeling
- Composite environmental health scoring
- Predictive or ML-based inference

The Context Panel exists to improve product narrative cohesion and
decision situational awareness while maintaining strict dataset semantic
integrity.

All existing visualization rules, dataset contracts, and explanation
safety patterns remain unchanged.




## Phase 15 — Polish & Presentation (Portfolio-Ready)
Goal: Converge architecture and documentation for a portfolio-ready handoff.

Status: Complete

### Definition of Done
- [x] Ask pipeline hardening complete and verified.
- [x] Coverage baseline (JaCoCo + CI threshold) enforced.
- [x] README includes scope, non-goals, architecture evolution, and run paths.
- [x] `DEMO.md` provides a 5-minute walkthrough.
- [x] Documentation structure is converged (canonical vs historical is explicit).
- [x] `progress.md` and `decisions.md` formatting is consistent and maintainable.

### Completed (Code + Verification)
- [x] Deterministic ask behavior, refusal taxonomy stabilization, and release pinning.
- [x] Repository-level filtering and indexing alignment.
- [x] LAWA normalization and trend-schema hygiene cleanup.
- [x] Test coverage floor with CI enforcement.
- [x] Postgres-dialect integration coverage for critical query paths.

Evidence (repo-relative):
- `backend/src/main/java/nz/waiwatts/explanations/`
- `backend/src/test/java/nz/waiwatts/explanations/`
- `backend/pom.xml`
- `.github/workflows/ci.yml`
- `archive/phase_notes/phase15/phase15_exit_rubric.md`
- `archive/phase_notes/phase15/phase15_generated_panel_spec.md`
- `archive/phase_notes/phase15/phase15_maturity_checklist.md`

Historical execution/audit records (archived):
- `archive/phase15_doc_convergence/`

### Historical Execution Breakdown (Preserved)
This section is intentionally preserved to show the execution rigor and sequencing used in Phase 15.

#### Tier 0 — Code Completeness (Completed)
- [x] Local development experience hardening:
  - `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`
  - README docker run instructions
- [x] Toolchain hardening:
  - JDK 21 standardization
  - Maven runtime Java 21 enforcer
  - CI JDK 21 alignment
- [x] Feature completion:
  - real LLM provider wiring + tests
  - differentiated refusal UX states
- [x] Intent parser restriction loosening under shape-based contract:
  - Gate A/B/C model adoption
  - generated pattern harness integration
  - refusal boundary and citation determinism hardening
- [x] Database/indexing improvements:
  - uniqueness/index coverage and query-path alignment
  - repository-level filtered reads replacing in-memory filtering
- [x] High-level architecture/code review and remediation execution:
  - backend + frontend review completed
  - remediation plan executed and closure verified
- [x] Tooling/dependency review:
  - Spring Boot BOM refresh
  - Apache POI refresh
  - Java runtime guardrails
- [x] Feature usage validation:
  - API versioning intent validated
  - `dataset_release` semantics validated
  - LLM stub/provider contract alignment tightened
- [x] Data normalization and cleanup:
  - metadata normalization across tables
  - region case normalization
  - abstract CSV parser introduction
  - LAWA trend `units` cleanup
- [x] Test coverage hardening:
  - JaCoCo enabled with 70% threshold
  - CI threshold enforcement
  - critical business-path test gap closure
  - Postgres-backed integration coverage and release determinism checks

#### Tier 0 — Carried-Forward Minor UI Follow-up
- [x] Ensure consistency throughout UI pages

#### Tier 1–3 Deferred/Documentation Follow-up Themes
- [x] README/demo narrative tightening and repo map clarity
- [x] Architecture diagram and dataset taxonomy placement
- [x] Insights and retrospective deliverables
- [x] Production-readiness doc notes (retries/backfill/DQ/observability)

Authoritative historical artifacts for full task-level detail:
- `archive/phase15_doc_convergence/phase15_refactoring_execution_checklist.md`
- `archive/phase15_doc_convergence/phase15_architectural_remediation_plan.md`
- `archive/phase15_doc_convergence/phase15_second_round_verification_findings.md`

### Documentation Convergence (Completed)

#### Tier 1 — Canonical Narrative
- [x] Final README rewrite for tighter onboarding flow (local dev, docker demo, operator workflow).
- [x] Add/confirm repository map section and canonical doc pointers.

#### Tier 2 — Engineering Doc Hygiene
- [x] Normalize formatting consistency in `engineering/progress.md` and `engineering/decisions.md`.
- [x] Replace remaining PR-style references in active progress entries with repo-relative evidence paths.
- [x] Refresh baseline design contracts for current runtime architecture:
  - `engineering/design/001-architecture.md`
  - `engineering/design/002-contracts.md`
  - `engineering/design/003-nonfunctional.md`
- [x] Add one architecture diagram reference location and ownership note.
- [x] Add dataset taxonomy table location and ownership note.

#### Tier 3 — Optional Portfolio Enhancements
- [x] Expand `Insights.md` with grounded findings linked to reproducible query evidence.
- [x] Add AI retrospective and onboarding validation notes.
- [x] Add concise production-readiness notes (retries, backfill, data-quality checks, observability).

### Notes
- Archive-first policy applies: replaced or heavily rewritten docs are moved to `archive/`.
- This pass completed documentation convergence scaffolding:
  - `archive/docs_legacy/README.md`
  - `archive/docs_legacy/REPO_MAP.md`
  - `archive/docs_legacy/architecture/ARCHITECTURE_DIAGRAM.md`
  - `archive/docs_legacy/reference/DATASET_TAXONOMY.md`
  - `archive/phase_notes/README.md`
  - `archive/phase_notes/phase15/README.md`
  - `archive/docs_legacy/ai-dev/documentation_convergence_audit.md`
  - `archive/docs_legacy/ai-dev/phase15_ai_retrospective.md`
  - `archive/docs_legacy/ai-dev/onboarding_validation_2026-02-23.md`
  - `archive/docs_legacy/operations/PRODUCTION_READINESS_NOTES.md`
  - `Insights.md` (query-backed findings)
- Canonical authority order remains:
  1. `engineering/project-context.md`
  2. `engineering/roles.md`
  3. `engineering/progress.md`
  4. `engineering/decisions.md`
  5. `engineering/ai_usage.md`

---

## Phase 16 — NL Explanation Final Refinement ✅
Goal: finalize NL capabilities as deterministic, discoverable, reviewer-friendly functionality without relaxing backend authority or safety boundaries.

Definition of Done:
- [x] Replace hardcoded question-type routing with a capability registry model.
- [x] Add composable metric-type support with backend-side validation and computation.
- [x] Add capability discovery endpoint (`GET /api/v1/capabilities`) for supported intents, filters, and examples.
- [x] Improve refusal UX to return clear refusal category plus guided supported alternatives.
- [x] Keep LLM boundary unchanged: interpretation/narrative only; no SQL or computation authority.
- [x] Extend deterministic test coverage for capability validation, metric computation, and refusal guidance.

Work Items:
- [x] Generalize hydro special intent to `fuel_generation_trend` (requires `fuelType`).
- [x] Generalize excellent-sites special intent to `water_quality_state_sites_trend` (requires `stateCategory`).
- [x] Add LAWA state-category mapping and regional sampling configuration.
- [x] Align parser schema + request validation with capability registry and metric-type constraints.
- [x] Add canonical capabilities endpoint contract fields for UI-guided discovery.
- [x] Refine Ask/Results/Landing/MBIE/LAWA UI copy and interaction flow to remove debug/demo tone and add guided alternatives.

Target NL capability outcomes:
- [x] MBIE window/ranking/share queries are answered deterministically (or refused deterministically when unsupported).
- [x] LAWA improvement/trend comparison queries are answered deterministically (or refused deterministically when unsupported).
- [x] Existing explanation determinism and provenance guarantees remain intact after refinement.

Explicit non-goals:
- [x] No arbitrary SQL generation.
- [x] No LLM-driven computation or planner execution.
- [x] No forecasting/predictive analytics expansion in this phase.

Notes:
- Phase 16 implementation included UX refinement work in the same stream because capability discoverability and refusal guidance were tightly coupled.
- Non-goals were preserved: no arbitrary SQL generation, no LLM-driven computation/planner execution, no forecasting expansion.


---

## Phase 17 — Capability Declaration, NL Determinism, Quality and Governance (in progress)

### Phase goal
Formalize supported capabilities as a declared, testable contract and eliminate NL parse drift that can produce inconsistent outcomes for the same prompt.

### Definition of Done
- [x] Parser normalization removes non-actionable categorical placeholders (e.g., `unknown`) before validation.
- [x] NL determinism checks exist for a fixed prompt corpus and fail on outcome/refusal-category drift.
- [x] `/api/v1/capabilities` contract stability tests include capability schema, suggested token values, and examples.
- [x] Capability declaration remains registry-authoritative; internal structuring additions do not create a second source of truth.
- [x] Existing API wire contracts remain backward compatible.

### Work items
- [x] Normalize `metricType=unknown` to absent in parsed requests before validation.
- [x] Add parser-service test coverage for unknown-metric normalization behavior.
- [x] Restore capability-driven Ask UI labels/prompts and remove hardcoded selective defaults.
- [x] Add `suggestedValuesByToken` to capabilities payload for deterministic, non-biased prompt template substitution.
- [x] Harden integration tests to self-seed required dataset_source records when baseline seed rows are absent in test DB lifecycle.
- [x] Enums over strings for capability vocabulary (User addition)
    - [x] Introduce internal enums for QuestionType/DatasetSource/MetricType/FilterKey to reduce stringly-typed drift.
    - [x] Preserve wire strings at API boundary; registry remains the authoritative support matrix.
- [x] Add NL determinism gate in CI
    - [x] Run a fixed prompt corpus multiple times and fail on parse/validation outcome or refusal-category drift.
- [x] Normalize parser output contract fully
    - [x] Normalize `metricType=unknown` to absent in parsed requests before validation.
    - [x] Add parser-service test coverage for unknown-metric normalization behavior.
    - [x] Extend normalization rules for all nullable categorical filters to prevent similar flake classes.
- [x] Formal API deprecation policy
    - [x] Document canonical endpoints and legacy aliases explicitly.
    - [x] Add parity tests for aliases and define sunset behavior.
- [x] Add architecture fitness tests
    - [x] Enforce controller→service boundaries (no controllers calling repositories).
    - [x] Enforce “no entity responses” (DTO-only at API boundary).
    - [x] Enforce explanation boundary constraints (provider/explanation logic only consumes FactPack + guardrails).
- [x] Add observability metrics by decision stage
    - [x] Emit counters/timers for parse → selection → validation → explanation → citation validation.
    - [x] Track refusal codes as tagged metrics.
- [x] Add branch protection receipts to repo docs
    - [x] In docs (e.g., docs/04 or docs/08), capture required checks, CODEOWNERS review policy, and merge gates so governance is auditable.
- [x] Add performance budget checks
    - [x] Track and report p95 latency for critical endpoints (at least `/ask`) on seeded data in CI.
    - [x] Start as non-blocking trend reporting; consider gating once stable.
- [x] Add backward-compat contract tests for capabilities payload
    - [x] Pin required JSON fields and semantics for `/api/v1/capabilities` including `suggestedValuesByToken`, examples, and capability schema.
- [x] Add fixed-corpus NL determinism suite (multiple executions per prompt) to CI.
- [x] Add explicit capability contract stability assertions for long-term frontend compatibility.

### Non-goals (explicit)
- [ ] No new datasets or explanation types in this phase.
- [ ] No forecasting/derived-analytics expansion.
- [ ] No replacement of capability registry authority with enum-only routing.

### Notes
Phase 17 improves determinism and declaration quality without expanding product scope.
The capability registry remains the authoritative support matrix; internal structuring aids (like enums) must not create a competing source of truth.
---
