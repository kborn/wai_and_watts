# Operational Model

## Canonical operator flow

`download → transform → ingest (CLI) → start backend → validate APIs`

- **Download** is manual (no polling or schedulers).
- **Transform** converts publisher artifacts into **contract CSV**.
- **Ingest** is CLI‑driven and reproducible.
- **Service runtime** is used for API validation and browsing, not as an ingestion orchestrator.

## Idempotency and releases
- Hash is computed over contract CSV bytes.
- DB uniqueness on `(dataset_source_id, content_hash)` ensures dedupe.
- Duplicate ingestion attempts are treated as successful no‑ops.

This keeps the operational surface area narrow and reliable.
