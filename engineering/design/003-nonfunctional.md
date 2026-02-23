# Wai & Watts — 003 Non-Functional Requirements

Status: Current enforced NFR baseline
Last updated: 2026-02-23
Audience: Engineers and AI builders

---

## 1) Correctness and Data Integrity
- Ingestion idempotency is enforced by DB uniqueness on `(dataset_source_id, content_hash)`.
- Domain rows are lineage-linked to `dataset_release`.
- Schema evolution uses forward-only Flyway migrations.

---

## 2) Determinism and Grounding (Ask/Explain)
- Ask flow must provide deterministic outcome class for equivalent input shape (answer vs refusal).
- Explanation responses must be grounded in Fact Pack data.
- Non-refusal explanation responses require citations.
- Unsupported capabilities must refuse explicitly; they must not be silently remapped into different answered intents.

---

## 3) Runtime Fidelity and Environment
- Runtime DB target is Postgres.
- H2 is test-only.
- Java runtime baseline is JDK 21.

---

## 4) Testing and CI
- CI executes backend `clean verify` on JDK 21.
- JaCoCo coverage check is enforced at 70% line coverage threshold.
- Test strategy uses fast H2 defaults plus targeted Postgres dialect checks via Testcontainers.

---

## 5) Observability and Failure Handling
- Request/exception logging is enabled through application filters and global handlers.
- `/ask` has refusal-envelope behavior for runtime failures (no raw 500 leakage to ask clients).
- Non-ask API failures use HTTP error responses with request correlation metadata.

---

## 6) Operator Workflow Constraints
- Operator ingestion path is CLI-first and reproducible.
- Transform and ingest scripts are part of the supported local workflow.
- Manual operations remain explicit (no hidden scheduler/poller behavior in this phase scope).

---

## 7) NFR Change Policy
NFR changes that affect architecture contracts must be recorded in `engineering/decisions.md` and reflected in this document in the same update cycle.
