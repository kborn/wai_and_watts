# Wai & Watts — Project Context

## Purpose
Wai & Watts is a portfolio project demonstrating senior Java/Spring Boot backend engineering,
real New Zealand environmental data processing, and disciplined, agentic use of LLMs.

## Problem Domain
- River water quality "state & trend" data (LAWA)
- Electricity generation and renewables data (MBIE)

## Non-Goals
- Raw time-series ingestion for water monitoring (out of scope for MVP)
- Autonomous AI committing code
- ML-based predictions

## Architecture Summary
- Spring Boot backend
- Postgres database
- REST APIs
- LLM used only for grounded explanations (fact-pack-first)

## AI Workflow
- PM / Staff / Data roles operate outside the IDE
- Builder GPT operates inside the IDE
- Human-in-the-loop for all commits


## Repository Layout

The repository is a multi-directory project:

- backend/ — Spring Boot application (ONLY place Java code lives)
    - backend/src/main/java/nz/waiwatts/...
    - backend/src/test/java/...
- frontend/ — thin web client (added later)
- specs/ — product specifications
- design/ — architecture and contracts
- docs/ai-dev/ — AI workflow documentation

IMPORTANT:
- All backend Java code MUST live under backend/src/main/java
- No Java code should be created at the repo root