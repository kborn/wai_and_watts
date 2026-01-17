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

