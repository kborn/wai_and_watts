# Wai & Watts — Engineering Decisions Log

Append-only record of why irreversible choices were made.

---

### Phase 11 Fact Pack Contract
Date: 2025-02-07

Decision: Fact Pack is the only interface to LLM providers; no direct database access or hallucination risks.

Rationale:
- Fact Pack enforces deterministic data boundaries
- LLM cannot hallucinate since only receives structured facts
- Citations are tied to persisted database rows
- Prevents ungrounded responses and data leakage

Implications:
- LLM providers receive only validated Fact Pack objects
- No direct repository access from explanation providers
- Citation validation occurs before returning responses
- Refusal behavior enforced at service layer
- All data must pass through Fact Pack serialization and provenance tracking

---

## Decision Entry Format (Required)

### [Title]
Date: YYYY-MM-DD

Decision: one sentence describing the decision

Rationale:
- why

Implications:
- what this enforces or forbids

---

## Core Principles (Stable)

- Clear sequencing over speculative completeness
- Published interpretations, not raw measurements
- Lineage separate from domain data
- Complexity deferred until earned
- AI accelerates execution, not judgment

---

## Foundational Decisions

### Dataset Lineage Is a First-Class Concept
Date: 2026-01-19

Decision: Model dataset lineage separately from domain data.

Rationale:
- Provenance and idempotency apply across datasets

Implications:
- Domain data always references a release

---

### Ingestion Lifecycle Before Parsing
Date: 2026-01-19

Decision: Build lifecycle plumbing before parsers.

Rationale:
- Lifecycle errors dominate ingestion failures

Implications:
- Parsing deferred

---

### Read-Only APIs First
Date: 2026-01-19

Decision: Expose read-only APIs before introducing mutations.

Rationale:
- Validate models and wiring with minimal risk and surface integration issues early

Implications:
- Write paths are added only after read endpoints stabilize

---

## Operational & Enforcement Decisions

### Database Vendor Fidelity in Tests
Date: 2026-01-19

Decision: Default to H2 for unit/slice tests; add Testcontainers Postgres when dialect fidelity matters.

Rationale:
- Fast feedback loop with a path to realism when needed

Implications:
- Some bugs only surface in Postgres; add targeted Testcontainers tests as features warrant

---

### H2 Scope Is Test-Only
Date: 2026-01-19

Decision: Do not include H2 on the runtime classpath; use Postgres at runtime.

Rationale:
- Avoid accidental use of H2 in the running app; preserve production fidelity

Implications:
- App requires Postgres to boot; tests remain fast on H2

---

### API Versioning Strategy
Date: 2026-01-19

Decision: Public endpoints live under `/api/v1/...`; internal/dev endpoints under `/api/v1/internal/...` with profile guards.

Rationale:
- Avoids future breaking changes and keeps internal routes clearly separate

Implications:
- Version bumps require explicit decisions and deprecation plans

---

### Internal Ingestion Trigger Is Dev-Only
Date: 2026-01-19

Decision: `POST /api/v1/internal/ingest` is restricted to the `dev` profile and token-guarded; it is not a public API.

Rationale:
- Prevent accidental exposure; keep public API surface clean while plumbing is validated

Implications:
- Endpoint stability is not guaranteed; may be removed or changed later

---

### Service Layering Is Mandatory for APIs
Date: 2026-01-19

Decision: Controllers depend on services, not repositories; DTOs are separate from entities.

Rationale:
- Separation of concerns and testability; enables validation/auth without controller churn

Implications:
- New endpoints require DTOs and service methods; repository access in controllers is disallowed

---

### Schema Evolution Discipline (Flyway)
Date: 2026-01-19

Decision: Never edit applied migrations; always add forward migrations (Vn+1) and update JPA mappings accordingly.

Rationale:
- Preserves environment compatibility and history

Implications:
- Even small tweaks (NULL/NOT NULL, indexes) require a new migration

---

### Idempotency Source of Truth
Date: 2026-01-19

Decision: Database uniqueness on `(dataset_source_id, content_hash)` is authoritative; application treats duplicates as successful no-ops.

Rationale:
- Concurrency safety and retry-friendliness

Implications:
- Services should return the existing release on conflicts and log deduplication

---

### Frontend Deferred
Date: 2026-01-19

Decision: Build the frontend only after backend contracts and lifecycle stabilize.

Rationale:
- Avoid early coupling and rework; keep focus on stable backend foundations

Implications:
- Backend milestones gate frontend work and APIs are the primary contract surface

---

### LLM Usage Is Grounded and Non-Autonomous
Date: 2026-01-19

Decision: Use LLMs for grounded explanation and development assistance only; no autonomous code commits.

Rationale:
- Prevent hallucinations and preserve human ownership of architecture and commits

Implications:
- AI output is reviewed by a human; architectural changes require explicit human approval

---

### Interface-First Service Contracts
Date: 2026-01-19

Decision: Public service types exposed to controllers are interfaces; concrete classes provide implementations.

Rationale:
- Improves testability (mock interfaces in `@WebMvcTest` without bytecode tricks)
- Encourages loose coupling and clearer boundaries
- Simplifies refactoring and alternative implementations

Implications:
- Controllers depend on interfaces
- Implementations use a conventional name (e.g., `*ServiceImpl` or `Default*Service`)
- Avoid mocking concrete classes in slice tests; prefer interface mocks

---

### Stable Source Code Identifier
Date: 2026-01-19

Decision: Introduce `dataset_source.code` (unique) as the stable identifier for source lookups; URLs remain metadata.

Rationale:
- Decouples identity from transport/location; avoids URL churn breaking lookups

Implications:
- Services use `findByCode(...)` for lookups
- Migrations add `code` column with a unique index

---

### CI Guardrails via GitHub Actions
Date: 2026-01-19

Decision: Build and test the backend on every `push` and `pull_request` using GitHub Actions.

Rationale:
- Ensure broken builds/tests are caught automatically

Implications:
- Workflow `.github/workflows/ci.yml` runs `mvn -f backend -B clean verify` on JDK 21 with Maven cache
- Tests run with current H2 configuration; no Postgres/Testcontainers in CI for this phase
- Branch protections may require this check before merging (when enabled)

---

### Local Pre-Push Guardrail (Opt-in)
Date: 2026-01-19

Decision: Provide a sample Git pre-push hook that runs the backend build/tests locally; document usage in README.

Rationale:
- Lightweight, repo-local safety net without external tooling

Implications:
- Script at `scripts/git-hooks/pre-push.sample`; developers can opt-in by copying to `.git/hooks/pre-push`
- Central enforcement remains CI; hook is recommended but not mandatory

---

### Published Date Nullable for Releases
Date: 2026-01-19

Decision: `dataset_release.published_date` is nullable.

Rationale:
- Some sources may lack a reliable publication date

