# Documentation Governance (Single Source of Truth)

## Canonical surfaces

| Surface | Purpose | Update Owner |
|---|---|---|
| `README.md` | Repo entrypoint, quick onboarding, operability proof | Maintainers |
| `docs/00-executive-overview.md` to `docs/05-llm-safety-model.md` | Curated portfolio narrative for reviewers | Staff Engineer documentation track |
| `engineering/project-context.md` | Authority map and guardrails | Staff Engineer |
| `engineering/roles.md` | Role boundaries and escalation policy | Staff Engineer |
| `engineering/progress.md` | Delivery log and phase execution history | Staff Engineer |
| `engineering/decisions.md` | Architectural/workflow decisions (append-only) | Staff Engineer |
| `engineering/ai_usage.md` | AI operating model and constraints | Staff Engineer |
| `engineering/design/*` | Technical contracts and invariants | Backend architecture owner |
| `engineering/specs/*` | Phase scope and acceptance criteria | Product/architecture owner |

## Historical surfaces

| Surface | Purpose |
|---|---|
| `archive/` | Historical artifacts retained for traceability; not canonical |

## Update rules

1. When behavior changes, update canonical docs in the same change set as code.
2. Keep `engineering/decisions.md` append-only; do not rewrite historical decisions.
3. If canonical content is replaced or heavily rewritten, move prior versions to `archive/`.
4. Do not introduce legacy path references in canonical docs (`docs/ai-dev`, `docs/phase-notes`, `docs/operations`, `docs/validation`).
5. Keep `README.md` operability section runnable with copy/paste commands.

## Reviewer read order

1. `README.md`
2. `docs/00-executive-overview.md`
3. `docs/01-architecture.md`
4. `docs/02-ai-governance-case-study.md`
5. `docs/03-design-invariants.md`
6. `docs/04-operational-model.md`
7. `docs/05-llm-safety-model.md`
8. `engineering/project-context.md`
9. `engineering/roles.md`
10. `engineering/progress.md`
11. `engineering/decisions.md`
12. `engineering/ai_usage.md`
