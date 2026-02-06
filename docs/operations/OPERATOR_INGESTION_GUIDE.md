# Operator Ingestion Guide (Phase 10)

## Purpose

This is the **single source of truth** for running Phase 10 ingestion locally.

If this document conflicts with any other document, this document is correct.

## Supported dataset codes

These are the dataset source codes accepted by both `transform.sh` and `ingest.sh`:

- `mbie.generation.annual`
- `mbie.generation.quarterly`
- `lawa.water_quality.state.multi_year`
- `lawa.water_quality.trend.multi_year`

Notes:
- MBIE annual + quarterly both come from the same MBIE workbook.
- LAWA state + trend both come from the same LAWA workbook.

## Am I ready? (quick checklist)

- [ ] `java -version` shows JDK 21
- [ ] `mvn -v` works
- [ ] Postgres is running locally and reachable
- [ ] Backend builds: `mvn -f backend -DskipTests package`
- [ ] Scripts are executable: `chmod +x ./scripts/*.sh`
- [ ] You can download a publisher workbook into `./downloads/<publisher>/<YYYY-MM-DD>/`

## Database setup (local Postgres)

This project expects Postgres running locally and the backend configured via environment variables:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

Example (adjust to your machine):

```bash
export DB_URL="jdbc:postgresql://localhost:5432/waiwatts"
export DB_USER="waiwatts"
export DB_PASSWORD="waiwatts"
```

If you need a quick local DB:

```bash
createdb waiwatts
createuser waiwatts
psql -d postgres -c "ALTER USER waiwatts WITH PASSWORD 'waiwatts';"
psql -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE waiwatts TO waiwatts;"
```

## Step 1 — Download publisher workbook

Use the helper scripts (these save under `./downloads/<publisher>/<YYYY-MM-DD>/`):

- `./scripts/download/mbie-download.sh`
- `./scripts/download/lawa-download.sh`

Run them, then confirm you have an XLSX at a path like:

- `./downloads/mbie/<YYYY-MM-DD>/electricity-generation-quarterly-and-annual-data-2025-quarter-3.xlsx`
- `./downloads/lawa/<YYYY-MM-DD>/NZ-State-and-Trend-Water-Quality-Data-2025.xlsx`

## Step 2 — Transform XLSX → contract CSV

Usage:

```bash
./scripts/transform.sh <dataset_source_code> <input_xlsx_path> <output_csv_path>
```

Concrete examples (MBIE Q3 2025 workbook):

```bash
./scripts/transform.sh mbie.generation.annual   ./downloads/mbie/2026-02-05/electricity-generation-quarterly-and-annual-data-2025-quarter-3.xlsx   /tmp/mbie_generation_annual.csv

./scripts/transform.sh mbie.generation.quarterly   ./downloads/mbie/2026-02-05/electricity-generation-quarterly-and-annual-data-2025-quarter-3.xlsx   /tmp/mbie_generation_quarterly.csv
```

Concrete examples (LAWA 2025 workbook):

```bash
./scripts/transform.sh lawa.water_quality.state.multi_year   ./downloads/lawa/2026-02-05/NZ-State-and-Trend-Water-Quality-Data-2025.xlsx   /tmp/lawa_state_multi_year.csv

./scripts/transform.sh lawa.water_quality.trend.multi_year   ./downloads/lawa/2026-02-05/NZ-State-and-Trend-Water-Quality-Data-2025.xlsx   /tmp/lawa_trend_multi_year.csv
```

## Step 3 — Ingest contract CSV (CLI)

Usage:

```bash
./scripts/ingest.sh <dataset_source_code> <file_path> [published_date] [release_label]
```

Concrete examples:

```bash
./scripts/ingest.sh mbie.generation.annual /tmp/mbie_generation_annual.csv 2025-09-01 "MBIE Q3 2025 workbook"
./scripts/ingest.sh mbie.generation.quarterly /tmp/mbie_generation_quarterly.csv 2025-09-01 "MBIE Q3 2025 workbook"
./scripts/ingest.sh lawa.water_quality.state.multi_year /tmp/lawa_state_multi_year.csv 2025-10-30 "LAWA State + Trend 2025"
./scripts/ingest.sh lawa.water_quality.trend.multi_year /tmp/lawa_trend_multi_year.csv 2025-10-30 "LAWA State + Trend 2025"
```

Important:
- The backend server must **not** be required to perform ingestion.
- The operator workflow does **not** use internal ingestion HTTP endpoints.

## Step 4 — Validate via API (server required for validation)

Start the backend server:

```bash
mvn -pl backend spring-boot:run
```

Then run API validation calls:

```bash
curl "http://localhost:8080/api/v1/mbie/generation"
curl "http://localhost:8080/api/v1/mbie/generation/quarterly"
curl "http://localhost:8080/api/v1/lawa/state/multiyear"
curl "http://localhost:8080/api/v1/lawa/trend/multiyear"
```

## Troubleshooting (operator-first)

- If transform fails: confirm the workbook is the expected publisher file and the transformer mapping is current.
- If ingestion fails: confirm the contract CSV contains only the canonical table (no titles/notes/footers).
- If API validation fails: confirm the backend server is running and Postgres credentials are correct.
