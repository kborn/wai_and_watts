# Architecture — Wai & Watts

This document explains the system boundaries, key flows, and invariants at a level intended for senior/staff reviewers.

For authoritative governance and sequencing, see:
- `engineering/project-context.md`
- `engineering/progress.md`
- `engineering/decisions.md`

## High-level shape

Wai & Watts is deliberately **contract‑first** and **lineage‑first**.

- **Lineage models** are shared across all datasets and enforce idempotency.
- **Domain models** are dataset‑specific and always reference a lineage release.
- **Read APIs** are public, versioned, and DTO‑based (entities are never exposed).
- **LLM explanations** are grounded exclusively via Fact Packs.

## Core components

### Backend (Spring Boot + Postgres)
- **Lineage schema**
  - `dataset_source` — stable identity (`code`) + metadata
  - `dataset_release` — immutable release with content hash, status, timestamps
- **Ingestion lifecycle**
  - Creates or reuses releases based on `(dataset_source_id, content_hash)` uniqueness
  - Treats duplicates as successful no‑ops (idempotency is DB‑authoritative)
- **Dataset modules**
  - MBIE annual + quarterly generation
  - LAWA state + trend (multi‑year)

### Frontend (React + TS)
- Thin client over stable APIs
- No domain logic or explanation logic in UI
- Visualization is presentational; backend remains source of truth

### LLM layer (Grounded explanations)
- Fact Packs are the **exclusive data boundary**
- Natural language endpoint performs **intent parsing only**
- Unsupported or ambiguous requests → deterministic refusal

## Key flows

### 1) Ingestion (operator-driven)

Canonical operator flow:

`download → transform → ingest (CLI) → start backend → validate APIs`

Important boundary:
- **Transform** converts publisher artifacts (often XLSX) to **contract CSV**
- **Ingestion** consumes contract CSV only

Idempotency:
- Hashing is performed on the contract CSV bytes
- DB uniqueness on `(dataset_source_id, content_hash)` is the source of truth

### 2) Query + read APIs

- Public endpoints live under `/api/v1/...`
- Controllers call services and return DTOs
- Repositories are not used directly in controllers

### 3) Explanations (Fact Pack safety model)

`Question → (optional) Intent parsing → Structured ExplanationRequest → Fact Pack builder → Explanation renderer → Response + citations`

Hard boundaries:
- LLMs never access DB or domain entities
- LLM outputs must cite Fact Pack fields/IDs
- Refusal is correct behavior when unsupported

## What was intentionally deferred (portfolio clarity)

- Scheduling / polling / orchestration infrastructure
- Streaming or raw telemetry ingestion
- Prediction / forecasting / ML inference
- Complex data platform storage zones (raw/bronze/silver, etc.)

The project optimizes for **sequencing, provenance, and correctness** over breadth.

## Where to go next

- AI governance: `docs/02-ai-governance-case-study.md`
- Invariants: `docs/03-design-invariants.md`
- Operational model: `docs/04-operational-model.md`
- LLM safety: `docs/05-llm-safety-model.md`
