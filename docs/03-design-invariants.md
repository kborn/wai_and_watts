# Design Invariants

These invariants govern the system and are treated as non‑negotiable unless an explicit decision is recorded.

1. Dataset lineage is first‑class and separate from domain data.
2. Idempotency is DB‑authoritative via uniqueness on `(dataset_source_id, content_hash)`.
3. Ingestion lifecycle precedes parsing.
4. Transform → Contract → Persist is mandatory; ingestion consumes contract CSV only.
5. Fact Packs are the exclusive LLM data boundary.
6. Natural language parsing is routing‑only and cannot access the database.
7. Controllers return DTOs; entities are never exposed.
8. Flyway migrations are forward‑only; never edit applied migrations.
9. Complexity is deferred until earned.
10. No autonomous AI commits; humans own architectural judgment.

If a change threatens an invariant, it must be escalated and recorded.
