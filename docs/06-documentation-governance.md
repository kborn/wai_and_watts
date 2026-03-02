# Documentation Governance (Single Source of Truth)

## Canonical surfaces

| Surface | Purpose | Update Owner |
|---|---|---|
| `README.md` | Repo entrypoint, quick onboarding, operability proof | Maintainers |
| `docs/00-executive-overview.md` to `docs/05-llm-safety-model.md` | Curated portfolio narrative for reviewers | Staff Engineer documentation track |
| `docs/07-reviewer-quickstart.md` | Operational reviewer checklist and validation commands | Maintainers |
| `docs/08-change-risk-checklist.md` | Pre-merge risk checklist for high-signal change control | Maintainers |
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

## API deprecation policy

#### Note on tooling: 
Some IDE HTTP inspections flag `Deprecation`/`Sunset` as “unknown headers”.
These headers are intentionally used as part of the API deprecation contract.
If your IDE flags them, add them to the HTTP Client inspection’s custom header allow-list.

Canonical endpoints:
- `GET /api/v1/capabilities`
- `GET /api/v1/health`

Canonical public endpoints should not emit deprecation metadata headers.
3. For capability endpoints, canonical and legacy payloads must remain response-equivalent until alias removal.
4. Alias removal requires:
   - docs update in this file and reviewer quickstart,
   - contract-test updates proving canonical behavior remains stable.

## Reviewer read order

1. `README.md`
2. `docs/00-executive-overview.md`
3. `docs/01-architecture.md`
4. `docs/02-ai-governance-case-study.md`
5. `docs/03-design-invariants.md`
6. `docs/04-operational-model.md`
7. `docs/05-llm-safety-model.md`
8. `docs/07-reviewer-quickstart.md`
9. `docs/08-change-risk-checklist.md`
10. `engineering/project-context.md`
11. `engineering/roles.md`
12. `engineering/progress.md`
13. `engineering/decisions.md`
14. `engineering/ai_usage.md`
