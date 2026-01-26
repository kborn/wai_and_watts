# Wai & Watts — Project Context

## Purpose
Wai & Watts is a portfolio project demonstrating:
- senior Java / Spring Boot backend engineering
- real New Zealand environmental data processing
- disciplined, intentional, agentic use of LLMs

The project emphasizes **sequencing, provenance, and correctness** over feature breadth.

---

## Problem Domain
- River water quality “state & trend” data (LAWA)
- Electricity generation and renewables data (MBIE)

The system models **published interpretations**, not raw measurements.

---

## Non-Goals
- Raw time-series ingestion for water monitoring
- Autonomous AI committing code
- ML-based predictions or forecasting
- Frontend-heavy business logic

---

## Architecture Summary
- Spring Boot backend
- Postgres database
- REST APIs (versioned)
- Dataset lineage modeled explicitly
- LLMs used only for grounded explanations

---

## Canonical Documents (Read Order)

1. **project-context.md** — orientation and authority map
2. **roles.md** — who is allowed to do what
3. **progress.md** — phases, current position, and definition of done
4. **DECISIONS.md** — non-negotiable architectural and workflow decisions
5. **AI_USAGE.md** — how AI tools are used and constrained

If documents conflict, authority flows top to bottom.

### Session recovery quickstart
- If session context is lost or unclear, follow the procedure in the section above: “Canonical Documents (Read Order)”.
- Follow the stated authority flow (top → bottom). If conflicts remain or scope is ambiguous, pause and escalate per the Builder GPT escalation policy in `roles.md`.

---

## Repository Guardrails

- All backend Java code lives under `backend/src/main/java`
- No Java code at repo root
- Controllers never expose entities directly
- Lineage models precede domain models

If a guardrail is at risk due to a required change, stop and escalate per `roles.md`.


## Documentation Taxonomy (Authoritative)

Wai & Watts documentation is organized by intent. New AI sessions must follow this taxonomy.

- **project-context.md** — High-level project purpose, architecture, repo layout, and how to resume context.
- **progress.md** — Execution state, phases, current phase, and Definition of Done. This is the operational source of truth.
- **decisions.md** — Append-only architectural and sequencing decisions with rationale and implications. Do not duplicate specs here.
- **specs/** — Product and data domain specifications (what to ingest, why, acceptance criteria). Phase-specific.
- **design/** — Technical contracts (schemas, mappings, constraints, ingestion contracts). How the system is structured.
- **ai_usage.md / roles.md** — AI workflow rules and role boundaries.

If a document does not fit one of these categories, it should not be added.

## Repository Layout
- backend/ — Spring Boot app (ONLY Java code lives here)
- frontend/ — future thin client
- specs/ — domain/product specs
- design/ — technical contracts
- docs/ai-dev/ — AI workflow docs

---

## Final Note
This document is intentionally stable.
If it changes often, something else is missing.
