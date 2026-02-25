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

## Service-level expectations (portfolio SLOs)
- **Availability target (local/demo runtime):** backend health endpoint returns HTTP 200 for >99% of checks during active demo sessions.
- **P95 API latency target (read endpoints):** <= 500 ms for catalog/list-style reads on seeded local data.
- **P95 ask latency target (non-refusal):** <= 2.0 s for supported asks against seeded local data.
- **Ingestion completion target (per dataset, seeded bundle):** <= 5 minutes end-to-end for transform + ingest.
- **Change failure guardrail:** all CI workflows green on `main` before merge; docs governance checks are blocking.

These are pragmatic portfolio targets used to evaluate operational health and regression risk, not production SLAs.
