# Wai & Watts — Manual Ingestion Runbook

This runbook describes how a human operator performs manual ingestion.
It is intentionally simple and repeatable.

## Pre-requisites
- Java + Maven available locally
- Backend built: `mvn -f backend -DskipTests package`
- Postgres running and backend config set (env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`)

## Step 1: Download source workbook
Use the helper scripts:
- `./scripts/download/mbie-download.sh`
- `./scripts/download/lawa-download.sh`

These save files under `./downloads/<publisher>/<YYYY-MM-DD>/`.

## Step 2: Transform XLSX to contract CSV
Use the transformer to convert the publisher workbook to the contract CSV.

```
./scripts/transform.sh <dataset_source_code> <input_xlsx_path> <output_csv_path>
```

Example:
```
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-generation-quarterly-and-annual-data-2025-quarter-3.xlsx /tmp/mbie_generation_annual.csv
```

The transformer is contract-first; it produces the canonical CSV required by ingestion.

Dataset codes:
- `mbie.generation.annual`
- `mbie.generation.quarterly`
- `lawa.water_quality.state.multi_year`
- `lawa.water_quality.trend.multi_year`

Note: MBIE annual + quarterly both come from the MBIE workbook; LAWA state + trend both come from the LAWA workbook.

## Step 3: Run manual ingestion
Use the ingestion CLI wrapper:
```
./scripts/ingest.sh <dataset_source_code> <file_path> [published_date] [release_label]
```

Example:
```
./scripts/ingest.sh mbie.generation.annual /path/to/mbie_generation_annual.csv 2025-01-01 "MBIE workbook export"
```

## Step 4: Verify success
The CLI prints:
- Release ID
- Rows persisted

Re-running on the same file should be idempotent.

## Example execution
```
./scripts/transform.sh mbie.generation.annual ./downloads/mbie/2026-02-05/electricity-sept-2025-q3.xlsx /tmp/mbie_generation_annual.csv
./scripts/ingest.sh mbie.generation.annual /tmp/mbie_generation_annual.csv 2025-09-01 "MBIE Q3 2025 workbook"
```

Expected output (shape, not exact values):
```
Starting ingestion...
Dataset: mbie.generation.annual
File: /tmp/mbie_generation_annual.csv
Published Date: 2025-09-01
Release Label: MBIE Q3 2025 workbook
SUCCESS
Release ID: <uuid>
Rows persisted: <count>
```

## Step 4: Run the API (optional)
Start the backend server:
```
mvn -pl backend spring-boot:run
```

Example API calls:
```
curl "http://localhost:8080/api/v1/mbie/generation"
curl "http://localhost:8080/api/v1/mbie/generation/quarterly"
curl "http://localhost:8080/api/v1/lawa/state/multiyear"
curl "http://localhost:8080/api/v1/lawa/trend/multiyear"
```

## Troubleshooting
- If parsing fails, confirm the transformer output CSV contains only the canonical table (no titles, notes, or footers).
- If required columns are missing, verify the source sheet and transformer mapping.
- If the file is truncated or header-only, re-export from the workbook and ensure at least one data row is included.
