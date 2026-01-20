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
