# Documentation Convergence Audit

Date: 2026-02-23
Status: Complete

This document records a repo-wide docs sanity pass and disposition decisions.

## 1) Design Coverage Review

Assessment:
- `design/` has broad coverage for datasets and explanation contracts.
- Early baseline docs (`001-003`) contain historical structures that no longer fully match current package naming and endpoint inventory.
- Later phase contract docs (`004+`) are generally aligned and should remain canonical for dataset/explanation behavior.

Backfill needed:
- Add one short design index mapping current runtime surfaces to design docs.

Completed in this pass:
- Refreshed `design/001-architecture.md` to match current module boundaries and execution paths.
- Refreshed `design/002-contracts.md` to match current endpoint/query/refusal contracts.
- Refreshed `design/003-nonfunctional.md` to match current runtime/CI determinism and coverage guardrails.

Not needed:
- Full retrospective rewrite of every design file.

## 2) Canonical Governance Docs Correctness

Reviewed:
- `docs/ai-dev/project-context.md`
- `docs/ai-dev/roles.md`
- `docs/ai-dev/progress.md`
- `docs/ai-dev/decisions.md`
- `docs/ai-dev/ai_usage.md`

Fixes applied in this convergence pass:
- Corrected canonical file naming/path consistency in `project-context.md`.
- Corrected taxonomy and directory mapping.
- Normalized `progress.md` and `decisions.md` formatting where drift had accumulated.

## 3) Operations Docs

Assessment:
- `docs/operations/OPERATOR_INGESTION_GUIDE.md` is still necessary as the canonical operator runbook.
- Archive docs under `docs/operations/archive/` remain useful as historical examples.

Action:
- Keep operations docs in current location.
- Ensure README points to `docs/operations/OPERATOR_INGESTION_GUIDE.md`.

## 4) Phase Notes

Assessment:
- Phase 15 had too many execution-time artifacts mixed with active references.

Action taken:
- Kept active Phase 15 docs/harness in `docs/phase-notes/phase15/`.
- Archived builder prompts, remediation execution notes, and verification artifacts to:
  `docs/archive/phase15/2026-02-doc-convergence/`

## 5) Product Docs

Assessment:
- `docs/product/phase_13_product_slice.md` is historical but still useful for frontend boundary intent.

Action:
- Keep as reference; treat as non-canonical historical product note.

## 6) Validation Docs

Assessment:
- `docs/validation/PHASE10_OPERATOR_TEST_DRIVE.md` is a validation playbook, not a runbook.

Action:
- Keep in `docs/validation/` and cross-link to operator guide for canonical execution steps.

## 7) Script Inventory Review

Keep:
- `scripts/download/mbie-download.sh`
- `scripts/download/lawa-download.sh`
- `scripts/transform.sh`
- `scripts/ingest.sh`
- `scripts/ingest-all.sh`
- `scripts/pipeline.sh`

Keep as non-active examples:
- `scripts/git-hooks/pre-push.sample`

Already archived:
- `scripts/archive/normalize_lawa_state_attribute_band.py`
- `scripts/archive/normalize_lawa_trend.py`

## 8) Additional Correctness Pass

Needed next:
- Link/path integrity sweep across docs after archive moves.
- Normalize stale endpoint examples in validation docs to current API routes.

## 9) README and Docker Docs

Assessment:
- README and Docker sections are useful but had stale links and scattered narrative ordering.

Action in this pass:
- Fixed known broken doc paths.
- Added path-based onboarding section with direct links to operator/validation/demo/docs map.
- Added canonical references for repo map, architecture diagram, taxonomy, and production-readiness notes.

Recommended follow-up:
- Rewrite README into three primary flows: local dev, docker demo, operator ingestion.