Implications:
- Parsers and APIs must tolerate missing `published_date`

---

### Created-At Audit Columns
Date: 2026-01-19

Decision: Add `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP` to `dataset_source` and `dataset_release`.

Rationale:
- Lightweight audit/debug signal

Implications:
- Entities expose read-only `createdAt` for observability


### Phase 6 MBIE Dataset Selection and Period Modeling
Date: 2026-01-26

#### Phase 6 Source Dataset Selection

Decision:
Use MBIE Electricity Quarterly data tables workbook
https://www.mbie.govt.nz/assets/Data-Files/Energy/nz-energy-quarterly-and-energy-in-nz/electricity-sept-2025-q3.xlsx
and specifically Sheet “6 - Fuel type (GWh)” as the Phase 6 ingestion source.

Rationale:
This workbook contains operational electricity system statistics (generation delivered to the grid).
Sheet “6 - Fuel type (GWh)” provides a longitudinal, structurally stable annual time series of net electricity generation by fuel type (1974 → present), making it ideal for initial domain ingestion. The alternative dataset (energy-overview.xlsx) represents national primary energy accounting (Energy Balance framework) and measures primary energy supply before conversion losses. Its electricity figures are not directly comparable to grid electricity generation and mix electricity with transport fuels, industrial heat, and transformation losses. It was rejected for Phase 6 to avoid semantic ambiguity and domain coupling.

Explicit selection criteria for Sheet 6:
- Single-domain dataset (electricity generation only)
- Stable schema across decades
- Policy-relevant analytical structure (used in Energy in New Zealand reports)
- Avoids mixed dashboard metrics (consumption, ICP counts, derived KPIs) present in quarterly reporting tables
- Minimizes ingestion and modeling churn for Phase 6

Implications: Annual-only modeling in Phase 6; quarterly modeled later via forward migration.

#### Phase 6 Period Modeling Scope

Decision:
Use annual-only period modeling (period_year INT) for Phase 6 fuel generation ingestion. Do not introduce polymorphic period fields (year/quarter/month).

Rationale:
Sheet “6 - Fuel type (GWh)” is published annually by calendar year. Introducing generalized temporal modeling at this stage would be speculative complexity. Phase 6’s objective is to validate ingestion lifecycle, lineage, and persistence, not temporal abstraction.

Implications:
- Phase 6 domain tables store annual records only.
- Quarterly datasets (e.g., Sheet 1) will be introduced in Phase 7+ via forward migrations or separate tables.
- Unified temporal modeling requires an explicit future decision and migration plan.

#### Phase 6 Ingestion Contract Specification (Fixture and Production)

This contract defines the Phase 6 ingestion inputs, domain schema, and fixture format. It is append-only and governs both fixtures and production ingestion for Phase 6.

Source Sheet
- Workbook: `electricity-sept-2025-q3.xlsx`
- Sheet: `6 - Fuel type (GWh)`
- Unit: GWh (net electricity generation)

Hashing & Lineage
- Content hash: SHA-256 of raw fixture bytes (used for idempotency)
- Each ingested row links to `dataset_release_id` from lineage

Fixture Location (Phase 6)
- `backend/src/test/resources/fixtures/mbie/generation/`

Domain Table: `mbie_generation_record`

| Column               | Type           | Description                                                                 |
|----------------------|----------------|-----------------------------------------------------------------------------|
| `id`                 | UUID or BIGINT | Primary key (implementation choice)                                         |
| `dataset_release_id` | FK             | Link to dataset lineage                                                     |
| `period_year`        | INT            | Calendar year (e.g., 2024)                                                  |
| `fuel_type_raw`      | TEXT           | Raw label from sheet (e.g., "Geothermal", "Natural gas")                   |
| `fuel_type_norm`     | TEXT           | Normalized fuel token (e.g., HYDRO, WIND, SOLAR, GAS, COAL, GEOTHERMAL, OTHER) |
| `generation_gwh`     | DECIMAL        | Net generation in GWh                                                       |

Notes on normalization (Phase 6)
- Parser produces both `fuel_type_raw` and `fuel_type_norm`.
- Normalization: trim, uppercase, collapse whitespace, standardize punctuation.
- Enum is deferred until vocabulary stabilizes; TEXT is used in Phase 6.

Phase 6 Fixture CSV Format
- File(s) under `fixtures/mbie/generation/`
- Header and sample rows:

```
period_year,fuel_type_raw,fuel_type_norm,generation_gwh
2022,Hydro,HYDRO,26071
2022,Geothermal,GEOTHERMAL,7984
2022,Wind,WIND,2835
2022,Solar,SOLAR,283
2022,Natural gas,GAS,672
2022,Coal,COAL,3527
```

Read API (non-binding sketch for Phase 6)
- Endpoint: `GET /api/v1/mbie/generation`
- DTO: `MbieGenerationRecordDto { period (YEAR for Phase 6), fuelType, fuelTypeRaw, generationMwh }`

Out of Scope (Phase 6)
- Quarterly ingestion
- Consumption modeling
- Capacity modeling
- Primary energy accounting (PJ)
- Live HTTP fetch (fixtures only)


#### Phase 6 Non-Goals
- No quarterly ingestion
- No consumption modeling
- No capacity modeling
- No primary energy accounting (PJ)
- No live HTTP fetch (fixtures only)

### Contract Location

Date: 2026-01-26

Decision: Store dataset contracts in `engineering/design/`, rationale in `engineering/specs/`.

Rationale: Keep decisions.md readable and stable.

Implications: Phase 6 contract lives in `engineering/design/004-mbie-annual-schema.md`.


## Dataset Source Taxonomy Convention

Date: 2026-01-27

Decision: Use `<publisher>.<domain>.<variant>` naming for `dataset_source.code` (e.g., `mbie.generation.annual`).

Rationale:
- Stable identifiers across lineage, APIs, docs, and future LLM fact packs.

Implications:
- All new datasets must follow this convention; existing sources are migrated forward via Flyway when renamed.
- Applied in Phase 6: canonical code set to `mbie.generation.annual` via Flyway V8.


## Phase 6 Dataset Source Code Backport

Date: 2026-01-27

Decision: Phase 6 `dataset_source.code` is `mbie.generation.annual`.

Rationale:
- Align Phase 6 with taxonomy before Phase 7 introduces `mbie.generation.quarterly`.

Implications:
- Flyway migration updates existing row; APIs/specs use the canonical code.


## Phase 7 Dataset Selection

Date: 2026-01-27

Decision: Phase 7 will ingest MBIE quarterly electricity generation data.

Rationale: Demonstrates schema evolution and harder parsing within the same domain before cross-domain ingestion.

Implications: Annual and quarterly tables coexist; future migrations unify temporal modeling.

## Phase 7 Extensibility Definition

Date: 2026-01-27

