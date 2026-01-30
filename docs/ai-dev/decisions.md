# Wai & Watts — Engineering Decisions Log

Append-only record of why irreversible choices were made.

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
- DTO: `MbieGenerationRecordDto { period (YEAR for Phase 6), source (fuel_type_norm), sourceRaw (fuel_type_raw), generationMwh }`

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

Decision: Store dataset contracts in design/, rationale in specs/.

Rationale: Keep decisions.md readable and stable.

Implications: Phase 6 contract lives in design/mbie-schema.md.


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

### Phase 8 LAWA Dataset Selection (State Attribute Band)

Date: 2026-01-28

Decision: Use LAWA River Water Quality State & Trend Results workbook, sheet State Attribute Band, as the Phase 8 ingestion source with code lawa.water_quality.state.multi_year.

Rationale:
- Published interpretation (state bands) rather than raw telemetry, matching project principles.
- Structurally stable, policy-relevant, and cross-domain proof without time-series/geospatial complexity.
- Multi-year aggregation semantics align with a clear period_start_year/period_end_year model.

Implications:
- Phase 8 fixture and parser are based on State Attribute Band fields; other LAWA tabs (quartiles, trend-only) are out of scope unless a future decision adds them.
- Trend is not required for Phase 8 unless explicitly added later as a separate dataset or join.