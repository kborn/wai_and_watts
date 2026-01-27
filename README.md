# wai_and_watts
Wai & Watts — Explore New Zealand environmental state & trends with a Spring Boot data platform

## Project Scope & Narrative

Wai & Watts is designed as a hybrid environmental storytelling platform and data platform engineering artifact.

The project intentionally sequences datasets and system capabilities to mirror how production data platforms evolve. Phase 6 ingests annual MBIE electricity generation data to establish ingestion lifecycle, lineage tracking, and domain persistence. Phase 7 introduces quarterly MBIE data to demonstrate schema evolution, heterogeneous ingestion, and extensibility. Later phases integrate additional environmental datasets and grounded LLM-based explanations.

Annual electricity data provides long-term structural decarbonization trends, while quarterly data exposes operational grid variability and fossil backup dynamics. Together, they enable honest environmental narratives while exercising realistic data engineering patterns such as forward-only migrations, idempotent ingestion, dataset-specific parsers, and reusable orchestration layers.

Wai & Watts is not intended to be a real-time energy dashboard or forecasting system. Instead, it serves as a portfolio-grade demonstration of how a senior backend/data platform engineer designs systems that grow safely and explain complex environmental data in a grounded way.

## Engineering Philosophy
Wai & Watts is built as a deliberately sequenced data platform rather than a feature-driven demo. The project prioritizes correctness, provenance, and evolvability over breadth.

### Key principles:
- Sequencing over completeness: ingestion lifecycle, lineage, and idempotency are implemented before parsing complex datasets.
- Semantic clarity before scale: the system models published interpretations (e.g., MBIE generation statistics) rather than raw telemetry to avoid ambiguous domain semantics.
- Schema evolution as a first-class concern: migrations are forward-only, and domain models are intentionally minimal until real requirements emerge.
- Grounded AI integration: LLMs are used only for fact-grounded explanation layers; they never write production code or make autonomous architectural decisions.
- Portfolio realism: the architecture mirrors production data platform patterns (lineage tables, ingestion orchestration, DTO/service layering, CI guardrails) rather than toy CRUD demos.

The goal is not to maximize features, but to demonstrate how a senior engineer designs systems that can grow safely over time.

## Repository layout

Top-level modules:
- `backend/` — Spring Boot application (Java 21, Spring Boot 3.3)
- `frontend/` — (planned) thin web client

Backend source tree has moved under `backend/src/...`:
- `backend/src/main/java/nz/waiwatts/...`
- `backend/src/main/resources/...`

## Key Documentation

This project uses structured documentation to support AI-assisted development and future contributors:

- `project-context.md` — project purpose, repo layout, and session recovery guide
- `progress.md` — current phase, tasks, and Definition of Done
- `decisions.md` — append-only architectural and sequencing decisions
- `specs/phase-6-mbie-ingestion.md` — MBIE dataset selection and ingestion rationale
- `design/mbie-schema.md` — Phase 6 database schema and fixture contract


## Build & Test (local)

From repository root:
- Build + test backend: `mvn -f backend clean verify`
- Test only: `mvn -f backend clean test`

## Run (backend)

At this stage (Step 2), there are no controllers, entities, or ingestion code. The goal is only to boot the app and run Flyway V1.

Option A (cd into module):
- `cd backend`
- Start (dev profile active): `mvn spring-boot:run`

Option B (from root using -pl):
- `mvn -pl backend spring-boot:run`

Expected on startup:
- Flyway applies migrations (`V1..Vn`) successfully.
- Application starts without errors.

Config:
- Postgres via env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD` (defaults provided in `application.yml`).

Note: The legacy `src/` directory at repo root is not built and can be safely deleted outside this environment.

---

## Pre-push guardrail (recommended)

Before pushing, run the test suite locally:

```
mvn -f backend clean verify
```

Optional: install a Git pre-push hook to prevent pushing broken builds (build only, tests run in CI):

```
cp scripts/git-hooks/pre-push.sample .git/hooks/pre-push
chmod +x .git/hooks/pre-push
```

This hook will run `mvn -f backend -DskipTests clean package` and abort the push if the build fails.

---

## Continuous Integration (GitHub Actions)

CI runs on every pull request and on pushes to `main`. It builds the backend and runs all tests.

Badge:

![Backend CI](https://github.com/kborn/wai_and_watts/actions/workflows/ci.yml/badge.svg)

Workflow:
- Location: `.github/workflows/ci.yml`
- Java: Temurin JDK 21
- Build: `mvn -f backend -B clean verify`