Decision: Extensibility is proven when a second dataset reuses lifecycle/orchestration unchanged and only dataset-specific parsers and schemas are added.

Rationale: Avoids framework overengineering while demonstrating scalable architecture.

Implications: Shared ingestion code is stable; dataset logic is pluggable.

## Phase 7 Ingestion Abstraction Boundary

Date: 2026-01-27

Decision: Use light abstraction (shared orchestrator + dataset-specific ingesters).

Rationale: Portfolio clarity and avoidance of premature ingestion frameworks.

Implications: Metadata-driven pipelines deferred to Phase 9+.

## Phase 7 Schema Evolution Strategy

Date: 2026-01-27

Decision: Use forward migrations and new tables for quarterly data.

Rationale: Demonstrates Flyway discipline and avoids polymorphic complexity.

Implications: Phase 8+ may unify via new schema or views.


## Documentation as Architecture Contract

Date: 2026-01-27

Decision: Specs, design contracts, and decisions files are authoritative over code structure during development.

Rationale: Ensures AI and humans converge on intended architecture rather than reverse-engineering from implementation.

Implications: New phases require spec + design + decision entries before coding.

## Fuel Type Normalization — Biogas Handling

Date: 2026-01-27

Decision: Normalize Biogas to OTHER for both annual and quarterly MBIE datasets.

Rationale: Maintain cross-dataset consistency and avoid premature fuel taxonomy expansion. Biogas is policy-heterogeneous and small in volume; dedicated categorization deferred.

Implications: Biogas is grouped with OTHER until a BIOGAS/BIOENERGY category is introduced across all datasets via forward migration.


## AI Onboarding Runbook and Validation Checklist

Date: 2026-01-28

Decision: Introduce an explicit AI onboarding runbook and onboarding validation checklist as first-class repository documentation to govern how AI agents are onboarded and evaluated before performing engineering tasks.

Rationale: Wai & Watts intentionally treats AI agents as engineering collaborators. As the project grows, implicit context transfer becomes unreliable across sessions and tools. A procedural onboarding runbook and validation checklist formalize the onboarding process, reduce context drift, and treat documentation as an executable system artifact. This mirrors human developer onboarding practices in production platform teams and strengthens the portfolio narrative around AI-assisted engineering discipline.

Implications:
- New AI sessions should be onboarded using the runbook file list and validation questions.
- Onboarding failures are treated as documentation defects and must be fixed in project-context.md, progress.md, or decisions.md.
- The checklist is a human-operated QA tool and is not provided to AI agents as a prompt.
- This process is documentation-only and does not affect runtime system behavior.

### Dataset Source Taxonomy v2 (Optional Dataset Segment)

Date: 2026-01-28

Decision: Support an optional 4-segment dataset source taxonomy: <publisher>.<domain>.<dataset>.<variant>,  while preserving existing 3-segment codes.

Rationale: LAWA datasets contain multiple dataset families within a single domain (e.g., state_trend, site_measurements). Forcing all into the variant field  would overload semantics and reduce discoverability. Introducing an optional  dataset segment enables structured grouping without breaking existing MBIE identifiers or requiring forward migrations.

Implications:
- Existing dataset_source.code values remain immutable.
- New datasets may use 3- or 4-segment taxonomy.
- Parsers must explicitly support 3- and 4-segment forms only.
- More than 4 segments requires a Staff architectural decision.
- Variant segment continues to encode temporal grain only.

### Phase 8 — LAWA State (Multi‑Year): Dataset Selection & Modeling Boundaries
Date: 2026-01-28

Decision: Adopt LAWA river water quality “State” (multi‑year) as the third dataset; model published 5‑year hydrological windows, not raw telemetry.

Rationale:
- Cross‑domain proof beyond MBIE electricity data while reusing the ingestion lifecycle
- Published interpretations are stable and interview‑friendly; avoid sensor‑level time series complexity in Phase 8
- LAWA export provides attribute bands (A..E) and indicator metrics suitable for deterministic normalization
- Structurally stable, policy-relevant, and cross-domain proof without time-series/geospatial complexity.
- Multi-year aggregation semantics align with a clear period_start_year/period_end_year model.

Implications:
- Dataset source code (taxonomy v2: <publisher>.<domain>.<dataset>.<variant>): `lawa.water_quality.state.multi_year`
- Normalization:
  - `indicator_norm` maps per `engineering/design/007-lawa-state-multi-year-schema.md`; unknown → `OTHER`
  - `state_norm` derived from `attribute_band` (A..E → EXCELLENT..VERY_POOR); unknown → `UNKNOWN`
- Parser must strictly validate header (BOM‑safe); preserve raw fields alongside normalized
- Ingestion remains idempotent via `(dataset_source_id, content_hash)`; duplicates are no‑ops
- Read API is read‑only; controllers return DTOs (no entities)

Notes:
- Fixture‑first only in Phase 8; live download deferred to Phase 10
- Paths must follow variant‑aware convention consistently across docs and code:
  - Fixtures: `backend/src/test/resources/fixtures/lawa/water_quality/state/multi_year/...`
  - Public API: `/api/v1/lawa/water-quality/state/multiyear`
  - Dataset source code: `lawa.water_quality.state.multi_year`
***

### Phase 9 — LAWA Trend (Multi‑Year): Dataset Selection & Period Semantics
Date: 2026-02-03

Decision: Ingest LAWA river water quality Trend results using dataset source code `lawa.water_quality.trend.multi_year`, modeling trend windows as hydrological multi‑year windows anchored to the latest State dataset year.

Rationale:
- Completes the environmental storytelling pair: State (current condition) + Trend (direction of change)
- LAWA Trend dataset publishes interpreted classifications and scores rather than raw measurements, aligning with Wai & Watts principle of ingesting published interpretations
- Trend sheet provides window length but not explicit end year, requiring deterministic period derivation for stable querying and joins with State data
- Reuses lifecycle, lineage, parser, and schema evolution patterns proven in Phases 6–8
- Maintains taxonomy v2 structure without introducing additional segments or semantic overload

Implications:
- Dataset source code is fixed to: `lawa.water_quality.trend.multi_year`
- Source workbook and sheet are fixed for Phase 9:
  - Workbook: `lawa-river-water-quality-state-and-trend-results_30oct2025.xlsx`
  - Sheet: `Trend`
- Period modeling contract is fixed:
  - `as_of_year = MAX(hYear)` from `State Attribute Band` sheet in same workbook
  - `period_type = HYDRO_NYR_WINDOW`
  - `period_end_year = as_of_year`
  - `period_start_year = period_end_year - trend_period_years`
- Trend normalization contract is fixed to enum:
  - IMPROVING
  - DEGRADING
  - NO_CHANGE (reserved; only used if explicitly published by LAWA)
  - INSUFFICIENT_DATA (covers Not determined / Indeterminate / non-directional classifications)
