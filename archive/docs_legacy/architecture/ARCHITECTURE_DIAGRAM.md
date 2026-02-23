# Architecture Diagram

Owner: Staff Engineer documentation track (`docs/ai-dev/progress.md` Phase 15)
Last updated: 2026-02-23

## Diagram Source
- Primary source: this file (Mermaid)
- If exported image is needed for portfolio artifacts, generate from this Mermaid block and store adjacent as `architecture_overview.png`.

## System Overview (Phase 15)

```mermaid
flowchart LR
  A[Operator Scripts\ntransform.sh / ingest.sh] --> B[CLI Commands\nManualTransformCommand\nManualIngestionCommand]
  B --> C[Ingestion Layer\nParsers + Ingestion Services]
  C --> D[(Postgres\nFlyway Schema)]

  E[Frontend React] --> F[Public REST API\n/api/v1/...]
  F --> G[Read Services]
  G --> D

  E --> H[/api/v1/explanations/ask]
  H --> I[Intent Parser + Validation + Dataset Selection]
  I --> J[FactPack Builders\nCanonical dataset_release pinning]
  J --> K[Explanation Provider\n(OpenAI or Stub)]
  K --> H
  J --> D
```

## Notes
- Frontend is presentational; backend remains semantic authority.
- Ask flow is Fact-Pack grounded and refusal-safe.
- Ingestion is operator-driven and idempotent at release lineage boundary.
