# Phase 15 Refactoring --- Execution Checklist

This document converts the Phase 15 Architectural Remediation Plan into
atomic, Builder-executable tasks.

Each task: - Is independent and session-safe - Has tightly defined
scope - Must not expand beyond defined boundaries - Can be executed in
isolation

Execution order is chronological, but tasks are designed to be completed
in separate Builder sessions.

------------------------------------------------------------------------

# Step 1 --- Backend Ask Pipeline Hardening

## Task 1.1 --- Canonicalize Refusal Code Taxonomy

Objective: Unify refusal code spelling and eliminate dual usage (e.g.,
UNSUPPORTED_CAPABILITY vs UNSUPPORTED_CAPABILITY).

Scope: - DatasetSelectionService - RequestValidationService -
ExplanationController refusal mapping - Related tests

Definition of Done: - Only one canonical refusal code exists. - All
tests updated. - No dual spelling remains.

------------------------------------------------------------------------

## Task 1.2 --- Deterministic FactPackBuilder Selection

Objective: Ensure exactly one FactPackBuilder can handle a request and
enforce deterministic resolution.

Scope: - ExplanationServiceImpl.selectFactPackBuilder -
ExplanationConfig builder wiring

Definition of Done: - Fail fast if \>1 builder matches. - Deterministic
selection rule documented. - Unit test enforces uniqueness.

------------------------------------------------------------------------

## Task 1.3 --- Remove In-Memory Filtering in Ask Builders

Objective: Replace repository.findAll() + stream filtering with
repository-level filtered queries.

Scope: - All FactPackBuilder getRecordsForRequest methods

Definition of Done: - No repository.findAll() in ask pipeline. -
Repository methods reflect filter criteria. - Tests pass unchanged.

------------------------------------------------------------------------

## Task 1.4 --- Canonical Dataset Release Pinning

Objective: Ensure ask explanations use a single deterministic
dataset_release.

Scope: - FactPackBuilders (MBIE + LAWA) - Release selection logic

Definition of Done: - Single release used in fact construction. - Rule
documented in code. - Determinism harness stable.

------------------------------------------------------------------------

# Step 2 --- Data Normalization & Schema Hygiene

## Task 2.1 --- Normalize Region Casing at Persistence Boundary

Objective: Eliminate region string case inconsistencies.

Scope: - LAWA ingestion normalization layer - Read services if required

Definition of Done: - Persisted rows use canonical case. - No
case-dependent repository behavior.

------------------------------------------------------------------------

## Task 2.2 --- Remove Unused Schema Fields

Objective: Remove unused 'units' field from LAWA trend domain + DTOs.

Scope: - Flyway migration - Entity - DTO - Tests

Definition of Done: - Field removed everywhere. - Migration
forward-only. - No broken mappings.

------------------------------------------------------------------------

## Task 2.3 --- Introduce Abstract CSV Parser Base Class

Objective: Consolidate shared CSV parsing logic across datasets.

Scope: - MBIE + LAWA parsers

Definition of Done: - Shared abstraction introduced. - No duplication
remains. - No behavior change.

------------------------------------------------------------------------

# Step 3 --- Feature Usage Validation

## Task 3.1 --- Validate API Version Intent

Objective: Confirm /api/v1 versioning is meaningful and documented.

Scope: - Controllers - README documentation

Definition of Done: - Versioning policy documented. - No ambiguous
usage.

------------------------------------------------------------------------

## Task 3.2 --- Validate dataset_release Semantics

Objective: Ensure dataset_release has explicit architectural meaning.

Scope: - Ask pipeline - Read endpoints - Documentation

Definition of Done: - Role documented. - No ambiguous release
aggregation.

------------------------------------------------------------------------

## Task 3.3 --- LLM Stub/Provider Contract Alignment

Objective: Ensure stub cannot silently diverge from real provider
contract.

Scope: - StubExplanationProvider - OpenAiExplanationProvider - Citation
validation layer

Definition of Done: - Contract documented. - Stub behavior aligned. -
Tests updated if needed.

------------------------------------------------------------------------

# Step 4 --- Test Coverage Floor

## Task 4.1 --- Add JaCoCo Coverage Reporting

Objective: Introduce coverage reporting and enforce threshold.

Scope: - Maven configuration - CI pipeline

Definition of Done: - Coverage plugin enabled. - Threshold enforced
(70--80%). - CI fails below threshold.

------------------------------------------------------------------------

## Task 4.2 --- Close Critical Coverage Gaps

Objective: Add tests for uncovered critical business logic.

Scope: - FactPack builders - Dataset selection logic - Refusal mapping

Definition of Done: - Critical paths covered. - No core logic untested.

------------------------------------------------------------------------

# Step 5 --- Frontend Architecture Sanity Sweep

## Task 5.1 --- Remove Domain Logic from Frontend

Objective: Ensure frontend remains purely presentational.

Scope: - Ask flow - Any derived logic in UI

Definition of Done: - No domain decisions in frontend. - Backend
authoritative.

------------------------------------------------------------------------

## Task 5.2 --- Refusal UX Clarity Improvements

Objective: Differentiate unsupported vs internal vs capability refusal
states in UI.

Scope: - Ask page UI components

Definition of Done: - Clear, intentional refusal messaging. - No
ambiguous error states.

------------------------------------------------------------------------

# Step 6 --- Documentation & Portfolio Narrative

## Task 6.1 --- Add Project Scope & Narrative to README

Objective: Clarify what Wai & Watts is and is not.

Scope: - README.md only - Add Project Scope section - Add Non-Goals
section - No restructuring of existing sections - No formatting
overhaul

Non-Scope: - Do not modify other documentation files - Do not rewrite
README entirely - Do not adjust badges, formatting, or install
instructions

Definition of Done: - Scope section added. - Non-goals documented.

------------------------------------------------------------------------

## Task 6.2 --- Add Architecture Evolution Narrative

Objective: Explain architectural progression across phases.

Scope: - README.md OR a new docs/architecture-evolution.md - Add
narrative explaining progression across phases - Cross-reference
existing phase numbers

Non-Scope: - Do not edit progress.md - Do not change phase definitions
- Do not reorganize docs folder

Definition of Done: - Evolution documented clearly.

------------------------------------------------------------------------

## Task 6.3 --- Add Demo Entry Point

Objective: Create DEMO.md with curl examples and 5-minute walkthrough.

Scope: - Create DEMO.md - Add local run instructions - Add example curl
calls - Add example grounded LLM explanation - Add minimal linking from
README

Non-Scope: - Do not modify backend code - Do not change API contracts -
Do not restructure README beyond linking DEMO.md

Definition of Done: - Demo can be executed in \<5 minutes. - Example LLM
explanation included.

------------------------------------------------------------------------

# Exit Criteria

Phase 15 completes when:

-   All tasks above are complete.
-   No architectural concerns remain unresolved.
-   Codebase is portfolio-ready.
-   Phase 16 can focus exclusively on NL/LLM expansion.
