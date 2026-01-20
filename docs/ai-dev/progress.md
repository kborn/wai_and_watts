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
- **Active Phase:** Phase 4 — Ingestion Lifecycle Skeleton
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

### Phase 4 — Ingestion Lifecycle Skeleton 🟡
Goal: Track ingestion attempts without parsing data.

Definition of Done:
- [ ] Dev-only ingestion trigger available: `POST /api/v1/internal/ingest` (profile=dev, token-guarded)
- [ ] Lifecycle state transitions recorded: PENDING → IMPORTED (stub success)
- [ ] Idempotency enforced for `(dataset_source_id, content_hash)` (no duplicate rows)
- [ ] One integration test verifies lifecycle + idempotency

Links (add upon merge):
- Implementation PR: [link]
- Tests: [link]
- Decisions added: [link]

---

### Phase 5 — First Dataset Ingestion
Goal: Persist first real interpreted dataset.

Definition of Done:
- [ ] Domain schema
- [ ] Parser
- [ ] Read APIs