- Trend ingestion stores published trend classifications and scores only; no trend computation is performed in Wai & Watts
- Phase 9 fixtures must be deterministic slices aligned to Phase 8 site/region selection to enable stable integration tests


### Phase 9 — Trend Cross-Sheet Period Derivation Contract
Date: 2026-02-03

Decision:
Trend period derivation depends on State sheet hYear values from the same workbook artifact used to generate fixtures.

Rationale:
Trend sheet does not contain explicit period end year. Using MAX(hYear) from State sheet provides deterministic, reproducible period derivation while avoiding cross-dataset coupling or runtime joins.

Implications:
- Trend ingestion must not query State domain tables.
- Trend ingestion derives period using fixture/workbook context only.
- Trend fixture generation must ensure State + Trend slices originate from same workbook version.


### Phase 9 — Trend Normalization Vocabulary Is Append-Only
Date: 2026-02-03

Decision:
Trend normalization vocabulary is append-only and stored as TEXT in Phase 9.

Rationale:
LAWA classification vocabulary may evolve. Early enum enforcement would create unnecessary migrations and ingestion failures.

Implications:
Known normalized values:
- IMPROVING
- DEGRADING
- NO_CHANGE (reserved; only if explicitly published by LAWA)
- INSUFFICIENT_DATA

Unknown future values map to OTHER or UNKNOWN until a formal expansion decision is recorded.

### Phase 10 — Live Ingestion Is Manual by Design
Date: 2026-02-04

Decision:
Live dataset ingestion is a manual, operator-triggered process. Wai & Watts does not implement scheduling, automated polling, or orchestration infrastructure.

Rationale:
- Phase 10 demonstrates real-world ingestion realism without introducing data platform complexity.
- Scheduling, polling, and orchestration are infrastructure concerns and not required to demonstrate lifecycle correctness, lineage discipline, or ingestion idempotency.
- Manual execution keeps operational surface area small and preserves portfolio clarity.

Implications:
- No Airflow, cron, or event-driven ingestion infrastructure will be added.
- No automated publisher polling will be implemented.
- Ingestion is initiated via CLI, script, or dev-only internal endpoint.
- Future automation requires a new explicit architectural decision.

### Fixture-First Development Remains Mandatory After Live Ingestion
Date: 2026-02-04

Decision:
All new datasets must be introduced using fixture-first development before enabling live ingestion.

Rationale:
- Preserves deterministic testing and reproducible development.
- Prevents coupling parser development to external publisher availability.
- Maintains portfolio narrative of disciplined ingestion engineering.

Implications:
- New dataset phases must include fixture generation before live ingestion is implemented.
- Live ingestion is considered an operational extension, not a development dependency.

### Phase 10 — Local File Ingestion Foundation
Date: 2026-02-04

Decision:
Extend all ingestion classes with file ingestion capabilities while maintaining existing fixture functionality and lifecycle discipline.

Rationale:
Live ingestion must be an operational extension of the existing ingestion pipeline, not a new architectural layer. By adding `ingestFile()` methods to all ingestion classes (MBIE Annual, MBIE Quarterly, LAWA State, LAWA Trend), operators can ingest real publisher files using the same provenance tracking, content hashing, and domain persistence patterns as fixture ingestion.

Implications:
- All dataset ingestion classes now support both fixture and file ingestion
- FileIngestionUtil provides centralized file validation and hashing
- No changes to lifecycle, parsing, or domain persistence logic
- Comprehensive test coverage ensures file ingestion works reliably across all datasets
- Maintains manual, local-filesystem-only approach per Phase 10 constraints

### Phase 10 — Manual Download Helper Scripts
Date: 2026-02-05

Decision:
Create manual download helper scripts that enable operators to fetch real publisher workbooks without introducing automation or discovery mechanisms.

Rationale:
Phase 10 requires manual-only, reproducible ingestion workflows. Download scripts provide simple, explicit tools for operators to fetch source files while avoiding complexity of web scraping, API integration, or background polling. Scripts use fixed URLs by default but accept custom URLs as needed.

Implications:
- Scripts follow consistent pattern: help text, validation, directory creation, download verification
- MBIE and LAWA scripts save to timestamped directories: ./downloads/<publisher>/<YYYY-MM-DD>/
- Comprehensive error handling and dependency checking
- No web scraping or automated discovery (avoids complexity and brittleness)
- Provides clear next-step guidance for ingestion commands
- Both scripts are executable and self-documenting via --help flag

### Phase 10 — Live Ingestion Input Boundary (Local Filesystem Only)
Date: 2026-02-04

Decision:
Phase 10 live ingestion reads source artifacts from the local filesystem only (operator-provided file paths). No remote storage adapters (S3/GCS) or raw-zone architecture is introduced.

Rationale:
- Demonstrates real ingestion realism while keeping operational surface area minimal.
- Avoids introducing storage/platform complexity unrelated to core lifecycle + parsing correctness.
- Keeps Phase 10 manual and reproducible.

Implications:
- Operator provides a local path to the downloaded workbook/CSV.
- The system does not implement remote fetchers, storage clients, or background jobs.
- Any future support for remote storage requires a new explicit architectural decision.

### Introduce Dataset-Specific Transform Step Before Ingestion

Date: 2026-02-05

Decision:
Introduce a dataset-specific transform step that converts raw publisher artifacts (primarily XLSX) into canonical contract CSV files prior to ingestion. The ingestion pipeline remains contract-CSV-only.

Rationale:
- Real publisher exports are not contract-shaped and contain layout artifacts (titles, notes, multi-table sheets, header drift).
- Preserves the contract-first ingestion architecture without expanding parser scope or introducing dual-schema parsing.
- Ensures live ingestion remains reproducible, testable, and deterministic.
- Eliminates manual spreadsheet editing from the operator workflow.
- Keeps ingestion lifecycle, lineage tracking, and hashing strategy unchanged.

Implications:
- New pipeline boundary is:
  Raw Publisher Artifact (XLSX)
  → Dataset-Specific Transformer
  → Contract CSV
  → Existing Ingestion Pipeline

- Transform is a pure function of artifact → contract CSV. Network IO is always external to transform.
- Transformers are dataset-specific implementations (not metadata-driven or generic framework).
- Transformers must output contract CSV files matching fixture schema exactly (headers, order, types).
- Parsers remain contract-only and do not accept raw publisher formats.
- Dataset release hashing continues to operate on contract CSV bytes.
- Transform step is implemented as an explicit CLI/script step; optional wrapper commands may orchestrate transform + ingest but must not introduce new ingestion logic.
- Raw CSV export may be supported as a secondary input mode but is not considered canonical input.


## Operator Execution Model

Date: 2026-02-05

Decision:
Operator ingestion is CLI-driven and must not require the backend service runtime.

