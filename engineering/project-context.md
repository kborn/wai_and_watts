# Wai & Watts — Project Context

## Purpose
Wai & Watts is a portfolio project demonstrating:
- senior Java / Spring Boot backend engineering
- real New Zealand environmental data processing
- disciplined, intentional, agentic use of LLMs

The project emphasizes sequencing, provenance, and correctness over feature breadth.

---

## Problem Domain
- River water quality state/trend data (LAWA)
- Electricity generation and renewables data (MBIE)

The system models published interpretations, not raw measurements.

---

## Non-Goals
- Raw telemetry ingestion for water monitoring
- Autonomous AI code commits
- ML-based predictions or forecasting
- Frontend-heavy business logic

---

## Architecture Summary
- Spring Boot backend
- Postgres database
- REST APIs (versioned)
- Explicit dataset lineage model
- LLMs used only for grounded explanations via Fact Packs

---

## Canonical Documents (Read Order)
1. `project-context.md` — orientation and authority map
2. `roles.md` — who is allowed to do what
3. `progress.md` — phases, current position, definition of done
4. `decisions.md` — non-negotiable architectural/workflow decisions
5. `ai_usage.md` — how AI tools are used and constrained

If documents conflict, authority flows top to bottom.

### Session Recovery Quickstart
- Follow the canonical read order above.
- If conflicts remain or scope is ambiguous, escalate per Builder policy in `roles.md`.

---

## Repository Guardrails
- All backend Java code lives under `backend/src/main/java`
- No Java code at repo root
- Controllers do not expose entities directly
- Lineage models precede domain models

If a required change risks guardrails, stop and escalate per `roles.md`.

---

## Documentation Taxonomy (Authoritative)
- `docs/` — curated portfolio narrative (executive overview, architecture, governance story)
- `engineering/` — canonical governance + execution receipts (`project-context`, `roles`, `progress`, `decisions`, `ai_usage`, plus `engineering/design/` and `engineering/specs/`)
- `engineering/specs/` — product/data scope and phase acceptance criteria
- `engineering/design/` — technical contracts and architecture constraints (schemas, mappings, invariants)
- `archive/` — non-canonical historical records kept for traceability (legacy docs, phase notes, convergence artifacts)

Rule: if a document is replaced or heavily reworked, archive the previous version instead of deleting.

---

## Repository Layout
- `backend/` — Spring Boot app
- `frontend/` — thin client
- `engineering/specs/` — product/domain specs
- `engineering/design/` — technical contracts
- `docs/` — curated portfolio narrative
- `scripts/` — operator/ingestion utility scripts

---

## Phase Numbering Convention
Phase structure:
- `Phase N` → original milestone delivery
- `N.x` → implementation step inside milestone
- `N[A-Z]` → post-phase capability expansion
- `N[A-Z].x` → implementation step inside expansion

Example:
- Phase 14 → original UI milestone
- 14.1 → original phase implementation step
- 14A → MBIE timeline capability expansion
- 14A.1 → MBIE expansion step
- 14B → LAWA visualization expansion
- 14B.1 → LAWA expansion step

Rules:
- `.x` always indicates an implementation step
- Lettered phases represent expansions
- Use `14A`, not `14.A`

---

## Final Note
This document should remain stable. Frequent change indicates missing architecture/governance elsewhere.
