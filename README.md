# Wai & Watts

**AI-Governed Environmental Data Platform**

Wai & Watts is a contract-first ingestion and normalization platform for
real New Zealand environmental datasets (MBIE electricity generation and
LAWA water quality).

It also serves as a case study in disciplined AI-augmented engineering:\
AI accelerated implementation, while architecture, invariants, and
system boundaries remained explicitly human-governed.

------------------------------------------------------------------------

# Why This Project Exists

Modern engineering teams are rapidly integrating AI into development
workflows.

Wai & Watts explores a more important question:

> How do you design architectural guardrails that allow AI to accelerate
> delivery without compromising correctness, provenance, or system
> integrity?

This repository demonstrates that control model.

------------------------------------------------------------------------

# System Overview

Wai & Watts is deliberately:

-   **Lineage-first** (dataset releases are explicit and immutable)
-   **Contract-first** (ingestion consumes normalized CSV contracts
    only)
-   **Idempotent by design** (DB-level uniqueness guarantees
    correctness)
-   **LLM-safe** (Fact Packs are the exclusive explanation boundary)

## Core Components

**Backend** - Spring Boot (Java) - Postgres - Flyway migrations -
Deterministic ingestion lifecycle

**Frontend** - React + TypeScript - Thin UI over stable APIs -
Visualization only --- no domain logic

**LLM Layer** - Fact Pack boundary - Intent parsing (routing-only) -
Deterministic refusal for unsupported queries

The LLM interprets language only, the backend validates requests and computes facts/metrics, and supported capabilities are registry-driven and discoverable via `GET /api/v1/capabilities`.

------------------------------------------------------------------------

# Quick Start (Local Development)

The entire stack (Postgres, backend, frontend) runs via Docker Compose.

## Start the application

``` bash
docker compose up --build
```

This starts:

-   Postgres database\
-   Spring Boot backend (http://localhost:8080)\
-   React frontend (http://localhost:5173)

Stop the stack with:

``` bash
docker compose down
```

## Operability Proof (3 Commands)

```bash
# 1) Start infrastructure + app
docker compose up -d --build

# 2) Verify backend health
curl -s http://localhost:8080/api/v1/health

# 3) Verify dataset catalog endpoint
curl -s http://localhost:8080/api/v1/datasets
```

Expected outcomes:
- command 1 starts `postgres`, `backend`, and `frontend` containers with no build/runtime errors.
- command 2 returns a health payload (HTTP 200).
- command 3 returns a JSON dataset list (HTTP 200), confirming API + DB connectivity.

------------------------------------------------------------------------

# Ingesting Data

Ingestion is deterministic and contract-driven.

Canonical flow:

download → transform → ingest (CLI inside backend container)

### Data Population

Data files are bundled in the container at `/app/downloads/`.
To run ingestion per dataset (transform + ingest), use the pipeline entrypoint:

```bash
# MBIE annual generation
docker compose run --rm ingest \
  mbie.generation.annual --bundle-date 2026-02-06 \
  --published-date 2025-11-01 --release-label "MBIE Q3 2025"

# MBIE quarterly generation
docker compose run --rm ingest \
  mbie.generation.quarterly --bundle-date 2026-02-06 \
  --published-date 2025-11-01 --release-label "MBIE Q3 2025"

# LAWA state (multi-year)
docker compose run --rm ingest \
  lawa.water_quality.state.multi_year --bundle-date 2026-02-06 \
  --published-date 2025-10-15 --release-label "LAWA Oct 2025"

# LAWA trend (multi-year)
docker compose run --rm ingest \
  lawa.water_quality.trend.multi_year --bundle-date 2026-02-06 \
  --published-date 2025-10-15 --release-label "LAWA Oct 2025"
```

The pipeline will:
1. Transform XLSX files to CSV
2. Ingest into the database
3. Handle idempotency (skip if already ingested)

Notes:
- `--bundle-date YYYY-MM-DD` derives the workbook path from `downloads/<provider>/<date>/...`.
- `--input /path/to/workbook.xlsx` overrides the derived path.
- `--published-date` and `--release-label` are optional flags.
- If `downloads/manifest/<bundle-date>.json` exists, `--published-date` and `--release-label` can be omitted.

Example (manifest-driven):

```bash
docker compose run --rm ingest-all --bundle-date 2026-02-06
```

Idempotency is enforced via database uniqueness on:

(dataset_source_id, content_hash)

Duplicate ingestions are treated as successful no-ops.

------------------------------------------------------------------------

# Example API Calls

List datasets:

GET http://localhost:8080/api/v1/datasets

Retrieve releases:

GET http://localhost:8080/api/v1/datasets/{code}/releases

Generate explanation:

POST http://localhost:8080/api/v1/explanations

LLMs operate only on structured Fact Packs. They never query the
database directly.

------------------------------------------------------------------------

# Repository Structure

/docs → Curated architectural narrative\
/engineering → Governance artifacts and execution history\
/archive → Historical phase artifacts and legacy documentation\
/backend\
/frontend

------------------------------------------------------------------------

# Architectural Narrative

See:

-   docs/00-executive-overview.md
-   docs/01-architecture.md
-   docs/02-ai-governance-case-study.md
-   docs/03-design-invariants.md
-   docs/04-operational-model.md
-   docs/05-llm-safety-model.md
-   docs/06-documentation-governance.md

------------------------------------------------------------------------

# AI Governance Summary

AI was used extensively for implementation.

However:

-   Architecture, invariants, and sequencing were human-owned.
-   Role boundaries were explicit.
-   Escalation was required for new architectural surface area.
-   LLM access is constrained to deterministic Fact Packs.
-   No autonomous AI commits were permitted.

This project demonstrates disciplined AI acceleration --- not
uncontrolled generation.
