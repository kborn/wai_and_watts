# Portfolio Engineering Process

This document explains how Wai & Watts was implemented as an engineering-led project, not prompt-only prototyping.

## 1) Delivery Model

Wai & Watts used a human-led, AI-assisted model with explicit role boundaries:
- Human owner:
  - owns architecture and scope decisions
  - approves all changes
  - owns commits and final direction
- Staff/Strategy AI role:
  - architecture review
  - sequencing and guardrails
  - risk identification
- Builder AI role:
  - scoped implementation
  - refactoring within constraints
  - tests and documentation updates for assigned tasks

Role source of truth:
- `docs/ai-dev/roles.md`

## 2) Decision and Execution Protocol

Work was governed through explicit documents, in this order:
1. `docs/ai-dev/project-context.md`
2. `docs/ai-dev/roles.md`
3. `docs/ai-dev/progress.md`
4. `docs/ai-dev/decisions.md`
5. `docs/ai-dev/ai_usage.md`

Expected execution pattern:
- Define/confirm phase goals in `progress.md`
- Capture non-negotiables in `decisions.md`
- Implement in small scoped changes
- Validate with tests and deterministic behavior checks
- Update docs as part of completion

## 3) Quality Controls and Guardrails

Engineering quality gates used throughout implementation:
- DB lineage + idempotency constraints as system-of-record behavior
- Fact-Pack grounded explanation boundary (no direct DB access by LLM providers)
- deterministic refusal taxonomy and deterministic builder/release selection
- CI build/test enforcement on JDK 21
- JaCoCo coverage threshold enforcement (70%)
- integration testing including Postgres-dialect checks

Representative artifacts:
- `backend/pom.xml`
- `.github/workflows/ci.yml`
- `docs/phase-notes/phase15/phase15_exit_rubric.md`
- `docs/phase-notes/phase15/phase15_generated_panel_spec.md`

## 4) Phase 15 Hardening Signal (Condensed)

Phase 15 focused on architectural hardening and convergence, including:
- ask pipeline determinism and refusal-envelope hardening
- in-memory query path cleanup to repository-filtered paths
- schema/data normalization cleanup
- coverage floor and critical path test closure
- frontend refusal-state clarity and consistency improvements

Condensed status is recorded in:
- `docs/ai-dev/progress.md` (Phase 15 section)

Full audit/remediation evidence trail is preserved in:
- `docs/archive/phase15/2026-02-doc-convergence/`

## 5) AI Process Deficiencies Observed

The project intentionally records process weaknesses encountered during AI-assisted delivery:
- session continuity loss across long-running ChatGPT work
- output quality/style drift between sessions
- standards mismatch risk between strategy-agent and builder-agent outputs
- insufficiently detailed specs causing over-constrained or under-constrained execution

Mitigations applied:
- stronger canonical-doc onboarding and validation
- archive-first documentation policy with explicit active-vs-historical split
- convergence pass to normalize paths/formatting and reconcile stale docs
- preserved historical execution breakdown in `progress.md`

Related docs:
- `docs/ai-dev/phase15_ai_retrospective.md`
- `docs/ai-dev/onboarding_validation_2026-02-23.md`
- `docs/ai-dev/documentation_convergence_audit.md`

## 6) Human Ownership and Accountability

AI accelerated implementation, but architecture authority remained human-owned.
This includes:
- decision acceptance/rejection
- scope control
- tradeoff decisions
- final review and commit accountability

This process is part of the portfolio signal: disciplined engineering execution under AI assistance, with explicit governance and traceability.
