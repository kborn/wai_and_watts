# AI Onboarding Runbook — Wai & Watts

This document defines the **procedural steps** for onboarding AI tools (ChatGPT, IDE copilots, custom GPTs) to the Wai & Watts repository.

This file is intended for **repository documentation**, not for direct AI consumption.

---

## 1) When to Run This Process

Run this onboarding procedure when:
- Starting a new AI chat session
- Adding a new dataset or domain
- After major refactors or schema changes
- Before starting a new project phase

---

## 2) Files to Share with an AI Agent

Always provide these files, in order:

1. `docs/ai-dev/project-context.md`
2. `docs/ai-dev/progress.md`
3. `docs/ai-dev/decisions.md`
4. `docs/ai-dev/roles.md`
5. `docs/ai-dev/ai_usage.md`

Phase-specific (if applicable):
- `specs/<current-phase>.md`
- `design/<current-schema>.md`

Avoid sharing the entire repository unless code-level changes are required.

---

## 3) Initial AI Prompt Template

Use the following prompt to bootstrap context:

"""
You are Staff Engineer GPT for Wai & Watts.
Read project-context.md and the canonical docs in the order specified and summarize:
1) Project purpose
2) Current phase and next phase
3) Ingestion architecture
4) AI roles and rules

You do not have access to the code base and should only suggest implementation details when prompted.
"""

---

## 4) Onboarding Validation Questions

Before assigning engineering tasks, ask the AI:

1. What is Wai & Watts?
2. What phases are complete and what is next?
3. How does the ingestion lifecycle work?
4. Where are fixtures stored?
5. Where are schema and design docs stored?
6. Where are progress and decision docs stored?
5. What naming conventions must be followed?
6. What are Staff vs Builder vs PM GPT responsibilities?
7. What is explicitly out of scope?

---

## 5) Acceptance Criteria

Onboarding is successful if the AI:
- Correctly identifies the current and next phases
- Explains dataset lineage and idempotency
- Follows naming and dataset taxonomy conventions
- Avoids unsolicited code generation
- Respects project non-goals (no forecasting, no real-time dashboards)

---

## 6) Failure Remediation

If onboarding fails:
1. Update documentation (project-context.md, progress.md, decisions.md)
2. Add missing conventions or clarifications
3. Re-run onboarding questions
4. Record onboarding friction in documentation as a knowledge gap

Treat onboarding failures as documentation bugs.

---

## 7) Post-Onboarding Workflow

Once validated:
- Staff GPT: architecture, sequencing, documentation governance
- Builder GPT: code, migrations, tests, implementation details
- PM GPT: specs and narrative documentation
- Human developer: approves commits and scope

Autonomous AI commits are forbidden.

---

## 8) Documentation Goal

This runbook ensures AI agents can be onboarded deterministically and consistently, mirroring human onboarding practices in production data platform teams.
