# Wai & Watts — Project Progress

This document tracks **phases and current status**.
It answers: “Where are we in the plan?”
If session context is lost: read `project-context.md` → `progress.md` → `decisions.md`.

---

## Phase Entry Format (Required)

### Phase N — [Name] [Status]
Goal: one sentence

Definition of Done:
- [ ] objective criteria

Notes (optional):
- temporary reminders or blockers

---

## Current Position
- **Active Phase:** Phase 5 — Build & Test Guardrails
- **Status:** In progress

---

### Phase 1 — Project Foundations ✅
Goal: Establish scope, workflow, and documentation.

Definition of Done:
- [x] project-context.md
- [x] roles.md
- [x] progress.md
- [x] decisions.md
- [x] ai_usage.md

---

### Phase 2 — Backend Scaffolding ✅
Goal: Bootable backend with no domain logic.

Definition of Done:
- [x] Spring Boot scaffold
- [x] Postgres + Flyway
- [x] Test framework

---

### Phase 3 — Dataset Lineage & Read APIs ✅
Goal: Model provenance and expose read-only metadata.

Definition of Done:
- [x] Dataset source + release models
- [x] Idempotency constraints
- [x] Service + controller layers
- [x] DTO separation

---

### Phase 4 — Ingestion Lifecycle Skeleton ✅
Goal: Track ingestion attempts without parsing data.

Definition of Done:
- [x] Dev-only ingestion trigger available: `POST /api/v1/internal/ingest` (profiles: dev,test; token from `waiwatts.internalToken`)
- [x] Lifecycle state transitions recorded: PENDING → IMPORTED (stub success)
- [x] Idempotency enforced for `(dataset_source_id, content_hash)` (no duplicate rows)
- [x] One integration test verifies lifecycle + idempotency (H2, test profile)

Links:
- Commit: https://github.com/kborn/wai_and_watts/commit/2112393c481f6ad2113f32d4ffcb0fceb4447358

Notes:
- The internal ingestion endpoint is a plumbing trigger, not a mock of external providers. It simulates “a new release arrived” to exercise lifecycle, timestamps, and idempotency without fetching/parsing.
- For tests, the controller is enabled under the `test` profile (H2) to avoid hitting the dev Postgres.

---


### Phase 5 — Build & Test Guardrails
Goal: Prevent broken builds from being committed or merged; establish baseline CI discipline.

Definition of Done:
- [x] GitHub Actions workflow runs on push and pull_request
- [x] Workflow builds the backend successfully
- [x] Workflow runs the full test suite
- [x] Failing tests fail the workflow
- [ ] Build status is visible in GitHub (Actions tab)
- [x] Local developer guidance for running checks is documented

Notes:
- CI uses in-memory DB (H2) initially
- Postgres/Testcontainers may be added later if dialect fidelity is required
 - Workflow file: `.github/workflows/ci.yml`

### Phase 6 — First Dataset Ingestion
Goal: Persist first real interpreted dataset.

Definition of Done:
- [ ] Domain schema
- [ ] Parser
- [ ] Read APIs
