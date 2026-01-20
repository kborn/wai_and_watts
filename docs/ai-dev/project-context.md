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

---

## Repository Guardrails

- All backend Java code lives under `backend/src/main/java`
- No Java code at repo root
- Controllers never expose entities directly
- Lineage models precede domain models

---

## Final Note
This document is intentionally stable.
If it changes often, something else is missing.
