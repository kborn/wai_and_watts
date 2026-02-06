# Wai & Watts

Wai & Watts is a contract-first data ingestion and normalization platform focused on real-world New Zealand environmental and energy datasets.

Phase 10 provides a **human-operated** operator workflow:

download → transform → ingest (CLI) → start backend → validate APIs

## Start here

**Canonical operator guide:**

- `docs/operators/OPERATOR_INGESTION_GUIDE.md`

**Validation exercise / portfolio artifact:**

- `docs/validation/PHASE10_OPERATOR_TEST_DRIVE.md`

## Phase 10 execution model (strict)

- Ingestion is **CLI-driven only** via `./scripts/ingest.sh` (no HTTP ingestion in the operator path).
- The backend server is **NOT required** to perform ingestion.
- The backend server **IS required** for post-ingestion API validation (curl examples in the test drive).
- Internal ingestion endpoints (dev/test) are not operator workflows and should not be referenced in operator docs.

## Supported datasets (Phase 10)

- `mbie.generation.annual`
- `mbie.generation.quarterly`
- `lawa.water_quality.state.multi_year`
- `lawa.water_quality.trend.multi_year`
