# Phase 10 — Operator Test Drive (Validation Exercise)

## Scope

This document is a **validation and learning exercise**.

It is NOT the canonical operator ingestion workflow.

For canonical operator execution, follow:

- `docs/operators/OPERATOR_INGESTION_GUIDE.md`

---

## Purpose

Validate that Wai & Watts Phase 10 behaves like a real operator-facing ingestion tool, end-to-end.

Goals:
- Prove real publisher XLSX → contract CSV → CLI ingestion
- Prove idempotency
- Exercise read APIs after ingestion (server required for validation)
- Capture operator notes for portfolio + future phases

---

## Am I ready?

- [ ] JDK 21 active (`java -version`)
- [ ] Maven works (`mvn -v`)
- [ ] Postgres running locally
- [ ] DB env vars set: `DB_URL`, `DB_USER`, `DB_PASSWORD`
- [ ] Backend builds: `mvn -f backend clean package spring-boot:repackage -DskipTests`
- [ ] Database migrations run once: `mvn -f backend spring-boot:run` (then stop)
- [ ] Download scripts produce XLSX under `./downloads/<publisher>/<YYYY-MM-DD>/`

---

## Supported dataset codes

- `mbie.generation.annual`
- `mbie.generation.quarterly`
- `lawa.water_quality.state.multi_year`
- `lawa.water_quality.trend.multi_year`

---

## Phase 1 — Download publisher workbook

Use the download scripts:

- `./scripts/download/mbie-download.sh`
- `./scripts/download/lawa-download.sh`

Confirm you have the XLSX paths under `./downloads/<publisher>/<YYYY-MM-DD>/`.

---

## Phase 2 — Transform (XLSX → contract CSV)

Run at least one dataset transform:

```bash
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-sept-2025-q3.xlsx
```

Verify:
- CSV exists and is non-empty
- Headers match the contract schema
- File is saved in `./transforms/mbie.generation.annual/YYYY-MM-DD/mbie_generation_annual.csv`

Optional: Test custom output directory:
```bash
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-sept-2025-q3.xlsx --output-dir /tmp
```

---

## Phase 3 — Ingest (CLI)

Ingest the contract CSV (using transform output from Phase 2):

```bash
./scripts/ingest.sh mbie.generation.annual ./transforms/mbie.generation.annual/2026-02-05/mbie_generation_annual.csv 2025-09-01 "MBIE Q3 2025 workbook"
```

Verify:
- Release ID printed
- Rows persisted printed
- Re-running the exact same command is idempotent (no duplicates)

---

## Phase 4 — Start backend service (required for validation)

The backend server is **required** for API validation steps.

```bash
mvn -pl backend spring-boot:run
```

---

## Phase 5 — Validate via API (curl)

Run these calls and sanity check results (counts/samples/no duplicates):

```bash
curl "http://localhost:8080/api/v1/mbie/generation"
curl "http://localhost:8080/api/v1/mbie/generation/quarterly"
curl "http://localhost:8080/api/v1/lawa/state/multiyear"
curl "http://localhost:8080/api/v1/lawa/trend/multiyear"
```

---

## Phase 6 — Failure testing (intentional)

Do at least 3:

- Wrong dataset code (expect fast validation failure)
- Missing input file (expect validation failure)
- Corrupt XLSX (expect transform failure)
- Schema drift / missing required columns (expect transform fail-fast)

---

## Phase 7 — Reflection notes

Capture:
- What worked well
- What was confusing
- What surprised you
- Time-to-first-success

---

## Architectural invariant

Transform is a pure function of publisher artifact → contract CSV.

Network IO, scheduling, and service runtime remain external to transform and ingestion execution.