The backend service runtime is required for post-ingestion validation workflows, including API verification and end-to-end platform checks.

Internal ingestion HTTP endpoints are retained for:
- Dev/test convenience
- Integration testing
- Local debugging

Internal endpoints are not part of the operator workflow and must not be documented as such.

Canonical Operator Flow:

download → transform → ingest (CLI) → start service → API validation



Rationale:

- This model provides:
  - Deterministic ingestion execution
  - Offline transform and ingestion capability
  - Reduced runtime coupling
  - Simplified CI execution
  - Clear separation of data plane and service runtime
  - Cleaner future transition to orchestrated ingestion in later phases

Non-Goals:

Phase 10 does not attempt to:

- Expose ingestion via public APIs
- Require service runtime for ingestion execution
- Provide scheduler or orchestration capabilities


Architectural Invariant:

Transform is a pure function of publisher artifact → contract CSV.

Network IO, artifact acquisition, scheduling, and service runtime must remain external to transform and ingestion execution.

Future Considerations:

Future phases may introduce:

- Orchestrated ingestion triggers
- Control-plane APIs
- Scheduled ingestion workflows

These must preserve CLI execution capability for deterministic and reproducible ingestion.

### Fact Pack Is The Exclusive LLM Data Boundary

Date: 2026-02-06

Decision:
Fact Packs are the mandatory and exclusive boundary between Wai & Watts domain data and any LLM interaction. All LLM-driven explanation generation must operate solely on Fact Pack inputs.

LLMs are prohibited from:
- Accessing database entities or queries
- Accessing raw publisher artifacts
- Inferring or fabricating missing factual data
- Using external knowledge for factual claims

Rationale:
- Prevent hallucination via architecture rather than prompt design
- Preserve deterministic explanation inputs and reproducibility
- Maintain dataset lineage and provenance traceability
- Ensure explanations remain testable and auditable
- Enable provider-agnostic LLM integration without leaking domain or persistence concerns

Implications:
- Fact Pack builders are required per dataset
- Explanation services must never embed DB queries or domain logic
- Derived metrics must be computed deterministically inside Fact Pack builders
- LLM outputs must cite Fact Pack fields or IDs
- Refusal behavior is considered correct system behavior
- New explanation capabilities must extend Fact Pack schema rather than bypassing it
- Fact Pack schema versioning must be maintained for compatibility and test stability

---

### Natural Language Intent Parsing Boundary

Date: 2026-02-07

### Context
To improve usability and discoverability of supported explanation capabilities, the system introduces a natural language query interface that maps user questions to structured explanation requests.

### Decision
Natural language LLM usage is restricted to **intent parsing only**.

The intent parser may:
- Convert natural language → structured ExplanationRequest
- Extract question type, dataset scope, and filters

The intent parser may NOT:
- Generate facts or metrics
- Access database or domain entities
- Perform reasoning about dataset values
- Bypass fact pack construction
- Bypass explanation validation or refusal logic

All parsed intents MUST be validated against supported question types and filter schemas before entering the explanation pipeline.

The structured explanation endpoint is the canonical system interface and source of truth for explanation execution. It exists to support deterministic testing, internal consumers, automation workflows, and operational fallback scenarios.

The natural language endpoint is a user convenience layer that translates unstructured questions into validated structured explanation requests.

Both endpoints are intentionally permanent to enforce separation of concerns between intent parsing and grounded explanation generation, and to maintain system debuggability and operational resilience.


### Rationale
- Preserves fact-pack safety architecture
- Prevents hallucinated or inferred facts
- Maintains deterministic explanation inputs
- Enables modern natural language UX without sacrificing system guarantees

### Implications
- Structured explanation endpoint remains permanent and supported
- Natural language endpoint is additive, not replacement
- Intent parsing failures must default to refusal, not guesswork
- Raw user question and parsed intent should be logged for auditability

### Future Considerations
- Confidence scoring may be introduced
- UI may optionally expose parsed intent for transparency
- Provider abstraction should remain intact for future LLM vendor flexibility


# Wai & Watts --- Engineering Decisions Log (Additions)

### Phase 13 Frontend Stack Selection

Date: 2026-02-09

Decision: Frontend stack is locked to: React, TypeScript, Vite, React
Router, TanStack Query, Tailwind, Vitest, Playwright

Rationale: Strong NZ job market alignment and fast iteration for demo
UI.

Implications: No SSR in Phase 13. Server state handled via TanStack
Query.

------------------------------------------------------------------------

### Phase 13 Testing Strategy

Date: 2026-02-09

Decision: Phase 13 includes Playwright smoke tests. Phase 14 expands
coverage.

Rationale: Ensures demo reliability and prevents "testing later"
ambiguity.

------------------------------------------------------------------------

### Intent Parsing Interface Seam

Date: 2026-02-11

Decision: Intent parsing now sits behind an interface with a
deterministic stub implementation. This enables future LLM-backed
parsing without changing public API contracts.

Rationale: Maintains deterministic Phase 12 / 13 behavior while enabling
safe LLM evolution later.

Implications: Future LLM implementation must preserve request schema and
refusal taxonomy.

------------------------------------------------------------------------

### Dynamic Filter Options via Backend Unique-Value Endpoints

Date: 2026-02-11

Decision: Frontend filter dropdowns are populated using backend
endpoints returning unique fuel types, regions, and indicators.

Rationale: Avoids duplication and drift between backend domain data and
frontend UI options.

Implications: These endpoints are part of the stable API surface and
should remain backward compatible.


# Decision --- MBIE Time-Series Visualization & Table Secondary Role

Date: 2026-02-12

------------------------------------------------------------------------

## Decision

For the MBIE Electricity Generation UI, the primary data exploration
surface will move from **bar charts + primary data table** to a
**time-series line chart with interactive time-range zoom**.

The data table will remain available but will be demoted to a
**secondary, user-toggled or collapsible audit surface** rather than the
primary data presentation layer.

------------------------------------------------------------------------

## Rationale

### Align Visualization With Dataset Shape

MBIE generation data is fundamentally time-series data. A timeline
visualization better supports: - Long-term trend comprehension -
Multi-fuel comparison - Seasonality and macro energy shifts - Natural
user mental models for electricity generation

Bar charts are useful for categorical comparisons but are less effective
for long time-range exploration.

------------------------------------------------------------------------

### Improve Product Credibility Without Expanding Domain Logic

A timeline chart with zoom interaction provides a recognizable,
production-grade data UX pattern (similar to cloud metrics dashboards
and data platforms) without introducing new backend computation or
client-side domain logic.

This improves portfolio signal while preserving Wai & Watts
architectural boundaries.

------------------------------------------------------------------------

### Preserve Deterministic Data Transparency

The data table remains available to: - Provide raw value inspection -
Support auditability and trust - Reinforce that charts are
presentational views over persisted data - Assist debugging and
verification workflows

