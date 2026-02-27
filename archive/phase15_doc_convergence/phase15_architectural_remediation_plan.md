# Phase 15 --- Architectural Remediation Plan

Status: Complete
Completed: 2026-02-21
Intent: Converge the codebase to senior-level architectural polish before Phase 16.

This document is the executable chronological contract for Phase 15 remediation.
There is no severity ranking; execution order governs.

------------------------------------------------------------------------

## Execution Order

------------------------------------------------------------------------

## Step 1 --- Backend Ask Pipeline Hardening
Description: Tighten `/api/v1/explanations/ask` for deterministic, stable, production-credible behavior.

1.1 Validate endpoint versioning intent and correctness.
1.2 Validate `dataset_release` semantics and ensure meaningful, consistent usage.
1.3 Replace Java in-memory filtering with repository-level filtered queries where possible.
1.4 Eliminate any remaining non-deterministic citation ordering.
1.5 Ensure refusal model is explicit and stable (no `INTERNAL_ERROR` leakage).
1.6 Confirm controller/service boundary separation.
1.7 Remove any redundant DTO transformation layers.

Completion Criteria:
- No in-memory filtering in critical read paths.
- Deterministic citation ordering verified.
- No ambiguous refusal behavior.
- Clear dataset release intent documented.

------------------------------------------------------------------------

## Step 2 --- Data Normalization & Schema Hygiene
Description: Remove modeling drift and enforce consistent persisted semantics.

2.1 Normalize metadata columns across all domain tables.
2.2 Fix region string case inconsistencies.
2.3 Remove unused `units` field from LAWA trend table and related DTOs.
2.4 Create abstract CSV parser base class.
2.5 Refactor duplicated logic from LAWA state and trend where duplication exists.

Completion Criteria:
- No inconsistent casing in persisted domain rows.
- No unused schema fields.
- Shared parsing logic consolidated.
- All migrations stable.

------------------------------------------------------------------------

## Step 3 --- Feature Usage Validation
Description: Validate that runtime behavior matches intended architecture contracts.

3.1 Validate version usage in endpoints (is it meaningful?).
3.2 Validate `dataset_release` usage intent.
3.3 Review LLM stub behavior vs real provider.
3.4 Confirm stub cannot silently diverge from production contract.

Completion Criteria:
- Endpoint versioning documented or simplified.
- `dataset_release` role explicitly documented.
- Stub and real provider behavior contract-aligned.

------------------------------------------------------------------------

## Step 4 --- Test Coverage Floor
Description: Establish a portfolio-credible coverage baseline with CI enforcement.

4.1 Add JaCoCo plugin.
4.2 Set coverage threshold (70--80%).
4.3 Fail CI if threshold is not met.
4.4 Review uncovered critical logic.
4.5 Add tests for uncovered business logic.

Completion Criteria:
- CI enforces coverage threshold.
- Critical business paths covered.
- No untested fact-pack logic.

------------------------------------------------------------------------

## Step 5 --- Frontend Architecture Sanity Sweep
Description: Keep frontend presentational while improving clarity and UX consistency.

5.1 Confirm no domain logic in frontend.
5.2 Simplify awkward Ask → Result flow.
5.3 Improve refusal UX clarity.
5.4 Improve explanation of citations.
5.5 Add UI signal when no LLM provider is configured.
5.6 Improve contextual explainer text.

Completion Criteria:
- Frontend remains purely presentational.
- Ask flow feels intentional and clean.
- Refusal states are differentiated and clear.

------------------------------------------------------------------------

## Step 6 --- Documentation & Portfolio Narrative
Description: Make architecture and usage understandable without session context.

6.1 Add Project Scope & Narrative to README.
6.2 Add Architecture Evolution Narrative.
6.3 Add Repo Map.
6.4 Add `DEMO.md` with curl examples.
6.5 Add 5-minute interview walkthrough script.
6.6 Add architectural diagram.
6.7 Add dataset taxonomy table.
6.8 Ensure cohesion across docs.

Completion Criteria:
- Repo is self-explanatory without session context.
- Demo can be executed in under 5 minutes.
- Architecture is visually understandable.

------------------------------------------------------------------------

## Exit Condition

Phase 15 is complete when:
- All above steps are complete.
- No architectural concerns remain unaddressed.
- Project is presentation-ready for recruiters.
- Phase 16 can focus exclusively on expanding NL/LLM capability.

## Dropped Findings

Findings not included in this plan are considered intentional design tradeoffs for Phase 15.
Rationale for omission is documented in decisions.md.

## Completion Record (2026-02-22)

Execution of all remediation steps is complete.
Verification and closure evidence is documented in:
- `docs/phase-notes/phase15/phase15_second_round_verification_findings.md` (including final closure addendum)
- `docs/phase-notes/phase15/phase15_refactoring_execution_checklist.md` (status + final closure notes)
