# Contributing

## Scope
This repository is a portfolio project with explicit architecture and governance constraints.
Contributions should preserve contract-first ingestion, lineage correctness, and Fact Pack safety boundaries.

## First Read
1. `engineering/project-context.md`
2. `engineering/roles.md`
3. `engineering/decisions.md`
4. `docs/06-documentation-governance.md`
5. `docs/08-change-risk-checklist.md`

## Development Workflow
1. Create a branch from `main`.
2. Keep changes small and scoped to one concern.
3. Update canonical docs in the same change set as behavior changes.
4. Run relevant checks locally before opening a PR.

## Local Checks
```bash
# Backend contract tests
mvn -pl backend -Dgroups=contract test

# Backend full verification
mvn -f backend -B verify

# Frontend unit tests
npm --prefix frontend run test:run

# Canonical docs validation
./scripts/ci/check-docs.sh
```

## Pull Request Requirements
- Explain user-visible behavior changes.
- Call out API, schema, and migration impact explicitly.
- Include docs updates for changed behavior.
- Confirm checklist completion in `docs/08-change-risk-checklist.md`.

## Guardrails
- Do not expose entities directly from controllers.
- Do not bypass dataset lineage model for ingestion.
- Do not allow LLM providers to access repositories or raw entities.
- Keep schema evolution forward-only with Flyway migrations.