The table is intentionally not removed.

------------------------------------------------------------------------

## Implementation Direction (High-Level, Non-Binding)

### Chart Type

- Time-series line chart

### Series Behavior

- One line per selected fuel type
- Optional total generation line

### Interaction Model

- Click-and-drag time range zoom (brush selection)
- Reset to "All Time" control
- Hover tooltips with period + value(s)

### Data Rules

- Charts must be derived only from backend API responses
- No client-side metric invention
- No forecasting, smoothing, or derived analytics
- No client aggregation beyond simple grouping for display

------------------------------------------------------------------------

## Table Role Change

The MBIE data table will: - Remain accessible on the page - Be
collapsible, toggleable, or placed in a secondary tab - Continue to
reflect the same filtered dataset used for chart rendering

The table remains the canonical UI representation of raw values.

------------------------------------------------------------------------

## Architectural Boundaries Reinforced

This decision does **not** change: - Backend ownership of domain
semantics - Fact Pack boundaries - API contract authority - Thin client
philosophy - Explanation pipeline behavior

------------------------------------------------------------------------

## Non-Goals

This decision does NOT introduce: - Dashboard framework architecture -
Generic chart engine abstraction - Client-side analytics or derived
metrics - Additional dataset visualization mandates

------------------------------------------------------------------------

## Implications

### For Frontend Architecture

- Chart interaction state must remain UI-only
- Backend APIs remain unchanged unless explicitly extended later

### For Portfolio Narrative

- Demonstrates real data-product UX patterns
- Shows ability to match visualization type to data semantics
- Improves demo storytelling without increasing system complexity

------------------------------------------------------------------------

## Future Considerations (Deferred)

- Multi-dataset unified timeline views
- Cross-dataset comparison charts
- Server-side aggregation APIs for visualization optimization
- Advanced visualization frameworks
- Exportable chart states / saved views


### Frontend Charting Standard + Display Aggregation Boundary (MBIE Timeline)
Date: 2026-02-12

Decision:
Adopt ECharts as the frontend charting library for interactive visualizations (starting with MBIE).
Allow frontend "display aggregates" only when they are computed solely from already-fetched API rows for visualization purposes and are clearly labeled as such.

Rationale:
- MBIE timeline requirements include multi-series time-series rendering, brush zoom, and reset interactions; ECharts provides these reliably without building a bespoke chart framework.
- Backend remains authoritative for domain truth; frontend remains a thin client.
- Some chart affordances (pivoting, series shaping, and summing displayed series) are necessary to render interactive visualizations and are not new domain metrics.

Implications:
- Frontend may reshape/pivot API rows into chart series and compute "Total (sum of displayed fuels)" as a visualization-only series.
- Frontend must NOT introduce new analytics (smoothing, forecasting, statistical transforms) or present display aggregates as authoritative domain facts.
- "Total" must be labeled explicitly as a display aggregate (e.g., "Total (sum of displayed fuels)"), not as a published MBIE metric.
- Chart zoom (brush window) is treated as part of the active view state and must filter the table to reflect the same dataset window shown in the chart.
- No backend API changes are required for this decision; any proposal to add backend totals or aggregation endpoints requires a new Staff decision entry.

---

### MBIE Chart Uses Normalized Totals With Raw Breakdown Tooltip
Date: 2026-02-13

Decision: Plot MBIE generation lines by normalized fuel type totals per period; show raw fuel type breakdown in the tooltip, while the table remains raw.

Rationale:
- Normalized categories are the intended legend semantics
- Raw fuel types can map to the same normalized bucket (e.g., OTHER), which otherwise creates duplicate series labels
- Tooltip breakdown preserves transparency without cluttering the chart

Implications:
- Chart aggregates records by (period, normalized fuel type)
- Tooltip renders per-series raw contributions
- Table continues to show raw fuel types without aggregation


### Decision --- Phase Expansion Numbering Convention

Date: 2026-02-13

Decision: Post-phase feature expansions will use lettered phase
extensions (e.g., 14A, 14B).

Rationale: - Preserves historical numeric step structure (e.g., 14.1,
14.2). - Allows major feature expansions without introducing new
numbered phases. - Maintains consistent `.n` step semantics.

Implications: - Numeric substeps remain implementation-level. - Lettered
phases represent product capability expansions.

# Decision --- LAWA Table Must Be Semantically Complete

Date: 2026-02-13

## Decision

LAWA browse tables must include sufficient fields to interpret the
environmental meaning of each row.

## Rationale

- LAWA results are published interpretations, not raw telemetry.
- Minimal tables (period, region, site) do not communicate
    environmental meaning.
- Table must support explanation verification and auditability.

## Implications

### Common Fields

- Period (human readable)
- Region
- Site
- Indicator (raw + normalized when available)

### State Dataset Fields

- Published state classification
- Normalized state label
- Numeric values (if present in DTO)
- Units (if present)

### Trend Dataset Fields

- Published trend classification
- Window length (if present)
- Numeric values (if present)
- Units (if present)

Frontend must never invent missing semantic fields.


# Decision --- LAWA Visualization Philosophy: Faithful Interpretation Display

Date: 2026-02-13

## Decision

LAWA visualizations prioritize faithful display of published
environmental interpretations rather than exploratory analytics or
distribution visualizations.

## Rationale

- LAWA datasets contain published interpreted environmental results
    (state bands, trend classifications).
- Distribution-style charts (counts by region, rankings, etc.) obscure
    environmental meaning.
- Visualization must support explanation workflows, not exploratory
    data science workflows.

## Implications

- Primary visualization is time-slice timeline of filtered dataset.
- Visualization represents dataset slice semantics, not population
    distribution.
- Frontend must not compute environmental meaning, classifications, or
    derived analytics.
- Table remains canonical semantic surface for interpreting row
    meaning.


# Decision --- LAWA Unified Visualization Model (Trend + State)

Date: 2026-02-14\
Supersedes: 2026-02-13 version

------------------------------------------------------------------------

## Decision

LAWA visualizations follow a **unified model aligned to dataset
semantics**.

### Trend

Primary visualization: **Classification Distribution (Site Count by
Trend Classification)**

TrendScore is treated as **ordinal classification**, not numeric
magnitude.

------------------------------------------------------------------------

### State

Primary visualization: **State Band Distribution (Site Count by
Attribute Band)**

State data represents **latest-condition snapshot assessments**, not
continuous measurement telemetry.

State must NOT be visualized as time series.

------------------------------------------------------------------------

## Trend Visualization Rules

### TrendScore Domain

Allowed values: - -2 - -1 - 0 - +1 - +2

Sentinel: - -99 → Insufficient Data → mapped to NULL for visualization

------------------------------------------------------------------------

### Trend Chart

Type: Classification Distribution Bar Chart

Fixed bucket order: 1. Very Likely Degrading\
2. Likely Degrading\
3. Indeterminate\
4. Likely Improving\
5. Very Likely Improving\
6. Insufficient Data (only if present)

------------------------------------------------------------------------

### Trend Forbidden Patterns

- Site ranking by TrendScore\
- Top-N site charts by TrendScore\
- Timeline visualization of trend windows

------------------------------------------------------------------------

## State Visualization Rules

### State Chart Type

State Band Distribution

X-axis: Attribute Band (best → worst ordering)

Y-axis: Site count

------------------------------------------------------------------------

### State Chart Gating

Chart renders only when: - Region selected\
- Indicator selected\
- Indicator must be single indicator (NOT "All Indicators")

------------------------------------------------------------------------

### State Interaction

Chart bucket click → filters State table by selected band.

------------------------------------------------------------------------

## State Data Semantics

State records represent: - Latest statistical assessment window - Not
measurement telemetry - Not continuous time series

Time-series visualization is explicitly disallowed.

------------------------------------------------------------------------

## Sentinel Handling

Sentinel values (-99): - Converted to NULL for visualization - Displayed
as "Insufficient Data" - Never rendered as raw numeric values

Insufficient Data may appear as its own distribution bucket.

------------------------------------------------------------------------

## Architectural Boundaries

Frontend MUST NOT: - Derive environmental metrics\
- Recompute classifications\
- Reconstruct historical measurement series\
- Perform statistical modeling or smoothing

Frontend MAY: - Normalize sentinel values\
- Group records for distribution visualization\
- Sort and paginate for usability


# Decision --- Phase 14C Regional Environmental Context Panel

Date: 2026-02-14


## Decision

Implement a Regional Environmental & Infrastructure Context Panel that
synthesizes water monitoring and energy system signals into a single
situational awareness surface without implying causal or statistical
relationships between datasets.


## Core Guardrail

The panel MUST include disclaimer text:

"These signals are presented for situational context only. No causal or
statistical relationship between datasets is implied."


## Panel Sections

Water Monitoring Confidence\
Water Direction Signal (Trend Distribution)\
Water Condition Signal (State Band Distribution)\
Energy System Context (National MBIE Summary)



# Decision — Regional Environmental Context View Independence

Date: 2026-02-14

## Decision

The Regional Environmental Context card is independent of LAWA View Type (Trend vs State) and must be computed from the active filter slice only.

Context values are derived from:
* Region (required)
* Indicator scope (selected indicator or All Indicators)

Context values must not change when users switch between Trend and State visualization modes unless the underlying filter slice changes.

Trend-derived metrics and State-derived metrics are computed independently from their respective datasets and merged into a single context surface for display.

## Rationale
### Preserve Signal vs Interpretation Separation

The Context card is an interpretation support surface, not a visualization-mode-dependent data surface.
Tying Context to View Type would incorrectly imply that Context represents the chart rather than the environmental situation for the selected slice.

### Prevent Silent Data Drift

If Context were recomputed based on View Type, switching between Trend and State
could cause values to change without user-visible filter changes, creating confusion and reducing trust.

### Maintain Product Mental Model

The product architecture intentionally separates:
* Signal surfaces (Trend chart, State chart)
* Interpretation surface (Context)
* Evidence surface (Table)

Keeping Context independent preserves this structure.

## Implications
### Frontend
* Context must not re-query or recompute based on View Type changes.
* Context recomputation is triggered only by Region or Indicator filter changes.

### Backend / Data Logic
* Context endpoints must ignore View Type.
* Trend and State context metrics must be computed independently and returned together.

### UX / Product Behavior
* Users may switch between Trend and State views without Context values changing.
* Context reflects environmental conditions for the selected slice, not the active chart.

### Phase 15 — Citation Family Matching (Validation Softening)
Date: 2026-02-17

Decision:
Citation validation will treat certain required citations as **evidence families** via prefix matching (e.g., `metric:lawa:excellent_sites_percentage:*`), rather than requiring exact ID equality.

Rationale:
- Exact citation matching causes false INTERNAL_ERROR outcomes when the model cites an equivalent concrete fact within the same family.
- Phase 15 prioritizes safe usefulness and determinism over overly strict coupling to a single citation ID spelling.

Implications:
- Validation passes if each required citation is satisfied by:
  - exact match, or
  - family-prefix match (wildcard `:*`)
- Tests must cover both match and non-match cases.
- This change must not weaken the “no uncited claims” rule; it only prevents false rejects for equivalent evidence.

---

### Phase 15 — Deterministic Required Citation Selection
Date: 2026-02-17

Decision:
Fact Pack builders must produce deterministic required-citation sets and ordering, independent of collection iteration order.

Rationale:
- Non-deterministic required citations create random pass/fail across identical runs, undermining Phase 15 repeatability guarantees.

Implications:
- Required citations are deduped and sorted by a stable key.
- Any “representative selection” must be deterministic with documented tie-breakers.
- Tests must demonstrate stability under input reordering.

---

### Phase 15 — Derived Analytics Refusal Boundary (Phase 16)
Date: 2026-02-17

Decision:
Prompts requesting derived analytics not present in Fact Packs (ranking, argmax windows, shares/threshold crossing) must refuse with `UNSUPPORTED_CAPABILITY` rather than being mapped into a descriptive trend/overview response.

Rationale:
- Answering a different question “as if” it answered the user’s question violates trustworthiness.
- Derived analytics are explicitly deferred to Phase 16 unless included deterministically in Fact Packs.

Implications:
- Intent parsing/validation recognizes derived-analytics language (e.g., “fastest”, “largest”, “which fuel”, “share”, “exceed”).
- Phase 15 does not add new analytics computation; refusal is correct.
- Tests must ensure these prompts do not return generic trend narratives.

### Phase 15 — Architectural Convergence Mode
Date: 2026-02-19

Decision:
Phase 15 operates in strict architectural convergence mode. All findings from the high-level code review must either be executed, explicitly deferred with rationale, or explicitly dropped with justification before Phase 15 can close.

Rationale:
- Phase 15 is the polish phase; architectural debt must not roll into Phase 16.
- The project is transitioning from exploratory build-out to portfolio-grade presentation.
- Architectural clarity is more important than feature expansion at this stage.

Implications:
- Findings are not tracked as checklists.
- Remediation tasks are executed chronologically.
- No reprioritization based on severity; execution order governs.
- Items dropped from remediation must be explicitly justified.

---

### Phase 15 — Architectural Review Triage Policy
Date: 2026-02-19

Decision:
Architectural review findings are observational artifacts. The remediation plan is the only executable artifact.

Rationale:
- Findings document what exists.
- Remediation plan defines what changes.
- Conflating the two creates confusion and false task tracking.

Implications:
- `archive/phase15_doc_convergence/phase15_architectural_review_findings.md` is a historical findings record, not an execution checklist.
- `archive/phase15_doc_convergence/phase15_architectural_remediation_plan.md` is the historical remediation execution contract for the completed Phase 15 convergence run.
- `progress.md` references the remediation plan but does not duplicate its tasks.
- Findings not promoted into the remediation plan are considered intentionally accepted design tradeoffs.

---

### Phase 15 — dataset_release Semantics Across Ask and Read
Date: 2026-02-20

Decision:
`dataset_release` is the explicit lineage boundary; ask flows pin to one canonical release per request, while read APIs remain release-transparent row retrieval and expose per-row `releaseId`.

Rationale:
- Ask answers and citations must be deterministic and provenance-stable.
- Read endpoints are retrieval interfaces and should not silently synthesize cross-release aggregates.

Implications:
- Fact Pack builders must continue canonical release pinning before fact construction.
- Read endpoints return persisted rows with `releaseId` and do not apply hidden “latest release” collapsing semantics.

---

### Phase 15 — Independent SE Status Audit and Remediation PR Strategy
Date: 2026-02-20

Decision:
Run a full independent Staff Engineer audit of the current Phase 15 remediation status against architectural findings, and implement any unresolved corrective work in explicit follow-up PRs rather than burying changes through history rewriting.

Rationale:
- A full SE status check provides an authoritative verification of what is truly fixed vs still open.
- Separate remediation PRs preserve a transparent engineering narrative for portfolio review.
- Hiding corrective work in rewritten history reduces traceability and weakens reviewability.

Implications:
- The SE audit artifact is treated as an explicit checkpoint input for remaining Phase 15 work.
- Unresolved findings are addressed in new, reviewable PRs scoped to concrete remediation steps.
- Commit history remains honest about discovery and correction flow; no “fixup to hide findings” workflow for this remediation pass.


### API v1 Namespace Semantics + Dataset Release Semantics Alignment (Phase 15)
Date: 2026-02-21

Decision:
- /api/v1 is a path-based stability namespace (URL namespacing), not a runtime multi-version mechanism (no header/content-negotiation routing implied).
- dataset_release is a first-class ingestion/lineage key.
- Ask (/explanations/ask) must pin to one canonical dataset_release per request for determinism.
- Read endpoints are intentionally release-transparent and may return rows across multiple releases, exposing releaseId per row as provenance.

Rationale:
This matches current implementation and supports deterministic grounded explanations without expanding scope into a version lifecycle / negotiation framework.

Implications / Follow-ups (Phase 15):
1. Standardize FactPack provenance datasetReleaseId format across builders (prefer UUID string; define explicit fallback).
2. Document read endpoint semantics: multi-release transparency is expected; releaseId is a provenance key.
3. Make read ordering deterministic when duplicates across releases are possible by including releaseId (or equivalent) as a tie-breaker in repository ORDER BY clauses.
4. Remove any brittle coupling to literal /api/v1 text where feasible (or explicitly document it as a convention dependency).

---

### Phase 15 — LAWA State/Trend Indicator Vocabulary Split Is Intentional
Date: 2026-02-22

Decision:
LAWA indicator normalization remains dataset-specific: state and trend maintain independent normalized indicator vocabularies, and cross-view indicator reuse is not assumed.

Rationale:
- LAWA state and trend source datasets use different indicator semantics and naming conventions.
- Forcing premature unification would introduce ambiguous mappings and risk semantic drift in Phase 15 scope.
- Current backend behavior already treats the two indicator lists as independent (`/state/.../indicators` and `/trend/.../indicators` from separate tables).

Implications:
- Frontend must treat indicator filters as view-specific and avoid carrying stale indicator values across State/Trend view switches.
- UI copy should explicitly state that indicator codes can differ by dataset view.
- Any future canonical cross-dataset indicator taxonomy requires a separate explicit decision and migration plan.

---

### LAWA Browse Filter Gating Relaxation (State + Trend)
Date: 2026-02-25
Supersedes: strict filter gating language under "LAWA Unified Visualization Model (Trend + State)" (2026-02-14)

Decision:
For LAWA browse data loading and chart rendering, both State and Trend views are gated by:
- Region selected OR Indicator selected

Rationale:
- Improves exploration flow by allowing users to start from either geographic scope or indicator scope.
- Preserves semantic boundaries while reducing unnecessary empty-state friction.
- Aligns product behavior with reviewer expectations for guided, low-friction discovery.

Implications:
- Frontend gating logic must treat State and Trend consistently (`region || indicator`).
- Empty-state copy must instruct users to choose a region or indicator (not both).
- Existing Trend/State semantic constraints remain unchanged (classification/band interpretations, no derived analytics).

---

### Phase 17 — Capability Vocabulary Structuring Without Registry Replacement
Date: 2026-02-25

Decision:
Internal capability vocabulary may be represented with stronger typing/structure for maintainability, but capability support authority remains the central capability registry contract.

Rationale:
- String-only internal handling increases accidental inconsistency risk during capability expansion.
- Structured internal vocabulary improves maintainability and reviewability.
- Replacing registry authority would create hidden coupling and reduce contract audibility.

Implications:
- API/DTO wire contracts remain string-based and backward compatible.
- Registry remains source of truth for supported questionType/dataset/filter combinations.
- Internal structuring must not become a second support matrix.

---

### Phase 17 — Parser Normalization Must Drop Non-Actionable Categorical Placeholders
Date: 2026-02-25

Decision:
If NL parsing yields placeholder categorical values (for example `metricType=unknown`), normalization must treat them as absent before validation.

Rationale:
- Placeholder categorical values create nondeterministic refusal/answer behavior for identical prompts.
- Validation should reject explicit unsupported values, not parser uncertainty placeholders.
- Deterministic outcomes are a core contract for reviewer trust.

Implications:
- Normalization layer strips placeholder categorical values before capability validation.
- Builders/default metric behavior applies when optional categorical filters are absent.
- Determinism regressions are covered by parser/service tests and expanded corpus checks.

---

### Retroactive Python Tooling Bootstrap for Archived Scripts
Date: 2026-02-27

Decision:
Provide optional, retroactive Python tooling bootstrap support for archived scripts under `archive/`, using local `.venv` setup and dependency pinning in `archive/tools/python/`.

Rationale:
- Python-based helper scripts exist in repository history/artifacts and can trigger IDE unresolved-reference noise without interpreter/dependency setup.
- A lightweight, optional bootstrap improves contributor/reviewer onboarding without coupling runtime paths to Python.

Implications:
- Python tooling remains optional and is not required for backend/frontend runtime.
- Bootstrap entrypoints and docs live under `archive/tools/python/` (`README.md`, `Makefile`, `setup-python.sh`, `requirements.txt`).
- Root runtime paths (Docker ingestion, backend/frontend startup) remain Java/Node only and do not depend on Python.
